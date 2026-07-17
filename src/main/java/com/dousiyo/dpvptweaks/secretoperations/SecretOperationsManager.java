package com.dousiyo.dpvptweaks.secretoperations;

import com.dousiyo.dpvptweaks.network.secretoperations.SecretOperationsNetwork;
import com.dousiyo.dpvptweaks.network.secretoperations.SecretOperationsStatePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.network.PacketDistributor;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Shared participation state. Manual activation coexists with automatic match activation. */
public final class SecretOperationsManager {
    private static final UUID HEALTH_MODIFIER_ID = UUID.fromString("6c5f7ddc-9289-4d27-84a7-fbe88a8dd6df");
    private static final double SECRET_OPERATIONS_MAX_HEALTH = 60.0D;
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
        boolean active = isActive(player);
        updateHealth(player, active);
        SecretOperationsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SecretOperationsStatePacket(active));
    }

    private static void updateHealth(ServerPlayer player, boolean active) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) return;
        AttributeModifier modifier = health.getModifier(HEALTH_MODIFIER_ID);
        if (!active) {
            if (modifier != null) health.removeModifier(modifier);
            if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
            return;
        }
        if (modifier != null && Math.abs(health.getValue() - SECRET_OPERATIONS_MAX_HEALTH) < 0.001D) return;
        boolean newlyApplied = modifier == null;
        if (modifier != null) health.removeModifier(modifier);
        double amount = SECRET_OPERATIONS_MAX_HEALTH - health.getValue();
        health.addTransientModifier(new AttributeModifier(HEALTH_MODIFIER_ID, "Secret Operations max health", amount,
                AttributeModifier.Operation.ADDITION));
        if (newlyApplied) player.setHealth(player.getMaxHealth());
        else if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
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
