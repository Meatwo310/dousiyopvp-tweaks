package com.dousiyo.dpvptweaks.inteldraft;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

public final class IntelDraftDefinitionLoader {
    public static final String JSON_FILE_NAME = "intel_draft_gui.json";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private IntelDraftDefinitionLoader() {
    }

    public static IntelDraftDefinition load() {
        Path jsonPath = resolveJsonPath();
        ensureDefaultJson(jsonPath);

        if (!Files.exists(jsonPath)) {
            DpvpTweaks.LOGGER.warn("[{}] Intel Draft definition json was not found: {}", DpvpTweaks.MOD_NAME, jsonPath);
            return IntelDraftDefinition.empty();
        }

        Root root;
        try (Reader reader = Files.newBufferedReader(jsonPath, StandardCharsets.UTF_8)) {
            root = GSON.fromJson(reader, Root.class);
        } catch (IOException | JsonParseException e) {
            DpvpTweaks.LOGGER.error("[{}] Failed to read Intel Draft definition from {}", DpvpTweaks.MOD_NAME, jsonPath, e);
            return IntelDraftDefinition.empty();
        }

        if (root == null) {
            return IntelDraftDefinition.empty();
        }

        List<IntelDraftDefinition.TechDefinition> techs = loadTechs(root.techs);
        List<IntelDraftDefinition.GunDefinition> guns = loadGuns(root.guns);
        int count = Math.min(3, Math.min(techs.size(), guns.size()));
        List<IntelDraftDefinition.ChoiceDefinition> choices = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            choices.add(new IntelDraftDefinition.ChoiceDefinition(techs.get(i), guns.get(i)));
        }
        return new IntelDraftDefinition(0L, Math.max(0, root.rerollCount), choices);
    }

    private static Path resolveJsonPath() {
        return FMLPaths.GAMEDIR.get().resolve("dousiyo").resolve(JSON_FILE_NAME);
    }

    private static void ensureDefaultJson(Path jsonPath) {
        if (Files.exists(jsonPath)) {
            return;
        }

        String resourcePath = "/assets/" + DpvpTweaks.MODID + "/defaults/" + JSON_FILE_NAME;
        try {
            Files.createDirectories(jsonPath.getParent());
            try (InputStream input = IntelDraftDefinitionLoader.class.getResourceAsStream(resourcePath)) {
                if (input == null) {
                    return;
                }
                Files.copy(input, jsonPath, StandardCopyOption.REPLACE_EXISTING);
            }
            DpvpTweaks.LOGGER.info("[{}] Created default Intel Draft definition json: {}", DpvpTweaks.MOD_NAME, jsonPath);
        } catch (IOException e) {
            DpvpTweaks.LOGGER.error("[{}] Failed to create default Intel Draft definition json at {}", DpvpTweaks.MOD_NAME, jsonPath, e);
        }
    }

    private static List<IntelDraftDefinition.TechDefinition> loadTechs(List<TechEntry> entries) {
        if (entries == null) {
            return List.of();
        }

        List<IntelDraftDefinition.TechDefinition> techs = new ArrayList<>();
        for (TechEntry entry : entries) {
            if (entry == null || entry.id < 0 || isBlank(entry.name)) {
                continue;
            }
            techs.add(new IntelDraftDefinition.TechDefinition(
                    entry.id,
                    entry.name.trim(),
                    entry.description == null ? "" : entry.description,
                    createStack(entry.iconItem, "tech icon", Integer.toString(entry.id))
            ));
        }
        return List.copyOf(techs);
    }

    private static List<IntelDraftDefinition.GunDefinition> loadGuns(List<GunEntry> entries) {
        if (entries == null) {
            return List.of();
        }

        List<IntelDraftDefinition.GunDefinition> guns = new ArrayList<>();
        for (GunEntry entry : entries) {
            if (entry == null || entry.id < 0 || isBlank(entry.name) || isBlank(entry.item)) {
                continue;
            }
            guns.add(new IntelDraftDefinition.GunDefinition(
                    entry.id,
                    entry.name.trim(),
                    createStack(entry.item, "gun", Integer.toString(entry.id))
            ));
        }
        return List.copyOf(guns);
    }

    private static ItemStack createStack(String itemId, String kind, String ownerId) {
        ResourceLocation location = itemId == null ? null : ResourceLocation.tryParse(itemId.trim());
        if (location == null) {
            DpvpTweaks.LOGGER.warn("[{}] Intel Draft has invalid {} item id in '{}': {}", DpvpTweaks.MOD_NAME, kind, ownerId, itemId);
            return new ItemStack(Items.BARRIER);
        }

        String namespace = location.getNamespace();
        if ("tacz".equals(namespace) || "maxstuff".equals(namespace) || "elitex".equals(namespace) || "cib".equals(namespace)) {
            ResourceLocation baseItemId = ResourceLocation.fromNamespaceAndPath("tacz", "modern_kinetic_gun");
            Item base = ForgeRegistries.ITEMS.getValue(baseItemId);
            if (base == null) {
                DpvpTweaks.LOGGER.warn("[{}] Intel Draft missing base gun item: {}", DpvpTweaks.MOD_NAME, baseItemId);
                return new ItemStack(Items.BARRIER);
            }
            ItemStack stack = new ItemStack(base);
            stack.getOrCreateTag().putString("GunId", location.toString());
            return stack;
        }

        Item direct = ForgeRegistries.ITEMS.getValue(location);
        if (direct == null) {
            DpvpTweaks.LOGGER.warn("[{}] Intel Draft missing item: {}", DpvpTweaks.MOD_NAME, location);
            return new ItemStack(Items.BARRIER);
        }
        return new ItemStack(direct);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class Root {
        int rerollCount = 1;
        List<TechEntry> techs = List.of();
        List<GunEntry> guns = List.of();
    }

    private static final class TechEntry {
        int id = -1;
        String name;
        String description;
        String iconItem;
    }

    private static final class GunEntry {
        int id = -1;
        String name;
        String item;
    }
}
