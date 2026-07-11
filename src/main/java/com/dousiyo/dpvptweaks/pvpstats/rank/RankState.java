package com.dousiyo.dpvptweaks.pvpstats.rank;

public record RankState(int rating, int peakRating, int placementMatches) {
    public static final RankState INITIAL = new RankState(RankSystem.INITIAL_RATING, RankSystem.INITIAL_RATING, 0);

    public RankState {
        rating = Math.max(0, rating);
        peakRating = Math.max(rating, peakRating);
        placementMatches = Math.max(0, placementMatches);
    }

    public RankTier tier() {
        return RankTier.fromRating(rating, placementMatches);
    }
}
