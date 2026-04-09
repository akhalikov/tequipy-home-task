# Equipment Allocation API

## Context
Tequipy helps companies manage the full lifecycle of IT equipment — from procurement and assignment to retrieval and disposal. 
Your task is to build a single backend service that manages an asset inventory and handles allocation requests: assigning 
the best available equipment to employees based on a role-based equipment policy.

## Data Model
`Equipment`
- id, type (main_computer / monitor / keyboard / mouse)
- brand, model
- State:
  - available / reserved / assigned
- condition_score (0.0–1.0)
- purchase_date

`AllocationRequest`
- id, employee_id
- policy — list of required equipment types with optional constraints
- state, allocated_equipments

## REST API
- `POST /equipments` - Register a new equipment
- `GET /equipments` - List equipments, filter by status
- `POST /allocations` - Create request, trigger allocation
- `GET /allocations/{id}` - Get status and allocated equipments
- `POST /allocations/{id}/confirm` - Confirm — equipments transition to ass
- `POST /allocations/{id}/cancel` - Cancel — release equipments back to available

## Allocation Algorithm

This is the core challenge. Given a policy such as:
    
_"1 laptop with condition ≥ 0.8, prefer Apple; 2 monitors"_

…select the optimal set of available equipment. Rules:

- **Hard constraints** (type, minimum condition score) must be satisfied — no exceptions.
- **Soft preferences** (brand, recency) are scored and used to rank candidates.
- **Global optimality** — a naive per-slot greedy will fail when equipment competes across slots. For example: two monitor slots and three available monitors, where one monitor satisfies a constraint the other slot also needs. Design for this.

## In you README.md:

- explain your algorithmic approach 
- its time complexity, and trade-offs vs. simpler alternatives.

When `POST /allocations` is called, the service must publish an internal AllocationCreated event 
(in-process event bus or a simple queue) that a background handler consumes to transition equipment statuses. This simulates the async boundary that would exist in a real multi-service architecture — show that you can design for it.

## Technical Requirements
Production-quality product: As a Senior/Lead Engineer, please frame the task within a 
given environment (feel free to make reasonable assumptions, which allows you to demonstrate 
your highest level of expertise) and provide everything necessary.

## Bonus (Optional)
- Replace the in-process event bus with a real broker (RabbitMQ, Redis Streams) — show the config in docker-compose
- POST /equipments/{id}/retire — marks equipment as retired with a reason; allocation must never select retired equipments
- Benchmark the allocation algorithm with 5 000+ equipments and include p50/p99 latency results in the README

## Evaluation Criteria
- Algorithm correctness & reasoning
- Service design
- Code quality
- Testing
- Documentation