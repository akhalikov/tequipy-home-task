package com.tequipy.allocation.service.algorithm;

import com.tequipy.allocation.domain.PolicyItem;
import com.tequipy.allocation.service.algorithm.AllocationResult.Success;
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

import static com.tequipy.allocation.service.algorithm.AllocationResult.failure;
import static com.tequipy.allocation.service.algorithm.AllocationResult.success;
import static com.tequipy.allocation.service.algorithm.Constants.BRAND_PREFERENCE_WEIGHT;
import static com.tequipy.allocation.service.algorithm.Constants.MAX_CANDIDATES_PER_POLICY_ITEM;
import static com.tequipy.allocation.service.algorithm.Constants.RECENCY_SCALE_DAYS;
import static com.tequipy.allocation.service.algorithm.Constants.RECENCY_WEIGHT;
import static com.tequipy.allocation.service.algorithm.ProcessingOrder.preferringSlotsWithLessCandidates;
import static java.lang.Math.max;
import static java.time.LocalDate.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Comparator.comparing;

@Component
@Slf4j
public class BacktrackingAllocationStrategy implements AllocationStrategy {

    private static final double INITIAL_SCORE = 0.0;

    @Override
    public AllocationResult allocate(List<PolicyItem> policy, Collection<Equipment> equipment) {
        if (equipment.isEmpty())
            return failure("No equipment available");

        log.debug("Running allocation: availableEquipment={} policy={}", equipment.size(), policy);
        final var candidates = findEligibleEquipment(policy, equipment);

        for (int i = 0; i < policy.size(); i++) {
            if (candidates.get(i).isEmpty()) {
                return failure("No available equipment of type: " + policy.get(i).type());
            }
        }

        final var processingOrder = preferringSlotsWithLessCandidates(candidates.stream().map(List::size).toList());
        final var orderedPolicy = processingOrder.stream().map(policy::get).toList();
        final var orderedCandidates = processingOrder.stream().map(candidates::get).toList();
        return backtrack(0, orderedPolicy, orderedCandidates, new LinkedHashMap<>(), new HashSet<>(), INITIAL_SCORE);
    }

    private List<List<Equipment>> findEligibleEquipment(List<PolicyItem> policyItems, Collection<Equipment> available) {
        final List<List<Equipment>> candidates = new ArrayList<>(policyItems.size());
        for (PolicyItem policyItem : policyItems) {
            final var eligible = available.stream()
                .filter(policyItem::isSatisfiedBy)
                .sorted(comparing((Equipment e) -> scoreEquipment(policyItem, e)).reversed())
                .limit(MAX_CANDIDATES_PER_POLICY_ITEM)
                .toList();

            candidates.add(eligible);
        }
        return candidates;
    }

    private static double scoreEquipment(PolicyItem policyItem, Equipment equipment) {
        double score = equipment.getConditionScore().doubleValue();
        if (policyItem.isOfPreferredBrand(equipment))
            score += BRAND_PREFERENCE_WEIGHT;

        long daysSincePurchased = DAYS.between(equipment.getPurchaseDate(), now());
        double recency = max(0.0, 1.0 - (double) daysSincePurchased / RECENCY_SCALE_DAYS);
        score += recency * RECENCY_WEIGHT;
        return score;
    }

    private Success backtrack(int depth,
                              List<PolicyItem> policyItems,
                              List<List<Equipment>> candidates,
                              Map<Integer, Equipment> current,
                              Set<UUID> usedEquipmentIds,
                              double currentScore) {
        if (depth == policyItems.size()) {
            final var assignment = new LinkedHashMap<PolicyItem, Equipment>();
            current.forEach((idx, equipment) -> assignment.put(policyItems.get(idx), equipment));
            return success(assignment, currentScore);
        }

        final var policyItem = policyItems.get(depth);
        Success bestResultSoFar = null;

        for (Equipment equipment : candidates.get(depth)) {
            if (usedEquipmentIds.contains(equipment.getId())) {
                continue;
            }

            current.put(depth, equipment);
            usedEquipmentIds.add(equipment.getId());

            Success result = backtrack(
                depth + 1, policyItems, candidates, current, usedEquipmentIds,
                currentScore + scoreEquipment(policyItem, equipment));

            if (bestResultSoFar == null || (result.totalScore() > bestResultSoFar.totalScore())) {
                bestResultSoFar = result;
            }

            current.remove(depth);
            usedEquipmentIds.remove(equipment.getId());
        }

        return bestResultSoFar;
    }
}
