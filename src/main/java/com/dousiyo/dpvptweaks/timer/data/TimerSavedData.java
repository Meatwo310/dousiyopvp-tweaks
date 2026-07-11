package com.dousiyo.dpvptweaks.timer.data;

import com.dousiyo.dpvptweaks.timer.core.TimerDefinition;
import com.dousiyo.dpvptweaks.timer.core.TimerMode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

public class TimerSavedData extends SavedData {
    private static final String DATA_ID = "meatwo310_timer_definitions";
    private final Map<String, TimerDefinition> definitions = new LinkedHashMap<>();

    public static TimerSavedData get(ServerLevel anyLevel) {
        ServerLevel overworld = anyLevel.getServer().overworld();
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(TimerSavedData::load, TimerSavedData::new, DATA_ID);
    }

    public static TimerSavedData load(CompoundTag root) {
        TimerSavedData data = new TimerSavedData();
        ListTag list = root.getList("definitions", Tag.TAG_COMPOUND);
        for (Tag tag : list) {
            CompoundTag defTag = (CompoundTag) tag;
            String id = defTag.getString("id");
            TimerMode mode = TimerMode.valueOf(defTag.getString("mode"));
            int durationTicks = Math.max(0, defTag.getInt("durationTicks"));
            @Nullable Component defaultTitle = null;
            if (defTag.contains("defaultTitle", Tag.TAG_STRING)) {
                String titleJson = defTag.getString("defaultTitle");
                if (!titleJson.isBlank()) {
                    defaultTitle = Component.Serializer.fromJson(titleJson);
                }
            }

            TimerDefinition definition = new TimerDefinition(id, mode, durationTicks, defaultTitle);
            if (defTag.contains("finishMessage", Tag.TAG_STRING)) {
                String messageJson = defTag.getString("finishMessage");
                if (!messageJson.isBlank()) {
                    definition.setFinishMessage(Component.Serializer.fromJson(messageJson));
                }
            }
            ListTag onFinishList = defTag.getList("onFinishCommands", Tag.TAG_STRING);
            for (Tag cmdTag : onFinishList) {
                definition.addOnFinishCommand(cmdTag.getAsString());
            }
            data.definitions.put(id, definition);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        ListTag list = new ListTag();
        for (TimerDefinition definition : definitions.values()) {
            CompoundTag defTag = new CompoundTag();
            defTag.putString("id", definition.getId());
            defTag.putString("mode", definition.getMode().name());
            defTag.putInt("durationTicks", definition.getDurationTicks());
            if (definition.getDefaultTitle() != null) {
                defTag.putString("defaultTitle", Component.Serializer.toJson(definition.getDefaultTitle()));
            }
            if (definition.getFinishMessage() != null) {
                defTag.putString("finishMessage", Component.Serializer.toJson(definition.getFinishMessage()));
            }
            ListTag onFinishList = new ListTag();
            for (String command : definition.getOnFinishCommands()) {
                onFinishList.add(StringTag.valueOf(command));
            }
            defTag.put("onFinishCommands", onFinishList);
            list.add(defTag);
        }
        root.put("definitions", list);
        return root;
    }

    public Map<String, TimerDefinition> getDefinitions() {
        return definitions;
    }
}
