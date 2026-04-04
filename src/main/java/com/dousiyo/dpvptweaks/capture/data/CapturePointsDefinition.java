package com.dousiyo.dpvptweaks.capture.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class CapturePointsDefinition {
    private final Map<Integer, PointDefinition> pointsBySlot;

    private CapturePointsDefinition(Map<Integer, PointDefinition> pointsBySlot) {
        this.pointsBySlot = pointsBySlot;
    }

    public static CapturePointsDefinition empty() {
        return new CapturePointsDefinition(new LinkedHashMap<>());
    }

    public static CapturePointsDefinition load(Path path) throws IOException {
        if (!Files.exists(path)) {
            return empty();
        }

        String json = Files.readString(path, StandardCharsets.UTF_8);
        JsonElement rootElement = JsonParser.parseString(json);
        if (!rootElement.isJsonObject()) {
            return empty();
        }

        JsonObject root = rootElement.getAsJsonObject();
        JsonArray points = root.getAsJsonArray("points");
        if (points == null) {
            return empty();
        }

        Map<Integer, PointDefinition> pointMap = new LinkedHashMap<>();
        for (JsonElement pointElement : points) {
            if (!pointElement.isJsonObject()) {
                continue;
            }
            JsonObject point = pointElement.getAsJsonObject();
            if (!point.has("slot") || !point.has("aabb")) {
                continue;
            }

            int slot = point.get("slot").getAsInt();
            if (slot < 0 || slot > 4 || pointMap.containsKey(slot)) {
                continue;
            }

            String id = point.has("id") ? point.get("id").getAsString() : ("slot_" + slot);
            String dimension = point.has("dimension")
                    ? point.get("dimension").getAsString()
                    : Level.OVERWORLD.location().toString();
            JsonObject aabbObj = point.getAsJsonObject("aabb");
            if (aabbObj == null) {
                continue;
            }

            int x1 = aabbObj.get("x1").getAsInt();
            int y1 = aabbObj.get("y1").getAsInt();
            int z1 = aabbObj.get("z1").getAsInt();
            int x2 = aabbObj.get("x2").getAsInt();
            int y2 = aabbObj.get("y2").getAsInt();
            int z2 = aabbObj.get("z2").getAsInt();

            pointMap.put(slot, new PointDefinition(slot, id, dimension, x1, y1, z1, x2, y2, z2).normalized());
        }

        return new CapturePointsDefinition(pointMap);
    }

    public CapturePointsDefinition withPoint(PointDefinition point) {
        Map<Integer, PointDefinition> copy = new LinkedHashMap<>(pointsBySlot);
        copy.put(point.slot(), point);
        return new CapturePointsDefinition(copy);
    }

    public CapturePointsDefinition withoutSlot(int slot) {
        Map<Integer, PointDefinition> copy = new LinkedHashMap<>(pointsBySlot);
        copy.remove(slot);
        return new CapturePointsDefinition(copy);
    }

    public Collection<PointDefinition> points() {
        return Collections.unmodifiableCollection(pointsBySlot.values());
    }

    public Optional<PointDefinition> get(int slot) {
        return Optional.ofNullable(pointsBySlot.get(slot));
    }

    public int size() {
        return pointsBySlot.size();
    }

    public record PointDefinition(int slot, String id, String dimension, int x1, int y1, int z1, int x2, int y2, int z2) {
        public PointDefinition {
            if (slot < 0 || slot > 4) {
                throw new IllegalArgumentException("slot must be between 0 and 4");
            }
        }

        public ResourceKey<Level> dimensionKey() {
            ResourceLocation location = ResourceLocation.tryParse(dimension);
            if (location == null) {
                return Level.OVERWORLD;
            }
            return ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, location);
        }

        public AABB toAabb() {
            int minX = Math.min(x1, x2);
            int minY = Math.min(y1, y2);
            int minZ = Math.min(z1, z2);
            int maxX = Math.max(x1, x2);
            int maxY = Math.max(y1, y2);
            int maxZ = Math.max(z1, z2);
            return new AABB(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
        }

        public PointDefinition normalized() {
            int minX = Math.min(x1, x2);
            int minY = Math.min(y1, y2);
            int minZ = Math.min(z1, z2);
            int maxX = Math.max(x1, x2);
            int maxY = Math.max(y1, y2);
            int maxZ = Math.max(z1, z2);
            return new PointDefinition(slot, id, dimension, minX, minY, minZ, maxX, maxY, maxZ);
        }
    }
}