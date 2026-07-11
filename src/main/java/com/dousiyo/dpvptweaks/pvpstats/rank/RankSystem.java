package com.dousiyo.dpvptweaks.pvpstats.rank;

/**
 * Future ranked-play foundation. Keep {@link #ENABLED} false until match
 * eligibility, disconnect handling and season rules are implemented.
 */
public final class RankSystem {
    public static final boolean ENABLED = false;
    public static final int INITIAL_RATING = 1000;
    public static final int PLACEMENT_MATCHES = 10;
    private static final int K_FACTOR = 32;

    private RankSystem() {
    }

    public static int calculateRatingChange(int ownRating, double opponentAverageRating, double score) {
        double expected = 1.0D / (1.0D + Math.pow(10.0D, (opponentAverageRating - ownRating) / 400.0D));
        return (int) Math.round(K_FACTOR * (Math.max(0.0D, Math.min(1.0D, score)) - expected));
    }
}
