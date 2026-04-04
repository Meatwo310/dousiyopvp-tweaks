package com.dousiyo.dpvptweaks.pvpstats.data;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.pvpstats.model.PlayerStats;
import com.dousiyo.dpvptweaks.pvpstats.util.NbtStatsCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PvpStatsSavedData extends SavedData {
    public static final String DATA_NAME = DpvpTweaks.MODID + "_pvp_stats";

    private final Map<UUID, PlayerStats> players = new LinkedHashMap<>();

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
                    entry.getValue().recentMatches()
            );
            playerTag.putUUID("Uuid", entry.getKey());
            playersTag.add(playerTag);
        }
        tag.put("Players", playersTag);
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
}
