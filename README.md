# Equipment Allocation API

## Context
Tequipy helps companies manage the full lifecycle of IT equipment - from procurement and assignment to retrieval and disposal.
Your task is to build a single backend service that manages an asset inventory and handles allocation requests: assigning
the best available equipment to employees based on a role-based equipment policy.

Read the full task, API and data model description in [TODO.md](TODO.md)

## Requirements
- Java 21+
- Gradle 7+

## How To Run Performance Benchmarks
```bash
./gradlew testPerformance
```

## API Endpoints

| Method | Path                         | Description                                | Response Status      |
|--------|------------------------------|--------------------------------------------|----------------------|
| POST   | `/equipments`                | Register a new equipment                   | 201 Created          |
| GET    | `/equipments`                | List equipments, filter by status          | 200 OK               |
| POST   | `/equipments/{id}/retire`    | Retire equipment with a reason             | 200 OK               |
| POST   | `/allocations`               | Create request, trigger async allocation   | 201 Created          |
| GET    | `/allocations/{id}`          | Get status and allocated equipments        | 200 OK               |
| POST   | `/allocations/{id}/confirm`  | Confirm — equipments transition to assigned | 200 OK              |
| POST   | `/allocations/{id}/cancel`   | Cancel — release equipments back to available | 200 OK            |

## Important Assumptions & Design Decisions
### `Equipment` state model:
- Only `AVAILABLE` items are considered for allocation.
- Only `RESERVED` can be assigned.
- Only `AVAILABLE` items can be retired. If item us `RESERVED` it must be unassigned first. This simplifies the state machine and prevents edge cases where an item is retired while still assigned to an employee.
- Retired equipment cannot be made available again.

### `AllocationRequest` state model:
- Only `PENDING` requests can be considered for allocation.
- Only `ALLOCATED` requests can be marked as `CONFIRMED` or `CANCELLED`.

### Async allocation:
- `POST /allocations` returns 201 PENDING immediately; the actual matching runs asynchronously. 
Clients are expected to poll `GET /allocations/{id}` until the state reaches a terminal value `(ALLOCATED, FAILED)`. 
There is no webhook or long-poll alternative.

## Allocation Algorithm

Allocation is modeled as a **maximum-weight bipartite matching** problem:

- **Left nodes** - policy slots (e.g. `MAIN_COMPUTER`, `MONITOR`, `MONITOR`, `KEYBOARD`)
- **Right nodes** - available equipment candidates fetched from the database
- **Edge weight** - a composite quality score for assigning a specific piece of equipment to a slot:

  ```
  score = condition_score
        + recency_weight              (linear decay over 5 years, weight 0.3)
        + brand_preference_weight     (0.6 if preferred brand matches, else 0)
  ```

- **Goal** - find a perfect matching of all policy slots to **distinct** equipment items that maximises the total score

### Algorithm comparison

Three approaches were considered and benchmarked:

#### Greedy

For each slot, pick the highest-scoring available candidate of the required type, mark it as
used, and move on.

- **Time complexity** - O(n × m)
- **Optimality** - not guaranteed. Greedy can take the locally best item for an early slot and
  leave a later slot with no valid candidate, even when a valid assignment exists. 
  - Example: two `MONITOR` slots, one preferring Apple. If the algorithm assigns the only Apple monitor
    to the first slot (highest raw score), the second slot loses its preferred candidate even though
    swapping would yield a higher total score.
- **Verdict** - rejected: fails to find valid assignments in contention scenarios.

#### Backtracking with branch-and-bound ✓ (implemented)

Explores all candidate combinations recursively. Slots are processed in
**most-constrained-first** order (fewest candidates first) to prune dead branches early.
**Branch-and-bound** cuts any subtree whose upper-bound score (sum of each remaining slot's
best possible score) cannot beat the best complete assignment found so far.

- **Time complexity** - O(MAX_CANDIDATES^n) worst case; in practice near O(n × m) due to pruning
- **Optimality** - guaranteed (exhaustive with pruning never skips the global optimum)
- **Tuning** - `CANDIDATES_PER_SLOT` must be kept moderate (≤ 10–20) to bound the search
  space; with branch-and-bound the algorithm explores only a tiny fraction of the theoretical tree
- **Verdict** - chosen: provably optimal, straightforward to reason about, fast enough for
  typical policies (4–8 slots, 10 candidates each)

#### Hungarian algorithm (Kuhn–Munkres)

Solves the assignment problem exactly using dual potentials and augmenting paths.

- **Time complexity** - O(n² × m), polynomial regardless of candidate count
- **Optimality** - guaranteed by mathematical proof
- **Tuning** - none; `CANDIDATES_PER_SLOT` can be raised to 100+ with no algorithmic cost
- **Verdict** - valid alternative: superior scaling, but adds implementation complexity with no
  measurable latency benefit at the candidate sizes used in practice.

