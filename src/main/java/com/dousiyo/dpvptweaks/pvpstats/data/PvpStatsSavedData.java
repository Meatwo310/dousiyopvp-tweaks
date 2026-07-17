package com.dousiyo.dpvptweaks.pvpstats.data;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.pvpstats.model.PlayerStats;
import com.dousiyo.dpvptweaks.pvpstats.util.NbtStatsCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class PvpStatsSavedData extends SavedData {
    public static final String DATA_NAME = DpvpTweaks.MODID + "_pvp_stats";
    private static final int MAX_PROCESSED_MATCH_IDS = 4096;

    private final Map<UUID, PlayerStats> players = new LinkedHashMap<>();
    private final LinkedHashSet<String> processedMatchIds = new LinkedHashSet<>();
    private final Map<UUID, LinkedHashSet<String>> awardedBadges = new LinkedHashMap<>();

    public static PvpStatsSavedData load(CompoundTag tag) {
        PvpStatsSavedData data = new PvpStatsSavedData();
        ListTag playersTag = tag.getList("Players", Tag.TAG_COMPOUND);
        for (Tag element : playersTag) {
            if (!(element instanceof CompoundTag playerTag) || !playerTag.hasUUID("Uuid")) {
                continue;
            }
            UUID uuid = playerTag.getUUID("Uuid");
            data.players.put(uuid, NbtStatsCodec.readPlayer(playerTag));
        }
        for (Tag element : tag.getList("ProcessedMatchIds", Tag.TAG_STRING)) {
            String matchId = element.getAsString();
            if (!matchId.isBlank()) {
                data.processedMatchIds.add(matchId);
            }
        }
        for (Tag element : tag.getList("AwardedBadges", Tag.TAG_COMPOUND)) {
            if (!(element instanceof CompoundTag awardTag) || !awardTag.hasUUID("Uuid")) {
                continue;
            }
            LinkedHashSet<String> badges = new LinkedHashSet<>();
            for (Tag badgeTag : awardTag.getList("Badges", Tag.TAG_STRING)) {
                String badgeId = badgeTag.getAsString();
                if (!badgeId.isBlank()) {
                    badges.add(badgeId);
                }
            }
            if (!badges.isEmpty()) {
                data.awardedBadges.put(awardTag.getUUID("Uuid"), badges);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag playersTag = new ListTag();
        for (Map.Entry<UUID, PlayerStats> entry : players.entrySet()) {
            CompoundTag playerTag = NbtStatsCodec.writePlayer(
                    entry.getValue().lastKnownName(),
                    entry.getValue().global(),
                    entry.getValue().modes(),
                    entry.getValue().recentMatches(),
                    entry.getValue().privacySettings(),
                    entry.getValue().ranks()
            );
            playerTag.putUUID("Uuid", entry.getKey());
            playersTag.add(playerTag);
        }
        tag.put("Players", playersTag);
        ListTag processedIdsTag = new ListTag();
        for (String matchId : processedMatchIds) {
            processedIdsTag.add(StringTag.valueOf(matchId));
        }
        tag.put("ProcessedMatchIds", processedIdsTag);
        ListTag awardedBadgesTag = new ListTag();
        for (Map.Entry<UUID, LinkedHashSet<String>> entry : awardedBadges.entrySet()) {
            CompoundTag awardTag = new CompoundTag();
            awardTag.putUUID("Uuid", entry.getKey());
            ListTag badgesTag = new ListTag();
            for (String badgeId : entry.getValue()) {
                badgesTag.add(StringTag.valueOf(badgeId));
            }
            awardTag.put("Badges", badgesTag);
            awardedBadgesTag.add(awardTag);
        }
        tag.put("AwardedBadges", awardedBadgesTag);
        return tag;
    }

    public Map<UUID, PlayerStats> players() {
        return players;
    }

    public PlayerStats get(UUID uuid) {
        return players.get(uuid);
    }

    public PlayerStats getOrCreate(UUID uuid) {
        return players.computeIfAbsent(uuid, ignored -> new PlayerStats());
    }

    public boolean remove(UUID uuid) {
        return players.remove(uuid) != null;
    }

    public boolean hasProcessedMatch(String matchId) {
        return matchId != null && processedMatchIds.contains(matchId);
    }

    public void markProcessedMatch(String matchId) {
        if (matchId == null || matchId.isBlank() || !processedMatchIds.add(matchId)) {
            return;
        }
        while (processedMatchIds.size() > MAX_PROCESSED_MATCH_IDS) {
            processedMatchIds.remove(processedMatchIds.iterator().next());
        }
        setDirty();
    }

    public Set<String> processedMatchIds() {
        return Set.copyOf(processedMatchIds);
    }

    public Set<String> awardedBadges(UUID uuid) {
        Set<String> badges = awardedBadges.get(uuid);
        return badges == null ? Set.of() : Set.copyOf(badges);
    }

    public boolean awardBadge(UUID uuid, String badgeId) {
        boolean added = awardedBadges.computeIfAbsent(uuid, ignored -> new LinkedHashSet<>()).add(badgeId);
        if (added) {
            setDirty();
        }
        return added;
    }

    public boolean revokeBadge(UUID uuid, String badgeId) {
        LinkedHashSet<String> badges = awardedBadges.get(uuid);
        if (badges == null || !badges.remove(badgeId)) {
            return false;
        }
        if (badges.isEmpty()) {
            awardedBadges.remove(uuid);
        }
        setDirty();
        return true;
    }
}
