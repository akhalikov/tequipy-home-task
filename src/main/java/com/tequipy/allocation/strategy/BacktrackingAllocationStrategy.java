package com.tequipy.allocation.strategy;

import com.tequipy.allocation.domain.PolicyItem;
import com.tequipy.allocation.strategy.AllocationResult.Success;
import com.tequipy.equipment.domain.Equipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.tequipy.allocation.strategy.AllocationResult.failure;
import static com.tequipy.allocation.strategy.AllocationResult.success;
import static com.tequipy.allocation.strategy.Constants.BRAND_PREFERENCE_WEIGHT;
import static com.tequipy.allocation.strategy.Constants.CANDIDATES_PER_SLOT;
import static com.tequipy.allocation.strategy.Constants.RECENCY_SCALE_DAYS;
import static com.tequipy.allocation.strategy.Constants.RECENCY_WEIGHT;
import static com.tequipy.allocation.strategy.ProcessingOrder.preferringSlotsWithLessCandidates;
import static java.lang.Math.max;
import static java.time.LocalDate.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Comparator.comparing;

@Slf4j
@Component
public class BacktrackingAllocationStrategy implements AllocationStrategy {

    private static final int INITIAL_DEPTH = 0;
    private static final double INITIAL_SCORE = 0.0;

    /**
     * Selects the optimal set of available equipment for a given allocation policy.
     * <p>
     * Filters candidates by hard constraints, sorts slots in most-constrained-first order,
     * precomputes suffix scores for branch-and-bound pruning, then delegates to {@link #backtrack}.
     *
     * @param policy    list of policy slots defining required equipment types and constraints
     * @param equipment pool of available equipment to allocate from
     *
     * @return {@link AllocationResult.Success} with the optimal assignment, or {@link AllocationResult.Failure}
     *         if no valid assignment exists.
     */
    @Override
    public AllocationResult allocate(List<PolicyItem> policy, Collection<Equipment> equipment) {
        if (equipment.isEmpty())
            return failure("No equipment available");

        log.debug("Running allocation: availableEquipment={} policy={}", equipment.size(), policy);
        final var candidates = findEligibleEquipment(policy, equipment);

        // TODO: This check only guards against zero candidates per slot type.
        // It does not detect cross-slot contention (e.g. 2 MONITOR slots but only 1 monitor available).
        // Such cases are handled correctly by backtracking (returns failure), but at the cost of
        // running the full search. A pre-check comparing slot counts vs available counts per type
        // would fail fast without entering the recursion.
        for (int i = 0; i < policy.size(); i++) {
            if (candidates.get(i).isEmpty()) {
                return failure("No available equipment of type: " + policy.get(i).type());
            }
        }

        final var processingOrder = preferringSlotsWithLessCandidates(candidates.stream().map(List::size).toList());
        final var orderedPolicy = processingOrder.stream().map(policy::get).toList();
        final var orderedCandidates = processingOrder.stream().map(candidates::get).toList();
        final double[] suffixMaxScores = computeSuffixMaxScores(orderedPolicy, orderedCandidates);
        final double[] bestScoreSoFar = {0.0};
        return backtrack(
            INITIAL_DEPTH,
            orderedPolicy,
            orderedCandidates,
            suffixMaxScores,
            new LinkedHashMap<>(),
            new HashSet<>(),
            INITIAL_SCORE,
            bestScoreSoFar);
    }

    /**
     * Filters and ranks equipment candidates for each policy slot.
     * <p>
     * For each slot, retains only equipment satisfying hard constraints ({@link PolicyItem#isSatisfiedBy}),
     * sorts by score descending, and caps at {@link Constants#CANDIDATES_PER_SLOT} to bound the search space.
     *
     * @param policyItems list of policy slots to find candidates for
     * @param available   pool of available equipment to filter from
     * @return per-slot list of eligible equipment, ordered by score DESC; candidates.get(i) corresponds to policyItems.get(i)
     */
    private List<List<Equipment>> findEligibleEquipment(List<PolicyItem> policyItems, Collection<Equipment> available) {
        final var candidates = new ArrayList<List<Equipment>>(policyItems.size());
        for (var policyItem : policyItems) {
            final var eligible = available.stream()
                .filter(policyItem::isSatisfiedBy)
                .sorted(comparing((Equipment e) -> scoreEquipment(policyItem, e)).reversed())
                .limit(CANDIDATES_PER_SLOT)
                .toList();

            candidates.add(eligible);
        }
        return candidates;
    }

