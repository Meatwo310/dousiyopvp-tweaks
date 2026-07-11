package com.dousiyo.dpvptweaks.pvpstats.mode;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.pvpstats.service.ModeIdService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class PvpModeDefinitionLoader {
    private PvpModeDefinitionLoader() {
    }

    public static PvpModeDefinition fromDataPack(ResourceLocation resourceId, JsonElement element) {
        if (resourceId == null || element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject root = element.getAsJsonObject();
        String modeId = getString(root, "mode_id", resourceId.toString()).toLowerCase(Locale.ROOT);
        if (!ModeIdService.isValid(modeId)) {
            DpvpTweaks.LOGGER.warn("[{}] Invalid PvP mode id '{}' in {}", DpvpTweaks.MOD_NAME, modeId, resourceId);
            return null;
        }

        ResourceLocation icon = ResourceLocation.tryParse(getString(root, "icon", ""));
        Set<String> tags = new LinkedHashSet<>();
        JsonArray tagArray = getArray(root, "tags");
        if (tagArray != null) {
            for (JsonElement tagElement : tagArray) {
                if (tagElement != null && tagElement.isJsonPrimitive()) {
                    String tag = tagElement.getAsString().trim().toLowerCase(Locale.ROOT);
                    if (!tag.isBlank()) {
                        tags.add(tag);
                    }
                }
            }
        }

        JsonObject ranking = getObject(root, "ranking");
        return new PvpModeDefinition(
                modeId,
                getString(root, "display_name", modeId),
                getString(root, "translation_key", ""),
                getString(root, "description", ""),
                getString(root, "description_translation_key", ""),
                icon,
                getInt(root, "sort_order", 1000),
                tags,
                getBoolean(root, "visible", true),
                ranking == null || getBoolean(ranking, "enabled", true),
                ranking == null ? 10L : getLong(ranking, "min_matches", 10L),
                ranking == null ? 20L : getLong(ranking, "min_kills", 20L)
        );
    }

    private static String getString(JsonObject object, String key, String fallback) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString().trim() : fallback;
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        JsonElement value = object.get(key);
        try {
            return value != null && value.isJsonPrimitive() ? value.getAsInt() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static long getLong(JsonObject object, String key, long fallback) {
        JsonElement value = object.get(key);
        try {
            return value != null && value.isJsonPrimitive() ? value.getAsLong() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static boolean getBoolean(JsonObject object, String key, boolean fallback) {
        JsonElement value = object.get(key);
        try {
            return value != null && value.isJsonPrimitive() ? value.getAsBoolean() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static JsonArray getArray(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonArray() ? value.getAsJsonArray() : null;
    }

    private static JsonObject getObject(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }
}
