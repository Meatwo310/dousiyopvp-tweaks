package com.dousiyo.dpvptweaks.loadout;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class LoadoutSetDefinitionLoader {
    private LoadoutSetDefinitionLoader() {}

    public static LoadoutSetDefinition fromDataPack(ResourceLocation id, JsonElement element, String logName) {
        if (id == null || element == null || !element.isJsonObject()) return null;
        JsonObject root = element.getAsJsonObject();
        JsonArray array = root.has("loadouts") && root.get("loadouts").isJsonArray()
                ? root.getAsJsonArray("loadouts") : new JsonArray();
        List<LoadoutSetDefinition.Entry> entries = new ArrayList<>();
        for (JsonElement raw : array) {
            String loadoutId;
            String displayName;
            String description;
            String afterApply;
            LoadoutSetDefinition.RandomDefinition random = null;
            if (raw.isJsonPrimitive()) {
                loadoutId = raw.getAsString().trim();
                displayName = loadoutId;
                description = "";
                afterApply = "";
            } else if (raw.isJsonObject()) {
                JsonObject object = raw.getAsJsonObject();
                loadoutId = string(object, "id", "").trim();
                displayName = string(object, "display_name", loadoutId);
                description = string(object, "description", "");
                afterApply = string(object, "after_apply", "").trim();
                if (object.has("random") && object.get("random").isJsonObject()) {
                    JsonObject randomObject = object.getAsJsonObject("random");
                    String profile = string(randomObject, "profile", "").trim();
                    String template = string(randomObject, "template", "").trim();
                    int weaponCount = integer(randomObject, "weapon_count", 0);
                    if (!validId(profile) || !validId(template) || (weaponCount != 2 && weaponCount != 3)) {
                        DpvpTweaks.LOGGER.warn("[{}] Invalid random definition in set {} for entry {}", logName, id, loadoutId);
                        continue;
                    }
                    random = new LoadoutSetDefinition.RandomDefinition(profile, template, weaponCount);
                }
            } else continue;

            if (!validId(loadoutId)) {
                DpvpTweaks.LOGGER.warn("[{}] Invalid saved loadout id in set {}: {}", logName, id, loadoutId);
                continue;
            }
            ResourceLocation function = afterApply.isBlank() ? null : ResourceLocation.tryParse(afterApply);
            if (!afterApply.isBlank() && function == null) {
                DpvpTweaks.LOGGER.warn("[{}] Invalid after_apply in set {}: {}", logName, id, afterApply);
                continue;
            }
            entries.add(new LoadoutSetDefinition.Entry(loadoutId, displayName, description, function, random));
        }
        return new LoadoutSetDefinition(id, string(root, "display_name", id.getPath()), entries);
    }

    private static String string(JsonObject object, String key, String fallback) {
        try { return object.has(key) ? object.get(key).getAsString() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static int integer(JsonObject object, String key, int fallback) {
        try { return object.has(key) ? object.get(key).getAsInt() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static boolean validId(String id) {
        return id != null && id.matches("[a-z0-9][a-z0-9._-]{0,63}");
    }
}
