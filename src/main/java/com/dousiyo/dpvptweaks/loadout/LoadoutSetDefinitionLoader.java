package com.dousiyo.dpvptweaks.loadout;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class LoadoutSetDefinitionLoader {
    private LoadoutSetDefinitionLoader() {
    }

    public static LoadoutSetDefinition fromDataPack(ResourceLocation id, JsonElement element, String logName) {
        if (id == null || element == null || !element.isJsonObject()) {
            return null;
        }

        JsonObject root = element.getAsJsonObject();
        JsonArray loadoutArray = getArray(root, "loadouts");
        List<ResourceLocation> loadouts = new ArrayList<>();
        if (loadoutArray != null) {
            for (JsonElement loadoutElement : loadoutArray) {
                String raw = loadoutElement == null || !loadoutElement.isJsonPrimitive() ? "" : loadoutElement.getAsString().trim();
                ResourceLocation loadoutId = parseResourceLocation(raw, id.getNamespace());
                if (loadoutId == null) {
                    DpvpTweaks.LOGGER.warn("[{}] {} has invalid loadout id in set '{}': {}", DpvpTweaks.MOD_NAME, logName, id, raw);
                    continue;
                }
                loadouts.add(loadoutId);
            }
        }

        return new LoadoutSetDefinition(
                id,
                getString(root, "title", id.toString()),
                getString(root, "layout", "normal"),
                loadouts
        );
    }

    private static ResourceLocation parseResourceLocation(String raw, String defaultNamespace) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (!trimmed.contains(":")) {
            return ResourceLocation.tryParse(defaultNamespace + ":" + trimmed);
        }
        return ResourceLocation.tryParse(trimmed);
    }

    private static JsonArray getArray(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static String getString(JsonObject object, String key, String defaultValue) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return defaultValue;
        }
        try {
            return element.getAsString();
        } catch (ClassCastException | IllegalStateException e) {
            return defaultValue;
        }
    }
}
