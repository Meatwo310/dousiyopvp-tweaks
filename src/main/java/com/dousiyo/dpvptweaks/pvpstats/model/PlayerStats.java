package com.dousiyo.dpvptweaks.pvpstats.model;

import com.dousiyo.dpvptweaks.pvpstats.rank.RankState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlayerStats {
    private String lastKnownName;
    private final AggregateStats global;
    private final Map<String, AggregateStats> modes;
    private final List<MatchRecord> recentMatches;
    private final Map<String, RankState> ranks;
    private PlayerPrivacySettings privacySettings;

    public PlayerStats() {
        this("", new AggregateStats(), new LinkedHashMap<>(), new ArrayList<>(), PlayerPrivacySettings.DEFAULT, Map.of());
    }

    public PlayerStats(String lastKnownName, AggregateStats global, Map<String, AggregateStats> modes, List<MatchRecord> recentMatches) {
        this(lastKnownName, global, modes, recentMatches, PlayerPrivacySettings.DEFAULT, Map.of());
    }

    public PlayerStats(String lastKnownName, AggregateStats global, Map<String, AggregateStats> modes, List<MatchRecord> recentMatches, PlayerPrivacySettings privacySettings) {
        this(lastKnownName, global, modes, recentMatches, privacySettings, Map.of());
    }

    public PlayerStats(String lastKnownName, AggregateStats global, Map<String, AggregateStats> modes, List<MatchRecord> recentMatches, PlayerPrivacySettings privacySettings, Map<String, RankState> ranks) {
        this.lastKnownName = lastKnownName == null ? "" : lastKnownName;
        this.global = global == null ? new AggregateStats() : global;
        this.modes = new LinkedHashMap<>(modes == null ? Map.of() : modes);
        this.recentMatches = new ArrayList<>(recentMatches == null ? List.of() : recentMatches);
        this.ranks = new LinkedHashMap<>(ranks == null ? Map.of() : ranks);
        this.privacySettings = privacySettings == null ? PlayerPrivacySettings.DEFAULT : privacySettings;
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

    public Map<String, RankState> ranks() {
        return ranks;
    }

    public RankState rankFor(String modeId) {
        return ranks.getOrDefault(modeId == null || modeId.isBlank() ? "overall" : modeId, RankState.INITIAL);
    }

    public PlayerPrivacySettings privacySettings() {
        return privacySettings;
    }

    public void setPrivacySettings(PlayerPrivacySettings privacySettings) {
        this.privacySettings = privacySettings == null ? PlayerPrivacySettings.DEFAULT : privacySettings;
    }

    public AggregateStats getOrCreateMode(String modeId) {
        return modes.computeIfAbsent(modeId, ignored -> new AggregateStats());
    }

    public boolean hasAnyData() {
        return global.hasAnyValue() || !modes.isEmpty() || !recentMatches.isEmpty();
    }
}
