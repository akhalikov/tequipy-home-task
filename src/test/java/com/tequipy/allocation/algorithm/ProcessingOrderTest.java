package com.tequipy.allocation.algorithm;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tequipy.allocation.strategy.ProcessingOrder.preferringSlotsWithLessCandidates;
import static org.assertj.core.api.Assertions.assertThat;

class ProcessingOrderTest {

    @Test
    void prefers_fewer_size() {
        // given
        var sizes = List.of(3, 1, 2);

        // when
        var result = preferringSlotsWithLessCandidates(sizes);

        // then
        assertThat(result).containsExactly(1, 2, 0);
    }
}