package com.dousiyo.dpvptweaks.temporarybuilding;

import net.minecraft.server.level.ServerPlayer;

/** Mode-owned participant policy; future SECRET OPERATIONS modes use the same building manager. */
public interface TemporaryBuildingMatchBridge {
    boolean canBuild(ServerPlayer player);
}
