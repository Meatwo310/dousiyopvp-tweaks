package com.dousiyo.dpvptweaks.pvpstats.util;

import com.dousiyo.dpvptweaks.pvpstats.model.AggregateStats;
import com.dousiyo.dpvptweaks.pvpstats.model.MatchRecord;
import com.dousiyo.dpvptweaks.pvpstats.model.PlayerStats;
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
        CompoundTag playerTag = new CompoundTag();
        playerTag.putString("LastKnownName", lastKnownName == null ? "" : lastKnownName);
        playerTag.put("Global", writeAggregate(global));

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

        return new PlayerStats(lastKnownName, global, modes, recentMatches);
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
}