    /**
     * Precomputes suffix upper-bound scores for branch-and-bound pruning.
     * <p>
     * {@code suffixMaxScores[i]} holds the theoretical maximum total score achievable
     * for slots {@code i...n-1}, assuming each slot gets its highest-scoring candidate independently.
     * Since candidates are pre-sorted DESC, index 0 of each slot's list is always the max.
     *
     * @param policyItems list of policy slots in processing order
     * @param candidates  per-slot candidate lists, each sorted by score DESC
     * @return array where index {@code i} contains the upper-bound score sum for slots {@code i...n-1}
     */
    private double[] computeSuffixMaxScores(List<PolicyItem> policyItems, List<List<Equipment>> candidates) {
        int n = policyItems.size();
        double[] suffixMaxScores = new double[n];
        // candidates are already sorted descending, so index 0 holds the max score for that slot
        suffixMaxScores[n - 1] = scoreEquipment(policyItems.get(n - 1), candidates.get(n - 1).get(0));
        for (int i = n - 2; i >= 0; i--) {
            suffixMaxScores[i] = scoreEquipment(policyItems.get(i), candidates.get(i).get(0)) + suffixMaxScores[i + 1];
        }
        return suffixMaxScores;
    }

    /**
     * Computes a composite quality score for assigning a specific piece of equipment to a policy slot.
     * <p>
     * Score components:
     * <ul>
     *   <li><b>Condition</b> — equipment condition score (0.0–1.0), always included</li>
     *   <li><b>Brand preference</b> — adds {@link Constants#BRAND_PREFERENCE_WEIGHT} if equipment matches preferred brand</li>
     *   <li><b>Recency</b> — linear decay over {@link Constants#RECENCY_SCALE_DAYS}; newer equipment scores higher</li>
     * </ul>
     *
     * @param policyItem policy slot providing soft preference constraints
     * @param equipment  equipment being evaluated
     * @return composite score; higher is better
     */
    private static double scoreEquipment(PolicyItem policyItem, Equipment equipment) {
        double score = equipment.getConditionScore().doubleValue();
        if (policyItem.isOfPreferredBrand(equipment))
            score += BRAND_PREFERENCE_WEIGHT;

        long daysSincePurchased = DAYS.between(equipment.getPurchaseDate(), now());
        double recency = max(0.0, 1.0 - (double) daysSincePurchased / RECENCY_SCALE_DAYS);
        score += recency * RECENCY_WEIGHT;
        return score;
    }

    /**
     * Backtracking with branch-and-bound
     * <p>
     * Explores all candidate combinations recursively. Slots are processed in **most-constrained-first** order
     * (fewest candidates first) to prune dead branches early. **Branch-and-bound** cuts any subtree whose upper-bound
     * score (sum of each remaining slot's best possible score) cannot beat the best complete assignment found so far.
     *
     * @param depth            current slot index being assigned (0-based); equals policy size when a complete assignment is found
     * @param policyItems      policy slots sorted in most-constrained-first order
     * @param candidates       per-slot list of eligible equipment, pre-filtered by hard constraints and sorted by score DESC;
     *                         candidates.get(i) corresponds to policyItems.get(i)
     * @param suffixMaxScores  precomputed upper-bound scores: suffixMaxScores[i] is the sum of the best possible scores
     *                         for slots i...n-1, used for branch-and-bound pruning
     * @param current          mutable map of slot index → equipment for the assignment being explored; cleared on backtrack
     * @param usedEquipmentIds set of equipment IDs already assigned in the current path; prevents double-allocation across slots
     * @param currentScore     accumulated score for the current partial assignment
     * @param bestScoreSoFar   single-element array holding the best complete assignment score found so far;
     *                         array instead of primitive to allow mutation across recursive calls
     *
     * @return the best complete assignment found in this subtree, or null if no valid assignment exists.
     */
    private Success backtrack(int depth,
                              List<PolicyItem> policyItems,
                              List<List<Equipment>> candidates,
                              double[] suffixMaxScores,
                              Map<Integer, Equipment> current,
                              Set<UUID> usedEquipmentIds,
                              double currentScore,
                              double[] bestScoreSoFar) {
        if (depth == policyItems.size()) {
            final var assignment = new LinkedHashMap<PolicyItem, Equipment>();
            current.forEach((idx, equipment) -> assignment.put(policyItems.get(idx), equipment));
            return success(assignment, currentScore);
        }

        if (bestScoreSoFar[0] > 0 && currentScore + suffixMaxScores[depth] <= bestScoreSoFar[0]) {
            return null;
        }

        final var policyItem = policyItems.get(depth);
        Success bestResultSoFar = null;

        for (final var equipment : candidates.get(depth)) {
            if (usedEquipmentIds.contains(equipment.getId())) {
                continue;
            }

            current.put(depth, equipment);
            usedEquipmentIds.add(equipment.getId());

            Success result = backtrack(
                depth + 1, policyItems, candidates, suffixMaxScores, current, usedEquipmentIds,
                currentScore + scoreEquipment(policyItem, equipment), bestScoreSoFar);

            if (result != null && (bestResultSoFar == null || result.totalScore() > bestResultSoFar.totalScore())) {
                bestResultSoFar = result;
                bestScoreSoFar[0] = bestResultSoFar.totalScore();
            }

            current.remove(depth);
            usedEquipmentIds.remove(equipment.getId());
        }

        return bestResultSoFar;
    }
}
