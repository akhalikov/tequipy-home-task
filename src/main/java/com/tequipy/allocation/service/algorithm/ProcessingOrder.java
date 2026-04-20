package com.tequipy.allocation.service.algorithm;

import java.util.ArrayList;
import java.util.List;

import static java.util.Comparator.comparingInt;

public final class ProcessingOrder {

    public static List<Integer> preferringSlotsWithLessCandidates(List<Integer> equipmentSizes) {
        final List<Integer> processingOrder = new ArrayList<>();
        for (int i = 0; i < equipmentSizes.size(); i++) {
            processingOrder.add(i);
        }
        processingOrder.sort(comparingInt(equipmentSizes::get));
        return processingOrder;
    }

    private ProcessingOrder() {
    }
}
