package com.dousiyo.dpvptweaks.pvpstats.util;

import com.dousiyo.dpvptweaks.pvpstats.model.AggregateStats;
import com.dousiyo.dpvptweaks.pvpstats.model.MatchRecord;
import com.dousiyo.dpvptweaks.pvpstats.model.PlayerStats;
import com.dousiyo.dpvptweaks.pvpstats.model.PlayerPrivacySettings;
import com.dousiyo.dpvptweaks.pvpstats.rank.RankState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NbtStatsCodec {
    private NbtStatsCodec() {
    }

    public static CompoundTag writePlayer(String lastKnownName, AggregateStats global, Map<String, AggregateStats> modes, List<MatchRecord> recentMatches) {
        return writePlayer(lastKnownName, global, modes, recentMatches, PlayerPrivacySettings.DEFAULT, Map.of());
    }

    public static CompoundTag writePlayer(String lastKnownName, AggregateStats global, Map<String, AggregateStats> modes, List<MatchRecord> recentMatches, PlayerPrivacySettings privacySettings) {
        return writePlayer(lastKnownName, global, modes, recentMatches, privacySettings, Map.of());
    }

    public static CompoundTag writePlayer(String lastKnownName, AggregateStats global, Map<String, AggregateStats> modes, List<MatchRecord> recentMatches, PlayerPrivacySettings privacySettings, Map<String, RankState> ranks) {
        CompoundTag playerTag = new CompoundTag();
        playerTag.putString("LastKnownName", lastKnownName == null ? "" : lastKnownName);
        playerTag.put("Global", writeAggregate(global));
        playerTag.put("Privacy", writePrivacy(privacySettings));

        ListTag modeList = new ListTag();
        for (Map.Entry<String, AggregateStats> entry : modes.entrySet()) {
            CompoundTag modeTag = new CompoundTag();
            modeTag.putString("ModeId", entry.getKey());
            modeTag.put("Stats", writeAggregate(entry.getValue()));
            modeList.add(modeTag);
        }
        playerTag.put("Modes", modeList);

        ListTag recentMatchesTag = new ListTag();
        for (MatchRecord matchRecord : recentMatches) {
            recentMatchesTag.add(writeMatch(matchRecord));
        }
        playerTag.put("RecentMatches", recentMatchesTag);

        ListTag ranksTag = new ListTag();
        for (Map.Entry<String, RankState> entry : (ranks == null ? Map.<String, RankState>of() : ranks).entrySet()) {
            CompoundTag rankTag = new CompoundTag();
            rankTag.putString("ModeId", entry.getKey());
            rankTag.putInt("Rating", entry.getValue().rating());
            rankTag.putInt("PeakRating", entry.getValue().peakRating());
            rankTag.putInt("PlacementMatches", entry.getValue().placementMatches());
            ranksTag.add(rankTag);
        }
        playerTag.put("Ranks", ranksTag);
        return playerTag;
    }

    public static PlayerStats readPlayer(CompoundTag playerTag) {
        String lastKnownName = playerTag.getString("LastKnownName");
        AggregateStats global = readAggregate(playerTag.getCompound("Global"));

        Map<String, AggregateStats> modes = new LinkedHashMap<>();
        for (Tag tag : playerTag.getList("Modes", Tag.TAG_COMPOUND)) {
            if (!(tag instanceof CompoundTag modeTag)) {
                continue;
            }
            String modeId = modeTag.getString("ModeId");
            if (modeId.isBlank()) {
                continue;
            }
            modes.put(modeId, readAggregate(modeTag.getCompound("Stats")));
        }

        List<MatchRecord> recentMatches = playerTag.getList("RecentMatches", Tag.TAG_COMPOUND).stream()
                .filter(CompoundTag.class::isInstance)
                .map(CompoundTag.class::cast)
                .map(NbtStatsCodec::readMatch)
                .toList();

        Map<String, RankState> ranks = new LinkedHashMap<>();
        for (Tag tag : playerTag.getList("Ranks", Tag.TAG_COMPOUND)) {
            if (tag instanceof CompoundTag rankTag && !rankTag.getString("ModeId").isBlank()) {
                ranks.put(rankTag.getString("ModeId"), new RankState(
                        rankTag.getInt("Rating"), rankTag.getInt("PeakRating"), rankTag.getInt("PlacementMatches")));
            }
        }
        PlayerPrivacySettings privacySettings = readPrivacy(playerTag.getCompound("Privacy"));
        return new PlayerStats(lastKnownName, global, modes, recentMatches, privacySettings, ranks);
    }

    public static CompoundTag writeAggregate(AggregateStats stats) {
        AggregateStats safeStats = stats == null ? new AggregateStats() : stats;
        CompoundTag tag = new CompoundTag();
        tag.putLong("Wins", safeStats.wins());
        tag.putLong("Losses", safeStats.losses());
        tag.putLong("Draws", safeStats.draws());
        tag.putLong("Kills", safeStats.kills());
        tag.putLong("Deaths", safeStats.deaths());
        tag.putLong("Matches", safeStats.matches());
        return tag;
    }

    public static AggregateStats readAggregate(CompoundTag tag) {
        return new AggregateStats(
                tag.getLong("Wins"),
                tag.getLong("Losses"),
                tag.getLong("Draws"),
                tag.getLong("Kills"),
                tag.getLong("Deaths"),
                tag.getLong("Matches")
        );
    }

    public static CompoundTag writeMatch(MatchRecord matchRecord) {
        CompoundTag tag = new CompoundTag();
        tag.putString("MatchId", matchRecord.matchId());
        tag.putString("ModeId", matchRecord.modeId());
        tag.putString("Result", matchRecord.result());
        tag.putInt("Kills", matchRecord.kills());
        tag.putInt("Deaths", matchRecord.deaths());
        tag.putLong("Timestamp", matchRecord.timestamp());
        return tag;
    }

    public static MatchRecord readMatch(CompoundTag tag) {
        return new MatchRecord(
                tag.getString("MatchId"),
                tag.getString("ModeId"),
                tag.getString("Result"),
                tag.getInt("Kills"),
                tag.getInt("Deaths"),
                tag.getLong("Timestamp")
        );
    }

    public static CompoundTag writePrivacy(PlayerPrivacySettings settings) {
        PlayerPrivacySettings safeSettings = settings == null ? PlayerPrivacySettings.DEFAULT : settings;
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("ShowRank", safeSettings.showRank());
        tag.putBoolean("ShowStats", safeSettings.showStats());
        tag.putBoolean("ShowMatchHistory", safeSettings.showMatchHistory());
        tag.putBoolean("JoinLeaderboards", safeSettings.joinLeaderboards());
        return tag;
    }

    public static PlayerPrivacySettings readPrivacy(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return PlayerPrivacySettings.DEFAULT;
        }
        return new PlayerPrivacySettings(
                readBooleanOrDefault(tag, "ShowRank", true),
                readBooleanOrDefault(tag, "ShowStats", true),
                readBooleanOrDefault(tag, "ShowMatchHistory", true),
                readBooleanOrDefault(tag, "JoinLeaderboards", true)
        );
    }

    private static boolean readBooleanOrDefault(CompoundTag tag, String key, boolean defaultValue) {
        return tag.contains(key, Tag.TAG_BYTE) ? tag.getBoolean(key) : defaultValue;
    }
}
