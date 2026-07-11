package com.dousiyo.dpvptweaks.pvpstats.model;

import java.util.UUID;

public record RankingEntry(
        UUID playerId,
        String mcid,
        String modeId,
        int rank,
        long kills,
        long deaths,
        long matches,
        long wins,
        long losses
) {
    public static final String OVERALL_MODE_ID = "overall";

    public RankingEntry {
        mcid = mcid == null ? "" : mcid;
        modeId = modeId == null || modeId.isBlank() ? OVERALL_MODE_ID : modeId;
        rank = Math.max(1, rank);
        kills = Math.max(0L, kills);
        deaths = Math.max(0L, deaths);
        matches = Math.max(0L, matches);
        wins = Math.max(0L, wins);
        losses = Math.max(0L, losses);
    }

    public static RankingEntry of(UUID playerId, String mcid, String modeId, int rank, AggregateStats stats) {
        AggregateStats safeStats = stats == null ? new AggregateStats() : stats;
        return new RankingEntry(
                playerId,
                mcid,
                modeId,
                rank,
                safeStats.kills(),
                safeStats.deaths(),
                safeStats.matches(),
                safeStats.wins(),
                safeStats.losses()
        );
    }
}
