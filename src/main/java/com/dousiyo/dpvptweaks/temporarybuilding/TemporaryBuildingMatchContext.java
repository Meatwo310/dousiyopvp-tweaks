package com.dousiyo.dpvptweaks.temporarybuilding;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** Common arena description reusable by every SECRET OPERATIONS game mode. */
public record TemporaryBuildingMatchContext(UUID matchId, String modeId, ResourceKey<Level> dimension,
                                            int minX, int maxX, int minZ, int maxZ) {
    public TemporaryBuildingMatchContext {
        if (matchId == null || modeId == null || dimension == null) throw new IllegalArgumentException("Missing match context");
        if (minX >= maxX || minZ >= maxZ) throw new IllegalArgumentException("Invalid building bounds");
    }

    public boolean contains(ResourceKey<Level> level, int x, int z) {
        return dimension.equals(level) && x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }
}
