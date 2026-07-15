package com.dousiyo.dpvptweaks.temporarybuilding;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class TemporaryBuildingSavedData extends SavedData {
    private static final String DATA_ID = "dpvptweaks_temporary_buildings";
    private final Map<String, LongOpenHashSet> positions = new LinkedHashMap<>();
    private UUID matchId;
    private boolean resetRequired;

    public static TemporaryBuildingSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                TemporaryBuildingSavedData::load, TemporaryBuildingSavedData::new, DATA_ID);
    }

    public static TemporaryBuildingSavedData load(CompoundTag tag) {
        TemporaryBuildingSavedData data = new TemporaryBuildingSavedData();
        if (tag.hasUUID("MatchId")) data.matchId = tag.getUUID("MatchId");
        data.resetRequired = tag.getBoolean("ResetRequired");
        ListTag dimensions = tag.getList("Dimensions", Tag.TAG_COMPOUND);
        for (int i = 0; i < dimensions.size(); i++) {
            CompoundTag dimension = dimensions.getCompound(i);
            LongOpenHashSet set = new LongOpenHashSet(dimension.getLongArray("Positions"));
            if (!set.isEmpty()) data.positions.put(dimension.getString("DimensionId"), set);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (matchId != null) tag.putUUID("MatchId", matchId);
        tag.putBoolean("ResetRequired", resetRequired);
        ListTag dimensions = new ListTag();
        positions.forEach((id, values) -> {
            CompoundTag dimension = new CompoundTag();
            dimension.putString("DimensionId", id);
            dimension.putLongArray("Positions", values.toLongArray());
            dimensions.add(dimension);
        });
        tag.put("Dimensions", dimensions);
        return tag;
    }

    public void begin(UUID id) {
        matchId = id;
        resetRequired = true;
        setDirty();
    }

    public void add(String dimension, long position) {
        positions.computeIfAbsent(dimension, ignored -> new LongOpenHashSet()).add(position);
        setDirty();
    }

    public void remove(String dimension, long position) {
        LongOpenHashSet set = positions.get(dimension);
        if (set == null) return;
        set.remove(position);
        if (set.isEmpty()) positions.remove(dimension);
        setDirty();
    }

    public Map<String, long[]> snapshot() {
        Map<String, long[]> copy = new LinkedHashMap<>();
        positions.forEach((id, values) -> copy.put(id, values.toLongArray()));
        return copy;
    }

    public UUID matchId() { return matchId; }
    public boolean needsRecovery() { return resetRequired && !positions.isEmpty(); }
    public int size() { return positions.values().stream().mapToInt(LongOpenHashSet::size).sum(); }

    public void completeReset() {
        positions.clear();
        matchId = null;
        resetRequired = false;
        setDirty();
    }
}
