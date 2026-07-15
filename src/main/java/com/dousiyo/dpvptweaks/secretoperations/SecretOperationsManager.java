package com.dousiyo.dpvptweaks.secretoperations;

import com.dousiyo.dpvptweaks.network.SecretOperationsNetwork;
import com.dousiyo.dpvptweaks.network.SecretOperationsStatePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Shared participation state. Manual activation coexists with automatic match activation. */
public final class SecretOperationsManager {
    private static final Set<UUID> MANUALLY_ACTIVE = ConcurrentHashMap.newKeySet();

    private SecretOperationsManager() {}

    public static boolean isActive(ServerPlayer player) {
        return MANUALLY_ACTIVE.contains(player.getUUID()) || SecretShowdownManager.isParticipant(player)
                || SecretConvoyManager.isParticipant(player);
    }

    public static boolean enableManually(ServerPlayer player) {
        boolean changed = MANUALLY_ACTIVE.add(player.getUUID());
        sync(player);
        return changed;
    }

    public static boolean disableManually(ServerPlayer player) {
        boolean changed = MANUALLY_ACTIVE.remove(player.getUUID());
        sync(player);
        return changed;
    }

    public static void disableAllManual(MinecraftServer server) {
        MANUALLY_ACTIVE.clear();
        server.getPlayerList().getPlayers().forEach(SecretOperationsManager::sync);
    }

    public static void clearAll() {
        MANUALLY_ACTIVE.clear();
    }

    public static void sync(ServerPlayer player) {
        SecretOperationsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SecretOperationsStatePacket(isActive(player)));
    }

    public static Component status(ServerPlayer player) {
        boolean manual = MANUALLY_ACTIVE.contains(player.getUUID());
        boolean showdown = SecretShowdownManager.isParticipant(player);
        boolean convoy = SecretConvoyManager.isParticipant(player);
        return Component.literal("SECRET OPERATIONS: " + (manual || showdown || convoy ? "active" : "inactive")
                + " / command=" + (manual ? "on" : "off")
                + " / showdown=" + (showdown ? "on" : "off")
                + " / convoy=" + (convoy ? "on" : "off"));
    }
}