#### Summary

| | Greedy | Backtracking | Hungarian |
|---|---|---|---|
| Time complexity | O(n × m) | O(k^n) worst case* | O(n² × m) |
| Globally optimal | ✗ | ✓ | ✓ |
| Handles contention | ✗ | ✓ | ✓ |
| Implementation complexity | Low | Medium | High |
| Candidate cap needed | No | Yes (≤ 20) | No |

*\* with branch-and-bound pruning, the observed complexity is near-linear for typical policies*

### Candidate pre-selection

Before the algorithm runs, candidates are fetched per equipment type:

```sql
SELECT … FROM equipment
WHERE state = 'AVAILABLE' AND type = ?
ORDER BY condition_score DESC
LIMIT ?
FOR UPDATE
```

A covering index on `(state, type, condition_score DESC)` lets the planner satisfy the filter,
sort, and limit in a single B-tree scan with no separate sort step. At most
`slots_of_that_type × CANDIDATES_PER_SLOT` rows are locked per type, keeping the pessimistic
lock scope proportional to the policy size rather than the total equipment pool.

### Concurrency

Each allocation runs in a dedicated async thread pool (`allocation-*` threads, sized at
`2 × availableProcessors`) after the creating transaction commits. Pessimistic write locks are
held only for the duration of the allocation transaction, preventing double-allocation under
concurrent requests without serialising unrelated allocations.

### Benchmark results (5 000-item pool, H2, Apple M-series)

| Scenario | p50 | p95 | p99 |
|---|---|---|---|
| Typical policy (laptop + 2 monitors + keyboard) | 10 ms | 11 ms | 19 ms |
| Single item (1 monitor) | 9 ms | 9 ms | 9 ms |

End-to-end latency includes two HTTP round-trips (POST + polling GET), async dispatch, the
database lock query, and equipment saves. 
The algorithm itself accounts for < 1 ms of this budget.

### Examples

#### Example 1: Policy for graphical designer with two monitor slots, one preferring Apple

Policy:
- Slot 1: `MONITOR, preferredBrand=Apple (soft)`
- Slot 2: `MONITOR`

Equipment pool:
```
Monitor A: Apple, condition=0.9 → score for slot 1 = 0.9 + 0.6 = 1.5
Monitor B: Dell, condition=0.8  → score for slot 1 = 0.8
Monitor C: Dell, condition=0.7  → score for slot 1 = 0.7
```

- **Step 1** 
  - Fetching all monitors from the database locking the records as per pessimistic lock.
- **Step 2**
  - Apply `most-constrained-first`: the ordering has no effect here — both slots have the same number of candidates (3), 
  so the original order is preserved.
- **Step 3**
```
Slot 1 = Monitor A (score=1.5)
  └─ Slot 2 = Monitor B (score=0.8) → total=2.3 ✓ saved
  └─ Slot 2 = Monitor C (score=0.7) → total=2.2 < 2.3, not better

Slot 1 = Monitor B (score=0.8)
  └─ branch-and-bound: max possible = 0.8 + 1.5 = 2.3
     upper bound 2.3 does not exceed current best 2.3 → pruned (equal score, no improvement possible)

Slot 1 = Monitor C (score=0.7)
  └─ branch-and-bound: max possible = 0.7 + 1.5 = 2.2 < 2.3 → pruned ✗
```

Result: Slot 1 → Monitor A, Slot 2 → Monitor B, total score = 2.3.

#### Example 2: Shows that Greedy can fail to find a valid assignment

Policy:
- Slot 1: `MONITOR, preferredBrand=Apple (soft)`
- Slot 2: `MONITOR, minConditionScore=0.8 (hard)`

Equipment pool:
```
Monitor A: Apple, condition=0.9
Monitor B: Dell, condition=0.7
Monitor C: Dell, condition=0.6
```

**Greedy:**

```
Slot 1: Monitor A: 0.9 + 0.6 (apple bonus) = 1.5  ← save, best score ✓
  
Slot 2: minConditionScore=0.8, remained B (0.7) and C (0.6)
    No candidates satisfy hard constraint → allocation fails ✗
```

**Backtracking:**
```
Slot 2 goes first (most-constrained-first):
  Only Monitor A satisfies minConditionScore=0.8
  → Slot 2 = Monitor A (the only candidate) ✓

Slot 1: remained B and C
  Monitor B: score=0.8 → save (best from remained candidates) ✓
```

Result: Slot 2 → Monitor A, Slot 1 → Monitor B, total score = 1.7.