package com.dousiyo.dpvptweaks.inteldraft;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

/** Stable integration facade for datapack-adjacent mods. Calls must run on the server thread. */
public final class IntelDraftService {
    private IntelDraftService() {}
    public static boolean openDraft(ServerPlayer player) { return IntelDraftManager.open(player); }
    public static void clearState(ServerPlayer player) { IntelDraftManager.end(player); }
    public static void endAll(net.minecraft.server.MinecraftServer server) { IntelDraftManager.endAll(server); }
    public static boolean hasPlayerState(ServerPlayer player) { return IntelDraftManager.hasState(player); }
    public static Set<ResourceLocation> acquiredTechs(ServerPlayer player) { return IntelDraftManager.acquiredIds(player); }
    public static IntelDraftDefinition.Pool definitions() { return IntelDraftDefinitionLoader.get(); }
}
