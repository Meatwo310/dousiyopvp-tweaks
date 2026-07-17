package com.dousiyo.dpvptweaks.pvpstats.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.UUID;
import java.util.Set;
import com.dousiyo.dpvptweaks.pvpstats.mode.PvpModeDefinition;
import com.dousiyo.dpvptweaks.pvpstats.rank.RankState;

public record StatsGuiPayload(
        UUID targetId,
        String targetName,
        AggregateStats global,
        Map<String, AggregateStats> modeStats,
        List<MatchRecord> recentMatches,
        List<PvpModeDefinition> modeDefinitions,
        List<RankingEntry> rankingEntries,
        Map<String, RankState> ranks,
        Set<String> awardedBadgeIds,
        PlayerPrivacySettings privacySettings,
        boolean editableSettings,
        boolean statsVisible,
        boolean historyVisible
) {
    public StatsGuiPayload(String targetName, AggregateStats global, Map<String, AggregateStats> modeStats, List<MatchRecord> recentMatches) {
        this(new UUID(0L, 0L), targetName, global, modeStats, recentMatches, List.of(), List.of(), Map.of(), Set.of(), PlayerPrivacySettings.DEFAULT, false, true, true);
    }

    public StatsGuiPayload {
        targetId = targetId == null ? new UUID(0L, 0L) : targetId;
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
        modeDefinitions = modeDefinitions == null ? List.of() : List.copyOf(modeDefinitions);
        rankingEntries = rankingEntries == null ? List.of() : List.copyOf(rankingEntries);
        ranks = ranks == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(ranks));
        awardedBadgeIds = awardedBadgeIds == null ? Set.of() : Set.copyOf(awardedBadgeIds);
        privacySettings = privacySettings == null ? PlayerPrivacySettings.DEFAULT : privacySettings;
        if (!statsVisible) {
            global = new AggregateStats();
            modeStats = Map.of();
        }
        if (!historyVisible) {
            recentMatches = List.of();
        }
    }

    public boolean hasAnyData() {
        return global.hasAnyValue()
                || modeStats.values().stream().anyMatch(AggregateStats::hasAnyValue)
                || !recentMatches.isEmpty();
    }
}
