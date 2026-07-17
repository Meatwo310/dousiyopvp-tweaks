package com.dousiyo.dpvptweaks.pvpstats.service;

import com.dousiyo.dpvptweaks.pvpstats.data.PvpStatsSavedData;
import com.dousiyo.dpvptweaks.pvpstats.model.AggregateStats;
import com.dousiyo.dpvptweaks.pvpstats.model.MatchRecord;
import com.dousiyo.dpvptweaks.pvpstats.model.PlayerStats;
import com.dousiyo.dpvptweaks.pvpstats.model.PlayerPrivacySettings;
import com.dousiyo.dpvptweaks.pvpstats.model.RankingEntry;
import com.dousiyo.dpvptweaks.pvpstats.model.StatsGuiPayload;
import com.dousiyo.dpvptweaks.pvpstats.mode.PvpModeDefinition;
import com.dousiyo.dpvptweaks.pvpstats.mode.PvpModeManager;
import com.dousiyo.dpvptweaks.pvpstats.util.SavedDataAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PvpStatsQueryService {
    private PvpStatsQueryService() {
    }

    public static StatsGuiPayload query(ServerLevel level, UUID targetUuid, String fallbackName) {
        return query(level, targetUuid, targetUuid, fallbackName);
    }

    public static StatsGuiPayload query(ServerLevel level, UUID viewerUuid, UUID targetUuid, String fallbackName) {
        PvpStatsSavedData savedData = SavedDataAccessor.get(level);
        PlayerStats playerStats = savedData.get(targetUuid);
        String resolvedName = ScoreHolderResolveService.resolveLastKnownName(level.getServer(), targetUuid, fallbackName);
        List<RankingEntry> rankingEntries = buildRankings(level.getServer(), savedData);
        List<PvpModeDefinition> modeDefinitions = PvpModeManager.sortedDefinitions();
        boolean editableSettings = viewerUuid != null && viewerUuid.equals(targetUuid);

        if (playerStats == null) {
            return new StatsGuiPayload(
                    targetUuid,
                    resolvedName,
                    new AggregateStats(),
                    buildSortedModes(Map.of(), modeDefinitions),
                    List.of(),
                    modeDefinitions,
                    rankingEntries,
                    Map.of(),
                    savedData.awardedBadges(targetUuid),
                    PlayerPrivacySettings.DEFAULT,
                    editableSettings,
                    true,
                    true
            );
        }

        PlayerPrivacySettings privacySettings = playerStats.privacySettings();
        boolean statsVisible = editableSettings || privacySettings.showStats();
        boolean historyVisible = statsVisible && (editableSettings || privacySettings.showMatchHistory());
        String targetName = playerStats.lastKnownName().isBlank() ? resolvedName : playerStats.lastKnownName();
        Map<String, AggregateStats> sortedModes = buildSortedModes(playerStats.modes(), modeDefinitions);
        List<MatchRecord> history = List.copyOf(playerStats.recentMatches());
        return new StatsGuiPayload(
                targetUuid,
                targetName,
                playerStats.global(),
                sortedModes,
                history,
                modeDefinitions,
                rankingEntries,
                editableSettings || privacySettings.showRank() ? playerStats.ranks() : Map.of(),
                savedData.awardedBadges(targetUuid),
                privacySettings,
                editableSettings,
                statsVisible,
                historyVisible
        );
    }

    private static List<RankingEntry> buildRankings(MinecraftServer server, PvpStatsSavedData savedData) {
        Map<String, List<RankingCandidate>> byMode = new LinkedHashMap<>();
        for (Map.Entry<UUID, PlayerStats> entry : savedData.players().entrySet()) {
            UUID uuid = entry.getKey();
            PlayerStats stats = entry.getValue();
            PlayerPrivacySettings privacy = stats.privacySettings();
            if (!privacy.showRank() || !privacy.showStats() || !privacy.joinLeaderboards()) {
                continue;
            }

            String name = ScoreHolderResolveService.resolveLastKnownName(server, uuid, stats.lastKnownName());
            addCandidate(byMode, new RankingCandidate(uuid, name, RankingEntry.OVERALL_MODE_ID, stats.global()));
            for (Map.Entry<String, AggregateStats> modeEntry : stats.modes().entrySet()) {
                addCandidate(byMode, new RankingCandidate(uuid, name, modeEntry.getKey(), modeEntry.getValue()));
            }
        }

        List<RankingEntry> entries = new ArrayList<>();
        for (Map.Entry<String, List<RankingCandidate>> modeEntry : byMode.entrySet()) {
            List<RankingCandidate> candidates = modeEntry.getValue();
            candidates.sort(PvpStatsQueryService::compareCandidates);
            for (int i = 0; i < candidates.size(); i++) {
                RankingCandidate candidate = candidates.get(i);
                entries.add(RankingEntry.of(candidate.playerId(), candidate.mcid(), candidate.modeId(), i + 1, candidate.stats()));
            }
        }
        return entries;
    }

    private static void addCandidate(Map<String, List<RankingCandidate>> byMode, RankingCandidate candidate) {
        if (!isRankingEligible(candidate.modeId(), candidate.stats())) {
            return;
        }
        byMode.computeIfAbsent(candidate.modeId(), ignored -> new ArrayList<>()).add(candidate);
    }

    private static boolean isRankingEligible(String modeId, AggregateStats stats) {
        PvpModeDefinition definition = PvpModeManager.get(modeId);
        if (definition != null && !definition.rankingEnabled()) {
            return false;
        }
        long minMatches = definition == null ? 10L : definition.rankingMinMatches();
        long minKills = definition == null ? 20L : definition.rankingMinKills();
        return stats != null && stats.matches() >= minMatches && stats.kills() >= minKills;
    }

    private static Map<String, AggregateStats> buildSortedModes(Map<String, AggregateStats> stats, List<PvpModeDefinition> definitions) {
        Map<String, AggregateStats> sorted = new LinkedHashMap<>();
        for (PvpModeDefinition definition : definitions) {
            if (definition.visible()) {
                AggregateStats value = stats.get(definition.modeId());
                sorted.put(definition.modeId(), value == null ? new AggregateStats() : value.copy());
            }
        }
        stats.entrySet().stream()
                .filter(entry -> !sorted.containsKey(entry.getKey()))
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue().copy()));
        return sorted;
    }

    private static int compareCandidates(RankingCandidate left, RankingCandidate right) {
        int byKdr = Double.compare(kdr(right.stats()), kdr(left.stats()));
        if (byKdr != 0) {
            return byKdr;
        }
        int byWinRate = Double.compare(winRate(right.stats()), winRate(left.stats()));
        if (byWinRate != 0) {
            return byWinRate;
        }
        int byKills = Long.compare(right.stats().kills(), left.stats().kills());
        if (byKills != 0) {
            return byKills;
        }
        return left.mcid().compareToIgnoreCase(right.mcid());
    }

    private static double kdr(AggregateStats stats) {
        if (stats.deaths() <= 0L) {
            return stats.kills() <= 0L ? 0.0D : Double.POSITIVE_INFINITY;
        }
        return (double) stats.kills() / (double) stats.deaths();
    }

    private static double winRate(AggregateStats stats) {
        if (stats.matches() <= 0L) {
            return 0.0D;
        }
        return (double) stats.wins() / (double) stats.matches();
    }

    private record RankingCandidate(UUID playerId, String mcid, String modeId, AggregateStats stats) {
    }
}
