package com.dousiyo.dpvptweaks.pvpstats.service;

import com.dousiyo.dpvptweaks.pvpstats.model.AggregateStats;
import com.dousiyo.dpvptweaks.pvpstats.model.MatchRecord;
import com.dousiyo.dpvptweaks.pvpstats.model.PlayerStats;
import com.dousiyo.dpvptweaks.pvpstats.model.StatsGuiPayload;
import com.dousiyo.dpvptweaks.pvpstats.util.SavedDataAccessor;
import net.minecraft.server.level.ServerLevel;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PvpStatsQueryService {
    private PvpStatsQueryService() {
    }

    public static StatsGuiPayload query(ServerLevel level, UUID targetUuid, String fallbackName) {
        PlayerStats playerStats = SavedDataAccessor.get(level).get(targetUuid);
        String resolvedName = ScoreHolderResolveService.resolveLastKnownName(level.getServer(), targetUuid, fallbackName);
        if (playerStats == null) {
            return new StatsGuiPayload(resolvedName, new AggregateStats(), Map.of(), List.of());
        }

        String targetName = playerStats.lastKnownName().isBlank() ? resolvedName : playerStats.lastKnownName();
        Map<String, AggregateStats> sortedModes = playerStats.modes().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue().copy()), LinkedHashMap::putAll);
        List<MatchRecord> history = List.copyOf(playerStats.recentMatches());
        return new StatsGuiPayload(targetName, playerStats.global(), sortedModes, history);
    }
}
