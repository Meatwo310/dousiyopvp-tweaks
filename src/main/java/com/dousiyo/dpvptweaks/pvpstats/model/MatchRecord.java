package com.dousiyo.dpvptweaks.pvpstats.model;

public record MatchRecord(
        String matchId,
        String modeId,
        String result,
        int kills,
        int deaths,
        long timestamp
) {
    public MatchRecord {
        matchId = matchId == null ? "" : matchId;
        modeId = modeId == null ? "" : modeId;
        result = result == null ? "UNKNOWN" : result;
    }
}
