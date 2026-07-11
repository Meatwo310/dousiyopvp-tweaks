package com.dousiyo.dpvptweaks.pvpstats.service;

import com.dousiyo.dpvptweaks.pvpstats.data.PvpStatsSavedData;
import com.dousiyo.dpvptweaks.pvpstats.model.AggregateStats;
import com.dousiyo.dpvptweaks.pvpstats.model.MatchRecord;
import com.dousiyo.dpvptweaks.pvpstats.model.PlayerStats;
import com.dousiyo.dpvptweaks.pvpstats.model.PvpStatKey;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class PvpStatsMutationService {
    private static final int MAX_RECENT_MATCHES = 20;
    private static final DateTimeFormatter MATCH_ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .withZone(ZoneOffset.UTC);

    private PvpStatsMutationService() {
    }

    public static boolean importBundle(PvpStatsSavedData data, UUID uuid, String playerName, String modeId, int wins, int losses, int kills, int deaths, long timestamp) {
        return importBundle(data, uuid, playerName, modeId, wins, losses, kills, deaths, timestamp, null);
    }

    public static boolean importBundle(PvpStatsSavedData data, UUID uuid, String playerName, String modeId, int wins, int losses, int kills, int deaths, long timestamp, String matchId) {
        long total = Math.max(0L, wins) + Math.max(0L, losses) + Math.max(0L, kills) + Math.max(0L, deaths);
        if (uuid == null || modeId == null || modeId.isBlank() || total <= 0L) {
            return false;
        }

        PlayerStats playerStats = data.getOrCreate(uuid);
        playerStats.setLastKnownName(playerName);
        playerStats.global().addBundle(wins, losses, kills, deaths);
        playerStats.getOrCreateMode(modeId).addBundle(wins, losses, kills, deaths);

        if ((wins + losses) == 1) {
            addRecentMatch(playerStats, matchId, modeId, wins > 0 ? "WIN" : "LOSS", kills, deaths, timestamp);
        }

        data.setDirty();
        return true;
    }

    public static boolean importObjective(PvpStatsSavedData data, UUID uuid, String playerName, String modeId, PvpStatKey statKey, int value) {
        if (uuid == null || statKey == null || modeId == null || modeId.isBlank() || value <= 0) {
            return false;
        }

        PlayerStats playerStats = data.getOrCreate(uuid);
        playerStats.setLastKnownName(playerName);
        applyStat(playerStats.global(), statKey, value);
        applyStat(playerStats.getOrCreateMode(modeId), statKey, value);
        data.setDirty();
        return true;
    }

    public static boolean importDraw(PvpStatsSavedData data, UUID uuid, String playerName, String modeId, int kills, int deaths, long timestamp) {
        return importDraw(data, uuid, playerName, modeId, kills, deaths, timestamp, null);
    }

    public static boolean importDraw(PvpStatsSavedData data, UUID uuid, String playerName, String modeId, int kills, int deaths, long timestamp, String matchId) {
        if (uuid == null || modeId == null || modeId.isBlank()) {
            return false;
        }

        long total = Math.max(0L, kills) + Math.max(0L, deaths) + 1L;
        if (total <= 0L) {
            return false;
        }

        PlayerStats playerStats = data.getOrCreate(uuid);
        playerStats.setLastKnownName(playerName);

        AggregateStats global = playerStats.global();
        global.addDraws(1);
        global.addKills(kills);
        global.addDeaths(deaths);

        AggregateStats modeStats = playerStats.getOrCreateMode(modeId);
        modeStats.addDraws(1);
        modeStats.addKills(kills);
        modeStats.addDeaths(deaths);

        addRecentMatch(playerStats, matchId, modeId, "DRAW", kills, deaths, timestamp);
        data.setDirty();
        return true;
    }

    private static void applyStat(AggregateStats stats, PvpStatKey statKey, int value) {
        switch (statKey) {
            case WINS -> stats.addWins(value);
            case LOSSES -> stats.addLosses(value);
            case DRAWS -> stats.addDraws(value);
            case KILLS -> stats.addKills(value);
            case DEATHS -> stats.addDeaths(value);
        }
    }

    private static void addRecentMatch(PlayerStats playerStats, String requestedMatchId, String modeId, String result, int kills, int deaths, long timestamp) {
        long safeTimestamp = timestamp > 0L ? timestamp : System.currentTimeMillis();
        String matchId = requestedMatchId == null || requestedMatchId.isBlank()
                ? MATCH_ID_FORMATTER.format(Instant.ofEpochMilli(safeTimestamp))
                        + "_" + Integer.toUnsignedString((modeId + result + kills + deaths).hashCode())
                : requestedMatchId;
        playerStats.recentMatches().add(0, new MatchRecord(matchId, modeId, result, kills, deaths, safeTimestamp));
        while (playerStats.recentMatches().size() > MAX_RECENT_MATCHES) {
            playerStats.recentMatches().remove(playerStats.recentMatches().size() - 1);
        }
    }
}
