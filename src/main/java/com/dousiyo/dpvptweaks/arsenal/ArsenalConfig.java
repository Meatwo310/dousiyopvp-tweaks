package com.dousiyo.dpvptweaks.arsenal;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ArsenalConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final Path PATH = FMLPaths.GAMEDIR.get().resolve("dousiyo").resolve("arsenal.json");
    private static volatile Data current = new Data();
    private static volatile String loadError = "設定が読み込まれていません";

    private ArsenalConfig() {}

    public static synchronized void reload() {
        try {
            if (Files.notExists(PATH)) writeTemplate();
            try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
                Data parsed = GSON.fromJson(reader, Data.class);
                current = parsed == null ? new Data() : parsed;
                loadError = null;
            }
        } catch (Exception exception) {
            current = new Data();
            loadError = "設定読込エラー: " + exception.getMessage();
            DpvpTweaks.LOGGER.error("Failed to load {}", PATH, exception);
        }
    }

    public static Validation validate(MinecraftServer server) {
        if (loadError != null) return new Validation(null, null, loadError);
        AirSpawn spawn = current.airSpawn;
        if (spawn == null) return new Validation(null, null, "airSpawnが未設定です");
        ResourceLocation dimension = ResourceLocation.tryParse(spawn.dimension == null ? "" : spawn.dimension);
        if (dimension == null) return new Validation(null, null, "airSpawn.dimensionが不正です");
        ServerLevel level = null;
        for (ServerLevel candidate : server.getAllLevels()) {
            if (candidate.dimension().location().equals(dimension)) { level = candidate; break; }
        }
        if (level == null) return new Validation(null, null, "airSpawn.dimensionが存在しません: " + dimension);
        if (!finite(spawn.minX, spawn.maxX, spawn.minZ, spawn.maxZ, spawn.y, spawn.yaw, spawn.pitch))
            return new Validation(null, null, "airSpawnに有限でない値があります");
        if (spawn.minX >= spawn.maxX || spawn.minZ >= spawn.maxZ)
            return new Validation(null, null, "airSpawnのmin値はmax値より小さくしてください");
        if (spawn.y < level.getMinBuildHeight() || spawn.y >= level.getMaxBuildHeight())
            return new Validation(null, null, "airSpawn.yがワールド高度外です");
        return new Validation(spawn, level, null);
    }

    private static boolean finite(double... values) {
        for (double value : values) if (!Double.isFinite(value)) return false;
        return true;
    }

    private static void writeTemplate() throws Exception {
        Files.createDirectories(PATH.getParent());
        Files.writeString(PATH, """
                {
                  "airSpawn": {
                    "dimension": "minecraft:overworld",
                    "minX": -100.0,
                    "maxX": 100.0,
                    "minZ": -100.0,
                    "maxZ": 100.0,
                    "y": 200.0,
                    "yaw": 0.0,
                    "pitch": 0.0
                  }
                }
                """, StandardCharsets.UTF_8);
    }

    public static final class Data { public AirSpawn airSpawn; }
    public static final class AirSpawn {
        public String dimension; public double minX; public double maxX; public double minZ; public double maxZ;
        public double y; public float yaw; public float pitch;
    }
    public record Validation(AirSpawn spawn, ServerLevel level, String error) {
        public boolean valid() { return error == null; }
    }
}
