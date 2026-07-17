package com.dousiyo.dpvptweaks.secretoperations;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** UTF-8, operator-editable map locations for SECRET OPERATIONS. */
public final class SecretOperationsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Path PATH = FMLPaths.GAMEDIR.get().resolve("dousiyo").resolve("secret_operations.json");
    private static volatile Data current = new Data();
    private static volatile String loadError = "設定が読み込まれていません";

    private SecretOperationsConfig() {}

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
        Data data = current;
        String waitError = validatePoint(server, data.waitingSpawn, "waitingSpawn");
        if (waitError != null) return new Validation(null, null, waitError);
        if (data.secretShowdown == null || data.secretShowdown.airSpawn == null)
            return new Validation(null, null, "secretShowdown.airSpawnが未設定です");
        AirSpawn air = data.secretShowdown.airSpawn;
        ServerLevel airLevel = level(server, air.dimension);
        if (airLevel == null) return new Validation(null, null, "airSpawn.dimensionが不正です: " + air.dimension);
        if (!finite(air.minX, air.maxX, air.minZ, air.maxZ, air.y, air.yaw, air.pitch))
            return new Validation(null, null, "airSpawnに有限でない数値があります");
        if (air.minX >= air.maxX || air.minZ >= air.maxZ)
            return new Validation(null, null, "airSpawnのmin値はmax値より小さくしてください");
        if (air.y < airLevel.getMinBuildHeight() || air.y >= airLevel.getMaxBuildHeight())
            return new Validation(null, null, "airSpawn.yがワールド高度外です");
        return new Validation(data.waitingSpawn, air, null);
    }

    public static String error(MinecraftServer server) { return validate(server).error; }

    public static SupplyDrop supplyDrop() {
        Showdown showdown = current.secretShowdown;
        return showdown == null || showdown.supplyDrop == null ? new SupplyDrop() : showdown.supplyDrop;
    }

    public static ConvoyValidation validateConvoy(MinecraftServer server) {
        if (loadError != null) return new ConvoyValidation(null, null, loadError);
        String waitError = validatePoint(server, current.waitingSpawn, "waitingSpawn");
        if (waitError != null) return new ConvoyValidation(null, null, waitError);
        Convoy convoy = current.secretConvoy;
        if (convoy == null) return new ConvoyValidation(null, null, "secretConvoyが未設定です");
        String escortError = validatePoint(server, convoy.escortSpawn, "secretConvoy.escortSpawn");
        if (escortError != null) return new ConvoyValidation(null, null, escortError);
        String defenderError = validatePoint(server, convoy.defenderSpawn, "secretConvoy.defenderSpawn");
        if (defenderError != null) return new ConvoyValidation(null, null, defenderError);
        if (!convoy.escortSpawn.dimension.equals(convoy.defenderSpawn.dimension))
            return new ConvoyValidation(null, null, "CONVOYのスポーンは同じディメンションにしてください");
        ServerLevel level = level(server, convoy.escortSpawn.dimension);
        if (convoy.route == null || convoy.route.size() < 2)
            return new ConvoyValidation(null, null, "secretConvoy.routeには2地点以上必要です");
        double total = 0.0D;
        for (int i = 0; i < convoy.route.size(); i++) {
            RoutePoint p = convoy.route.get(i);
            if (p == null || !finite(p.x, p.y, p.z)) return new ConvoyValidation(null, null, "route[" + i + "]が不正です");
            if (p.y < level.getMinBuildHeight() || p.y >= level.getMaxBuildHeight())
                return new ConvoyValidation(null, null, "route[" + i + "].yがワールド高度外です");
            if (i > 0) total += p.vec().distanceTo(convoy.route.get(i - 1).vec());
        }
        if (total < 1.0D) return new ConvoyValidation(null, null, "CONVOY航路は1m以上必要です");
        if (!finite(convoy.escortRadius, convoy.verticalRange, convoy.speedPerEscort)
                || convoy.escortRadius <= 0 || convoy.verticalRange <= 0 || convoy.speedPerEscort <= 0
                || convoy.maxSpeedEscorts < 1 || convoy.overtimeGraceSeconds < 0)
            return new ConvoyValidation(null, null, "secretConvoyの移動設定が不正です");
        return new ConvoyValidation(convoy, level, null);
    }

    public static String convoyError(MinecraftServer server) { return validateConvoy(server).error; }

    private static String validatePoint(MinecraftServer server, SpawnPoint point, String name) {
        if (point == null) return name + "が未設定です";
        ServerLevel level = level(server, point.dimension);
        if (level == null) return name + ".dimensionが不正です: " + point.dimension;
        if (!finite(point.x, point.y, point.z, point.yaw, point.pitch)) return name + "に有限でない数値があります";
        if (point.y < level.getMinBuildHeight() || point.y >= level.getMaxBuildHeight()) return name + ".yがワールド高度外です";
        return null;
    }

    private static ServerLevel level(MinecraftServer server, String name) {
        ResourceLocation id = ResourceLocation.tryParse(name == null ? "" : name);
        if (id == null) return null;
        for (ServerLevel level : server.getAllLevels())
            if (level.dimension().location().equals(id)) return level;
        return null;
    }

    public static ServerLevel waitingLevel(MinecraftServer server, Validation validation) {
        return level(server, validation.waiting.dimension);
    }

    public static ServerLevel airLevel(MinecraftServer server, Validation validation) {
        return level(server, validation.air.dimension);
    }

    private static boolean finite(double... values) {
        for (double value : values) if (!Double.isFinite(value)) return false;
        return true;
    }

    private static void writeTemplate() throws IOException {
        Files.createDirectories(PATH.getParent());
        Files.writeString(PATH, """
                {
                  "waitingSpawn": {
                    "dimension": "minecraft:overworld",
                    "x": 0.5,
                    "y": 100.0,
                    "z": 0.5,
                    "yaw": 0.0,
                    "pitch": 0.0
                  },
                  "secretShowdown": {
                    "airSpawn": {
                      "dimension": "minecraft:overworld",
                      "minX": -500.0,
                      "maxX": 500.0,
                      "minZ": -500.0,
                      "maxZ": 500.0,
                      "y": 250.0,
                      "yaw": 0.0,
                      "pitch": 0.0
                    },
                    "supplyDrop": {
                      "enabled": true,
                      "dimension": "minecraft:overworld",
                      "minX": -500.0,
                      "maxX": 500.0,
                      "minZ": -500.0,
                      "maxZ": 500.0,
                      "dropHeight": 80,
                      "intervalSeconds": 120,
                      "openSeconds": 10,
                      "waitForClaimBeforeNextDrop": true,
                      "teamPoints": 20,
                      "personalPoints": 20,
                      "weapons": [
                        {
                          "gunId": "tacz:glock_17",
                          "weight": 1,
                          "fireMode": "SEMI",
                          "reserveMagazines": 3,
                          "attachments": {}
                        }
                      ]
                    }
                  },
                  "secretConvoy": {
                    "escortSpawn": null,
                    "defenderSpawn": null,
                    "route": [],
                    "escortRadius": 6.0,
                    "verticalRange": 3.0,
                    "speedPerEscort": 1.0,
                    "maxSpeedEscorts": 8,
                    "overtimeGraceSeconds": 3,
                    "buildingBoundsPadding": 32
                  }
                }
                """, StandardCharsets.UTF_8);
    }

    public static final class Data {
        public SpawnPoint waitingSpawn;
        public Showdown secretShowdown;
        public Convoy secretConvoy;
    }
    public static final class Showdown {
        public AirSpawn airSpawn;
        public SupplyDrop supplyDrop = new SupplyDrop();
    }
    public static final class SupplyDrop {
        public boolean enabled = true;
        /** Blank dimension and non-finite bounds inherit secretShowdown.airSpawn. */
        public String dimension = "";
        public double minX = Double.NaN;
        public double maxX = Double.NaN;
        public double minZ = Double.NaN;
        public double maxZ = Double.NaN;
        public int dropHeight = 80;
        public int intervalSeconds = 120;
        public int openSeconds = 10;
        public boolean waitForClaimBeforeNextDrop = true;
        public int teamPoints = 20;
        public int personalPoints = 20;
        public List<SupplyWeapon> weapons = List.of(defaultSupplyWeapon());

        private static SupplyWeapon defaultSupplyWeapon() {
            SupplyWeapon weapon = new SupplyWeapon();
            weapon.gunId = "tacz:glock_17";
            return weapon;
        }
    }
    public static final class SupplyWeapon {
        public String gunId;
        public int weight = 1;
        public String fireMode = "SEMI";
        public int reserveMagazines = 3;
        public Map<String, String> attachments = Map.of();
    }
    public static final class Convoy {
        public SpawnPoint escortSpawn;
        public SpawnPoint defenderSpawn;
        public List<RoutePoint> route = List.of();
        public double escortRadius = 6.0D;
        public double verticalRange = 3.0D;
        public double speedPerEscort = 1.0D;
        public int maxSpeedEscorts = 8;
        public int overtimeGraceSeconds = 3;
        public int buildingBoundsPadding = 32;
    }
    public static final class RoutePoint {
        public double x; public double y; public double z;
        public net.minecraft.world.phys.Vec3 vec() { return new net.minecraft.world.phys.Vec3(x, y, z); }
    }
    public static final class SpawnPoint {
        public String dimension; public double x; public double y; public double z; public float yaw; public float pitch;
    }
    public static final class AirSpawn {
        public String dimension; public double minX; public double maxX; public double minZ; public double maxZ;
        public double y; public float yaw; public float pitch;
    }
    public record Validation(SpawnPoint waiting, AirSpawn air, String error) { public boolean valid() { return error == null; } }
    public record ConvoyValidation(Convoy convoy, ServerLevel level, String error) { public boolean valid() { return error == null; } }
}
