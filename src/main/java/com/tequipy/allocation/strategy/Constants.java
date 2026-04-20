package com.tequipy.allocation.strategy;

public interface Constants {

    double BRAND_PREFERENCE_WEIGHT = 0.6;

    /**
     * Weight for the recency score, which is calculated based on how recently the equipment was purchased.
     * A higher weight encourages allocation of newer equipment.
     */
    double RECENCY_WEIGHT = 0.3;

    /**
     * Age threshold in days beyond which recency score drops to zero.
     * Equipment purchased today scores 1.0; equipment at this age or older scores 0.0,
     * with linear decay in between.
     */
    long RECENCY_SCALE_DAYS = 365L * 5;

    /**
     * Maximum number of candidate equipment items fetched from the database per policy slot.
     * Bounds the pessimistic lock scope: a 4-slot policy locks at most 4 × this many rows.
     * For Backtracking algorithm, this is a critical tuning parameter: the algorithm is O(m^n)
     * so raising this has exponential cost. In practice, a value of 10 provides a good balance between allocation
     * success rate and performance under typical conditions.
     */
    int CANDIDATES_PER_SLOT = 10;
}
