package com.dousiyo.dpvptweaks.arsenal;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.FireMode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ArsenalSavedData extends SavedData {
    private static final String DATA_ID = DpvpTweaks.MODID + "_arsenal";
    public ArsenalMatchState state = ArsenalMatchState.WAITING;
    public UUID matchId;
    public UUID winner;
    public String weaponSetId = "";
    public String previousSidebarObjective = "";
    public ArsenalWeaponSet snapshot;
    public boolean statsRecorded;
    public long countdownEndGameTime;
    public final Map<UUID, ArsenalPlayerData> players = new LinkedHashMap<>();

    public static ArsenalSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ArsenalSavedData::load, ArsenalSavedData::new, DATA_ID);
    }

    public static ArsenalSavedData load(CompoundTag root) {
        ArsenalSavedData data = new ArsenalSavedData();
        try { data.state = ArsenalMatchState.valueOf(root.getString("State")); }
        catch (IllegalArgumentException ignored) { data.state = ArsenalMatchState.WAITING; }
        if (root.hasUUID("MatchId")) data.matchId = root.getUUID("MatchId");
        if (root.hasUUID("Winner")) data.winner = root.getUUID("Winner");
        data.weaponSetId = root.getString("WeaponSetId");
        data.previousSidebarObjective = root.getString("PreviousSidebarObjective");
        data.statsRecorded = root.getBoolean("StatsRecorded");
        data.countdownEndGameTime = Math.max(0L, root.getLong("CountdownEnd"));
        if (root.contains("Snapshot", Tag.TAG_COMPOUND)) data.snapshot = readSet(root.getCompound("Snapshot"));
        for (Tag tag : root.getList("Players", Tag.TAG_COMPOUND)) {
            CompoundTag playerTag = (CompoundTag) tag;
            if (!playerTag.hasUUID("Uuid")) continue;
            ArsenalPlayerData player = new ArsenalPlayerData(playerTag.getUUID("Uuid"), playerTag.getString("Name"));
            player.stage = Math.max(0, Math.min(29, playerTag.getInt("Stage")));
            player.kills = Math.max(0, playerTag.getInt("Kills"));
            player.deaths = Math.max(0, playerTag.getInt("Deaths"));
            player.protectionEndGameTime = Math.max(0L, playerTag.getLong("ProtectionEnd"));
            data.players.put(player.playerId, player);
        }
        if (data.state == ArsenalMatchState.RUNNING && (data.snapshot == null || data.snapshot.stages().size() != 30)) {
            DpvpTweaks.LOGGER.error("Arsenal SavedData had no usable snapshot; moving match to FINISHED");
            data.state = ArsenalMatchState.FINISHED;
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        root.putString("State", state.name());
        if (matchId != null) root.putUUID("MatchId", matchId);
        if (winner != null) root.putUUID("Winner", winner);
        root.putString("WeaponSetId", weaponSetId);
        root.putString("PreviousSidebarObjective", previousSidebarObjective);
        root.putBoolean("StatsRecorded", statsRecorded);
        root.putLong("CountdownEnd", countdownEndGameTime);
        if (snapshot != null) root.put("Snapshot", writeSet(snapshot));
        ListTag playerList = new ListTag();
        for (ArsenalPlayerData player : players.values()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Uuid", player.playerId);
            tag.putString("Name", player.lastKnownName);
            tag.putInt("Stage", player.stage);
            tag.putInt("Kills", player.kills);
            tag.putInt("Deaths", player.deaths);
            tag.putLong("ProtectionEnd", player.protectionEndGameTime);
            playerList.add(tag);
        }
        root.put("Players", playerList);
        return root;
    }

    public void clearMatch() {
        state = ArsenalMatchState.WAITING; matchId = null; winner = null; weaponSetId = "";
        previousSidebarObjective = ""; snapshot = null;
        statsRecorded = false; countdownEndGameTime = 0L; players.clear(); setDirty();
    }

    private static CompoundTag writeSet(ArsenalWeaponSet set) {
        CompoundTag root = new CompoundTag();
        root.putInt("Schema", set.schemaVersion()); root.putString("Id", set.id()); root.putString("DisplayName", set.displayName());
        ListTag stages = new ListTag();
        for (ArsenalWeaponStage stage : set.stages()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Type", stage.type().name());
            if (stage.type() == ArsenalWeaponStage.Type.ITEM) {
                tag.put("ItemTemplate", stage.itemTemplate().save(new CompoundTag()));
            } else {
                tag.putString("GunId", stage.gunId().toString()); tag.putString("FireMode", stage.fireMode().name());
                tag.putInt("ReserveMagazines", stage.reserveMagazines());
                CompoundTag attachments = new CompoundTag();
                stage.attachments().forEach((type, id) -> attachments.putString(type.name(), id.toString()));
                tag.put("Attachments", attachments);
            }
            stages.add(tag);
        }
        root.put("Stages", stages); return root;
    }

    private static ArsenalWeaponSet readSet(CompoundTag root) {
        List<ArsenalWeaponStage> stages = new ArrayList<>();
        for (Tag raw : root.getList("Stages", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag) raw;
            if ("ITEM".equals(tag.getString("Type"))) {
                ItemStack item = ItemStack.of(tag.getCompound("ItemTemplate"));
                if (item.isEmpty()) return null;
                stages.add(ArsenalWeaponStage.item(item));
                continue;
            }
            ResourceLocation gunId = ResourceLocation.tryParse(tag.getString("GunId"));
            FireMode fireMode;
            try { fireMode = FireMode.valueOf(tag.getString("FireMode")); } catch (IllegalArgumentException ex) { return null; }
            if (gunId == null) return null;
            EnumMap<AttachmentType, ResourceLocation> attachments = new EnumMap<>(AttachmentType.class);
            CompoundTag attachmentTag = tag.getCompound("Attachments");
            for (String key : attachmentTag.getAllKeys()) {
                try {
                    AttachmentType type = AttachmentType.valueOf(key);
                    ResourceLocation id = ResourceLocation.tryParse(attachmentTag.getString(key));
                    if (id != null) attachments.put(type, id);
                } catch (IllegalArgumentException ignored) {}
            }
            stages.add(new ArsenalWeaponStage(gunId, fireMode, attachments, tag.getInt("ReserveMagazines")));
        }
        if (stages.size() != ArsenalWeaponSet.STAGE_COUNT) return null;
        return new ArsenalWeaponSet(root.getInt("Schema"), root.getString("Id"), root.getString("DisplayName"), stages);
    }
}
