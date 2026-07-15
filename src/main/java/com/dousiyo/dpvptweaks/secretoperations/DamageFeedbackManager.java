package com.dousiyo.dpvptweaks.secretoperations;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.network.DamageFeedbackStatePacket;
import com.dousiyo.dpvptweaks.network.SecretOperationsNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-authoritative opt-in state for the damage feedback HUD. */
@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DamageFeedbackManager {
    private static final Set<UUID> ENABLED_PLAYERS = ConcurrentHashMap.newKeySet();

    private DamageFeedbackManager() {}

    public static boolean isEnabled(ServerPlayer player) {
        return ENABLED_PLAYERS.contains(player.getUUID());
    }

    public static boolean enable(ServerPlayer player) {
        boolean changed = ENABLED_PLAYERS.add(player.getUUID());
        sync(player);
        return changed;
    }

    public static boolean disable(ServerPlayer player) {
        boolean changed = ENABLED_PLAYERS.remove(player.getUUID());
        sync(player);
        return changed;
    }

    public static boolean toggle(ServerPlayer player) {
        boolean enabled;
        if (ENABLED_PLAYERS.remove(player.getUUID())) {
            enabled = false;
        } else {
            ENABLED_PLAYERS.add(player.getUUID());
            enabled = true;
        }
        sync(player);
        return enabled;
    }

    public static void disableAll() {
        ENABLED_PLAYERS.clear();
    }

    public static void sync(ServerPlayer player) {
        SecretOperationsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new DamageFeedbackStatePacket(isEnabled(player)));
    }

    @SubscribeEvent
    public static void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        disableAll();
    }
}
