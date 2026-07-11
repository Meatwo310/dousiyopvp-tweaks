package com.dousiyo.dpvptweaks.loadout;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class LoadoutDefinitionLoader {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private LoadoutDefinitionLoader() {
    }

    public static List<LoadoutDefinition> load(String jsonFileName, String logName) {
        Path jsonPath = resolveJsonPath(jsonFileName);
        ensureDefaultJson(jsonPath, jsonFileName, logName);

        if (!Files.exists(jsonPath)) {
            DpvpTweaks.LOGGER.warn("[{}] {} definition json was not found: {}", DpvpTweaks.MOD_NAME, logName, jsonPath);
            return List.of();
        }

        Root root;
        try (Reader reader = Files.newBufferedReader(jsonPath, StandardCharsets.UTF_8)) {
            root = GSON.fromJson(reader, Root.class);
        } catch (IOException | JsonParseException e) {
            DpvpTweaks.LOGGER.error("[{}] Failed to read {} definition from {}", DpvpTweaks.MOD_NAME, logName, jsonPath, e);
            return List.of();
        }

        if (root == null || root.loadouts == null) {
            return List.of();
        }

        List<LoadoutDefinition> loaded = new ArrayList<>();
        for (LoadoutEntry entry : root.loadouts) {
            LoadoutDefinition loadout = toLoadout(entry, logName);
            if (loadout != null) {
                loaded.add(loadout);
            }
        }
        return List.copyOf(loaded);
    }

    public static LoadoutDefinition fromDataPack(ResourceLocation id, JsonElement element, String logName) {
        if (id == null || element == null || !element.isJsonObject()) {
            return null;
        }

        JsonObject root = element.getAsJsonObject();
        JsonObject display = getObject(root, "display");
        String name = getString(display, "name", id.toString()).trim();
        if (name.isEmpty()) {
            name = id.toString();
        }

        List<ItemStack> stacks = new ArrayList<>();
        JsonArray previewItems = getArray(display, "preview_items");
        if (previewItems == null) {
            previewItems = getArray(root, "items");
        }
        if (previewItems != null) {
            int index = 0;
            for (JsonElement itemElement : previewItems) {
                ItemStack stack = toPreviewStack(itemElement, logName, id.toString(), index);
                if (stack != null) {
                    stacks.add(stack);
                }
                index++;
            }
        }

        JsonObject conditions = getObject(root, "conditions");
        JsonObject apply = getObject(root, "apply");
        ResourceLocation applyFunction = parseOptionalResourceLocation(
                getString(apply, "function", ""),
                logName,
                id.toString(),
                "apply.function"
        );

        return new LoadoutDefinition(
                id.toString(),
                name,
                getString(display, "weapons", getString(root, "weapons", "")),
                stacks,
                getString(display, "description", getString(root, "description", "")),
                getStringList(conditions, "teams"),
                applyFunction
        );
    }

    private static Path resolveJsonPath(String jsonFileName) {
        return FMLPaths.GAMEDIR.get().resolve("dousiyo").resolve(jsonFileName);
    }

    private static void ensureDefaultJson(Path jsonPath, String jsonFileName, String logName) {
        if (Files.exists(jsonPath)) {
            return;
        }

        String resourcePath = "/assets/" + DpvpTweaks.MODID + "/defaults/" + jsonFileName;
        try {
            Files.createDirectories(jsonPath.getParent());
            try (InputStream input = LoadoutDefinitionLoader.class.getResourceAsStream(resourcePath)) {
                if (input == null) {
                    return;
                }
                Files.copy(input, jsonPath, StandardCopyOption.REPLACE_EXISTING);
            }
            DpvpTweaks.LOGGER.info("[{}] Created default {} definition json: {}", DpvpTweaks.MOD_NAME, logName, jsonPath);
        } catch (IOException e) {
            DpvpTweaks.LOGGER.error("[{}] Failed to create default {} definition json at {}", DpvpTweaks.MOD_NAME, logName, jsonPath, e);
        }
    }

    private static LoadoutDefinition toLoadout(LoadoutEntry entry, String logName) {
        if (entry == null || entry.id == null || entry.name == null) {
            return null;
        }

        String id = entry.id.trim();
        String name = entry.name.trim();
        if (id.isEmpty() || name.isEmpty()) {
            return null;
        }

        List<ItemStack> stacks = new ArrayList<>();
        if (entry.items != null) {
            for (String itemId : entry.items) {
                ResourceLocation location = itemId == null ? null : ResourceLocation.tryParse(itemId.trim());
                if (location == null) {
                    DpvpTweaks.LOGGER.warn("[{}] {} has invalid item id in loadout '{}': {}", DpvpTweaks.MOD_NAME, logName, id, itemId);
                    stacks.add(new ItemStack(Items.BARRIER));
                    continue;
                }
                stacks.add(createPreviewStack(location, logName));
            }
        }

        return new LoadoutDefinition(
                id,
                name,
                entry.weapons == null ? "" : entry.weapons,
                stacks,
                entry.description == null ? "" : entry.description
        );
    }

    public static ItemStack createPreviewStack(String itemId, String logName) {
        ResourceLocation location = itemId == null ? null : ResourceLocation.tryParse(itemId.trim());
        if (location == null) {
            DpvpTweaks.LOGGER.warn("[{}] {} has invalid item id: {}", DpvpTweaks.MOD_NAME, logName, itemId);
            return new ItemStack(Items.BARRIER);
        }
        return createPreviewStack(location, logName);
    }

    private static ItemStack createPreviewStack(ResourceLocation itemId, String logName) {
        if (isGunNamespace(itemId.getNamespace())) {
            return createGunStack(itemId, logName);
        }

        return createDirectStack(itemId, logName);
    }

    private static ItemStack createGunStack(ResourceLocation gunId, String logName) {
        ResourceLocation baseItemId = ResourceLocation.fromNamespaceAndPath("tacz", "modern_kinetic_gun");
        Item base = ForgeRegistries.ITEMS.getValue(baseItemId);
        if (base == null) {
            DpvpTweaks.LOGGER.warn("[{}] {} missing base gun item: {}", DpvpTweaks.MOD_NAME, logName, baseItemId);
            return new ItemStack(Items.BARRIER);
        }
        ItemStack stack = new ItemStack(base);
        stack.getOrCreateTag().putString("GunId", gunId.toString());
        return stack;
    }

    private static ItemStack createDirectStack(ResourceLocation itemId, String logName) {
        Item direct = ForgeRegistries.ITEMS.getValue(itemId);
        if (direct == null) {
            DpvpTweaks.LOGGER.warn("[{}] {} missing item: {}", DpvpTweaks.MOD_NAME, logName, itemId);
            return new ItemStack(Items.BARRIER);
        }
        return new ItemStack(direct);
    }

    private static boolean isGunNamespace(String namespace) {
        return "tacz".equals(namespace) || "maxstuff".equals(namespace) || "elitex".equals(namespace) || "cib".equals(namespace);
    }

    private static ItemStack toPreviewStack(JsonElement element, String logName, String ownerId, int index) {
        if (element == null || element.isJsonNull()) {
            return ItemStack.EMPTY;
        }

        try {
            if (element.isJsonPrimitive()) {
                return createPreviewStack(element.getAsString(), logName);
            }
            if (!element.isJsonObject()) {
                return ItemStack.EMPTY;
            }

            JsonObject object = element.getAsJsonObject();
            String type = getString(object, "type", "").trim();
            String rawId = getString(object, "id", "");
            if (rawId.isBlank()) {
                rawId = getString(object, "item", "");
            }

            ResourceLocation location = parseOptionalResourceLocation(rawId, logName, ownerId, "display.preview_items[" + index + "]");
            if (location == null) {
                return new ItemStack(Items.BARRIER);
            }
            if ("tacz_gun".equals(type) || "gun".equals(type)) {
                return createGunStack(location, logName);
            }
            if ("item".equals(type)) {
                return createDirectStack(location, logName);
            }
            return createPreviewStack(location, logName);
        } catch (IllegalStateException e) {
            DpvpTweaks.LOGGER.warn("[{}] {} has invalid preview item in loadout '{}'", DpvpTweaks.MOD_NAME, logName, ownerId, e);
            return new ItemStack(Items.BARRIER);
        }
    }

    private static ResourceLocation parseOptionalResourceLocation(String raw, String logName, String ownerId, String fieldName) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        ResourceLocation location = ResourceLocation.tryParse(raw.trim());
        if (location == null) {
            DpvpTweaks.LOGGER.warn("[{}] {} has invalid {} in loadout '{}': {}", DpvpTweaks.MOD_NAME, logName, fieldName, ownerId, raw);
        }
        return location;
    }

    private static JsonObject getObject(JsonObject object, String key) {
        if (object == null) {
            return null;
        }
        JsonElement element = object.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static JsonArray getArray(JsonObject object, String key) {
        if (object == null) {
            return null;
        }
        JsonElement element = object.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static String getString(JsonObject object, String key, String defaultValue) {
        if (object == null) {
            return defaultValue;
        }
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

    private static List<String> getStringList(JsonObject object, String key) {
        JsonArray array = getArray(object, key);
        if (array == null) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        for (JsonElement element : array) {
            if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
                continue;
            }
            String value = element.getAsString().trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }

    private static final class Root {
        List<LoadoutEntry> loadouts = List.of();
    }

    private static final class LoadoutEntry {
        String id;
        String name;
        String weapons;
        String description;
        List<String> items = List.of();
    }
}
