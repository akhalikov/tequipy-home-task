package com.tequipy.allocation.service.algorithm;

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
     * To prevent excessive computation, we limit the number of candidate equipment items considered for each policy item.
     */
    int MAX_CANDIDATES_PER_POLICY_ITEM = 100;
}
