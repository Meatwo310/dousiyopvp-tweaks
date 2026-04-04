package com.dousiyo.dpvptweaks.pvpstats.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

public record StatsGuiPayload(
        String targetName,
        AggregateStats global,
        Map<String, AggregateStats> modeStats,
        List<MatchRecord> recentMatches
) {
    public StatsGuiPayload {
        targetName = targetName == null ? "" : targetName;
        global = global == null ? new AggregateStats() : global.copy();

        Map<String, AggregateStats> copiedModes = new LinkedHashMap<>();
        if (modeStats != null) {
            for (Map.Entry<String, AggregateStats> entry : modeStats.entrySet()) {
                copiedModes.put(entry.getKey(), entry.getValue().copy());
            }
        }
        modeStats = Collections.unmodifiableMap(copiedModes);
        recentMatches = recentMatches == null ? List.of() : List.copyOf(recentMatches);
    }

    public boolean hasAnyData() {
        return global.hasAnyValue() || !modeStats.isEmpty() || !recentMatches.isEmpty();
    }
}
