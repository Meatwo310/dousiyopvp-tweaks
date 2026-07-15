package com.dousiyo.dpvptweaks.loadout;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class SavedLoadoutPreviewLoader {
    private static final Gson GSON = new Gson();
    private static final Path DIRECTORY = FMLPaths.GAMEDIR.get().resolve("dousiyo").resolve("loadouts");

    private SavedLoadoutPreviewLoader() {}

    static LoadoutDefinition load(LoadoutSetDefinition.Entry entry) {
        Path path = DIRECTORY.resolve(entry.id() + ".json");
        if (!Files.isRegularFile(path)) return null;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null || !entry.id().equals(root.get("id").getAsString())) return null;
            List<Preview> previews = new ArrayList<>();
            JsonArray items = root.getAsJsonArray("items");
            if (items != null) for (JsonElement raw : items) {
                if (!raw.isJsonObject()) continue;
                JsonObject item = raw.getAsJsonObject();
                if (!"hotbar".equals(item.get("area").getAsString())) continue;
                int slot = item.get("slot").getAsInt();
                if (slot < 0 || slot > 2) continue;
                String itemCommand = item.get("item").getAsString();
                String previewId = gunId(itemCommand);
                if (previewId == null) previewId = baseItemId(itemCommand);
                ItemStack stack = LoadoutDefinitionLoader.createPreviewStack(previewId, "saved loadout " + entry.id());
                previews.add(new Preview(slot, stack));
            }
            previews.sort(Comparator.comparingInt(Preview::slot));
            return new LoadoutDefinition(entry.id(), entry.displayName(), "",
                    previews.stream().map(Preview::stack).toList(), entry.description(), List.of(), entry.afterApply());
        } catch (Exception e) {
            DpvpTweaks.LOGGER.error("[{}] Failed to read saved loadout preview from {}", DpvpTweaks.MOD_NAME, path, e);
            return null;
        }
    }

    private static String gunId(String command) {
        int key = command.indexOf("GunId");
        if (key < 0) return null;
        int quote = command.indexOf('"', key);
        if (quote < 0) return null;
        int end = command.indexOf('"', quote + 1);
        return end < 0 ? null : command.substring(quote + 1, end);
    }

    private static String baseItemId(String command) {
        int open = command.indexOf('(');
        if (open < 0) return "";
        int quote = command.indexOf('\'', open);
        if (quote < 0) quote = command.indexOf('"', open);
        if (quote < 0) return "";
        char delimiter = command.charAt(quote);
        boolean escaped = false;
        StringBuilder id = new StringBuilder();
        for (int i = quote + 1; i < command.length(); i++) {
            char current = command.charAt(i);
            if (escaped) {
                id.append(current);
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (current == delimiter) {
                return id.toString();
            } else {
                id.append(current);
            }
        }
        return "";
    }

    private record Preview(int slot, ItemStack stack) {}
}
