package com.tequipy.allocation.strategy;

import java.util.ArrayList;
import java.util.List;

import static java.util.Comparator.comparingInt;

public final class ProcessingOrder {

    public static List<Integer> preferringSlotsWithLessCandidates(List<Integer> equipmentSizes) {
        final var processingOrder = new ArrayList<Integer>();
        for (int i = 0; i < equipmentSizes.size(); i++) {
            processingOrder.add(i);
        }
        processingOrder.sort(comparingInt(equipmentSizes::get));
        return processingOrder;
    }

    private ProcessingOrder() {
    }
}
