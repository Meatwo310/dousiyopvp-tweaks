package com.dousiyo.dpvptweaks.loadout;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class LoadoutSetReloadListener extends SimpleJsonResourceReloadListener {
    private static final Path DIRECTORY = FMLPaths.GAMEDIR.get().resolve("dousiyo").resolve("loadout_sets");
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    public LoadoutSetReloadListener() {
        super(GSON, DpvpTweaks.MODID + "/loadout_sets");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        ensureDefault("tb.json");
        ensureDefault("tb_mini.json");

        Map<ResourceLocation, LoadoutSetDefinition> loaded = new LinkedHashMap<>();
        if (Files.isDirectory(DIRECTORY)) {
            try (Stream<Path> paths = Files.list(DIRECTORY)) {
                for (Path path : paths.filter(Files::isRegularFile)
                        .filter(file -> file.getFileName().toString().endsWith(".json"))
                        .sorted(Comparator.comparing(file -> file.getFileName().toString()))
                        .toList()) {
                    load(path, loaded);
                }
            } catch (IOException exception) {
                DpvpTweaks.LOGGER.error("[{}] Failed to list loadout set configs in {}", DpvpTweaks.MOD_NAME, DIRECTORY, exception);
            }
        }
        LoadoutDataManager.replaceSets(loaded);
    }

    private static void load(Path path, Map<ResourceLocation, LoadoutSetDefinition> loaded) {
        String fileName = path.getFileName().toString();
        String pathId = fileName.substring(0, fileName.length() - ".json".length());
        ResourceLocation id = ResourceLocation.tryParse(DpvpTweaks.MODID + ":" + pathId);
        if (id == null) {
            DpvpTweaks.LOGGER.warn("[{}] Ignoring loadout set config with invalid file name: {}", DpvpTweaks.MOD_NAME, path);
            return;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            LoadoutSetDefinition definition = LoadoutSetDefinitionLoader.fromDataPack(
                    id, JsonParser.parseReader(reader), "dousiyo loadout set config");
            if (definition != null) loaded.put(id, definition);
        } catch (Exception exception) {
            DpvpTweaks.LOGGER.error("[{}] Failed to read loadout set config from {}", DpvpTweaks.MOD_NAME, path, exception);
        }
    }

    private static void ensureDefault(String fileName) {
        Path target = DIRECTORY.resolve(fileName);
        if (Files.exists(target)) return;

        String resourcePath = "/assets/" + DpvpTweaks.MODID + "/defaults/loadout_sets/" + fileName;
        try {
            Files.createDirectories(DIRECTORY);
            try (InputStream input = LoadoutSetReloadListener.class.getResourceAsStream(resourcePath)) {
                if (input != null) Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            DpvpTweaks.LOGGER.error("[{}] Failed to create default loadout set config at {}", DpvpTweaks.MOD_NAME, target, exception);
        }
    }
}
