package com.dousiyo.dpvptweaks.pvpstats.rank;

public enum RankTier {
    UNRANKED(0, "unranked"),
    BRONZE(0, "bronze"),
    SILVER(900, "silver"),
    GOLD(1100, "gold"),
    PLATINUM(1300, "platinum"),
    DIAMOND(1500, "diamond");

    private final int minimumRating;
    private final String serializedName;

    RankTier(int minimumRating, String serializedName) {
        this.minimumRating = minimumRating;
        this.serializedName = serializedName;
    }

    public int minimumRating() {
        return minimumRating;
    }

    public String serializedName() {
        return serializedName;
    }

    public static RankTier fromRating(int rating, int placementMatches) {
        if (placementMatches < RankSystem.PLACEMENT_MATCHES) {
            return UNRANKED;
        }
        RankTier result = BRONZE;
        for (RankTier tier : values()) {
            if (tier != UNRANKED && rating >= tier.minimumRating) {
                result = tier;
            }
        }
        return result;
    }
}
