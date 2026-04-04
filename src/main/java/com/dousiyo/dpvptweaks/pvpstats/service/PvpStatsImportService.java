package com.dousiyo.dpvptweaks.pvpstats.service;

import com.dousiyo.dpvptweaks.pvpstats.data.PvpStatsSavedData;
import com.dousiyo.dpvptweaks.pvpstats.model.PvpStatKey;
import com.dousiyo.dpvptweaks.pvpstats.util.SavedDataAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PvpStatsImportService {
    private PvpStatsImportService() {
    }

    public static int importBundle(ServerLevel level, String modeId, Objective wins, Objective losses, Objective kills, Objective deaths) {
        Scoreboard scoreboard = level.getServer().getScoreboard();
        Set<String> holders = new LinkedHashSet<>();
        collectHolders(holders, scoreboard, wins);
        collectHolders(holders, scoreboard, losses);
        collectHolders(holders, scoreboard, kills);
        collectHolders(holders, scoreboard, deaths);

        PvpStatsSavedData savedData = SavedDataAccessor.get(level);
        MinecraftServer server = level.getServer();
        int imported = 0;
        long timestamp = System.currentTimeMillis();
        for (String holder : holders) {
            Optional<UUID> uuid = ScoreHolderResolveService.resolvePlayerUuid(server, holder);
            if (uuid.isEmpty()) {
                continue;
            }

            int winValue = readScore(scoreboard, holder, wins);
            int lossValue = readScore(scoreboard, holder, losses);
            int killValue = readScore(scoreboard, holder, kills);
            int deathValue = readScore(scoreboard, holder, deaths);
            String playerName = ScoreHolderResolveService.resolveLastKnownName(server, uuid.get(), holder);
            if (PvpStatsMutationService.importBundle(savedData, uuid.get(), playerName, modeId, winValue, lossValue, killValue, deathValue, timestamp)) {
                imported++;
            }
        }
        return imported;
    }

    public static int importObjective(ServerLevel level, String modeId, Objective objective, PvpStatKey statKey) {
        Scoreboard scoreboard = level.getServer().getScoreboard();
        PvpStatsSavedData savedData = SavedDataAccessor.get(level);
        MinecraftServer server = level.getServer();
        int imported = 0;
        for (Score score : scoreboard.getPlayerScores(objective)) {
            int value = score.getScore();
            if (value <= 0) {
                continue;
            }

            String holder = score.getOwner();
            Optional<UUID> uuid = ScoreHolderResolveService.resolvePlayerUuid(server, holder);
            if (uuid.isEmpty()) {
                continue;
            }

            String playerName = ScoreHolderResolveService.resolveLastKnownName(server, uuid.get(), holder);
            if (PvpStatsMutationService.importObjective(savedData, uuid.get(), playerName, modeId, statKey, value)) {
                imported++;
            }
        }
        return imported;
    }

    public static int importMatchResult(ServerLevel level, String modeId, PlayerTeam winnerTeam, PlayerTeam loserTeam, Objective kills, Objective deaths) {
        Scoreboard scoreboard = level.getServer().getScoreboard();
        PvpStatsSavedData savedData = SavedDataAccessor.get(level);
        MinecraftServer server = level.getServer();
        long timestamp = System.currentTimeMillis();

        int imported = 0;
        imported += importTeamResult(savedData, server, scoreboard, modeId, winnerTeam, true, kills, deaths, timestamp);
        imported += importTeamResult(savedData, server, scoreboard, modeId, loserTeam, false, kills, deaths, timestamp);
        return imported;
    }

    public static int importDrawResult(ServerLevel level, String modeId, PlayerTeam teamA, PlayerTeam teamB, Objective kills, Objective deaths) {
        Scoreboard scoreboard = level.getServer().getScoreboard();
        PvpStatsSavedData savedData = SavedDataAccessor.get(level);
        MinecraftServer server = level.getServer();
        long timestamp = System.currentTimeMillis();

        int imported = 0;
        imported += importTeamDraw(savedData, server, scoreboard, modeId, teamA, kills, deaths, timestamp);
        imported += importTeamDraw(savedData, server, scoreboard, modeId, teamB, kills, deaths, timestamp);
        return imported;
    }

    private static int importTeamResult(PvpStatsSavedData savedData, MinecraftServer server, Scoreboard scoreboard, String modeId, PlayerTeam team, boolean isWinner, Objective kills, Objective deaths, long timestamp) {
        if (team == null) {
            return 0;
        }

        int imported = 0;
        for (String holder : team.getPlayers()) {
            Optional<UUID> uuid = ScoreHolderResolveService.resolvePlayerUuid(server, holder);
            if (uuid.isEmpty()) {
                continue;
            }

            int killValue = readScore(scoreboard, holder, kills);
            int deathValue = readScore(scoreboard, holder, deaths);
            String playerName = ScoreHolderResolveService.resolveLastKnownName(server, uuid.get(), holder);
            if (PvpStatsMutationService.importBundle(
                    savedData,
                    uuid.get(),
                    playerName,
                    modeId,
                    isWinner ? 1 : 0,
                    isWinner ? 0 : 1,
                    killValue,
                    deathValue,
                    timestamp
            )) {
                imported++;
            }
        }
        return imported;
    }

    private static int importTeamDraw(PvpStatsSavedData savedData, MinecraftServer server, Scoreboard scoreboard, String modeId, PlayerTeam team, Objective kills, Objective deaths, long timestamp) {
        if (team == null) {
            return 0;
        }

        int imported = 0;
        for (String holder : team.getPlayers()) {
            Optional<UUID> uuid = ScoreHolderResolveService.resolvePlayerUuid(server, holder);
            if (uuid.isEmpty()) {
                continue;
            }

            int killValue = readScore(scoreboard, holder, kills);
            int deathValue = readScore(scoreboard, holder, deaths);
            String playerName = ScoreHolderResolveService.resolveLastKnownName(server, uuid.get(), holder);
            if (PvpStatsMutationService.importDraw(
                    savedData,
                    uuid.get(),
                    playerName,
                    modeId,
                    killValue,
                    deathValue,
                    timestamp
            )) {
                imported++;
            }
        }
        return imported;
    }

    private static void collectHolders(Set<String> holders, Scoreboard scoreboard, Objective objective) {
        for (Score score : scoreboard.getPlayerScores(objective)) {
            holders.add(score.getOwner());
        }
    }

    private static int readScore(Scoreboard scoreboard, String holder, Objective objective) {
        if (objective == null || holder == null || holder.isBlank()) {
            return 0;
        }
        if (!scoreboard.hasPlayerScore(holder, objective)) {
            return 0;
        }
        return scoreboard.getOrCreatePlayerScore(holder, objective).getScore();
    }
}
