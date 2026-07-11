package com.dousiyo.dpvptweaks.pvpstats.model;

public record PlayerPrivacySettings(
        boolean showRank,
        boolean showStats,
        boolean showMatchHistory,
        boolean joinLeaderboards
) {
    public static final PlayerPrivacySettings DEFAULT = new PlayerPrivacySettings(true, true, true, true);

    public PlayerPrivacySettings {
        if (!showStats) {
            joinLeaderboards = false;
        }
    }
}
