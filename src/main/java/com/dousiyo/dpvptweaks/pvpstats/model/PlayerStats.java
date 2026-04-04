package com.dousiyo.dpvptweaks.pvpstats.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlayerStats {
    private String lastKnownName;
    private final AggregateStats global;
    private final Map<String, AggregateStats> modes;
    private final List<MatchRecord> recentMatches;

    public PlayerStats() {
        this("", new AggregateStats(), new LinkedHashMap<>(), new ArrayList<>());
    }

    public PlayerStats(String lastKnownName, AggregateStats global, Map<String, AggregateStats> modes, List<MatchRecord> recentMatches) {
        this.lastKnownName = lastKnownName == null ? "" : lastKnownName;
        this.global = global == null ? new AggregateStats() : global;
        this.modes = new LinkedHashMap<>(modes == null ? Map.of() : modes);
        this.recentMatches = new ArrayList<>(recentMatches == null ? List.of() : recentMatches);
    }

    public String lastKnownName() {
        return lastKnownName;
    }

    public void setLastKnownName(String lastKnownName) {
        if (lastKnownName != null && !lastKnownName.isBlank()) {
            this.lastKnownName = lastKnownName;
        }
    }

    public AggregateStats global() {
        return global;
    }

    public Map<String, AggregateStats> modes() {
        return modes;
    }

    public List<MatchRecord> recentMatches() {
        return recentMatches;
    }

    public AggregateStats getOrCreateMode(String modeId) {
        return modes.computeIfAbsent(modeId, ignored -> new AggregateStats());
    }

    public boolean hasAnyData() {
        return global.hasAnyValue() || !modes.isEmpty() || !recentMatches.isEmpty();
    }
}
