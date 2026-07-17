package com.dousiyo.dpvptweaks.secretoperations;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.config.ServerConfig;
import com.dousiyo.dpvptweaks.network.secretoperations.DamageFeedbackStatePacket;
import com.dousiyo.dpvptweaks.network.secretoperations.SecretOperationsNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/** Server-authoritative global state for the damage feedback HUD. */
@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DamageFeedbackManager {
    private static volatile boolean matchEnabled;

    private DamageFeedbackManager() {}

    public static boolean isEnabled() {
        return ServerConfig.DAMAGE_FEEDBACK_ENABLED.get() || matchEnabled;
    }

    public static boolean setEnabled(MinecraftServer server, boolean enabled) {
        boolean before = isEnabled();
        ServerConfig.DAMAGE_FEEDBACK_ENABLED.set(enabled);
        syncAll(server);
        return before != isEnabled();
    }

    public static boolean toggle(MinecraftServer server) {
        boolean enabled = !ServerConfig.DAMAGE_FEEDBACK_ENABLED.get();
        setEnabled(server, enabled);
        return isEnabled();
    }

    public static void syncAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) sync(player);
    }

    public static void sync(ServerPlayer player) {
        SecretOperationsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new DamageFeedbackStatePacket(isEnabled()));
    }

    @SubscribeEvent
    public static void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        boolean enabled = com.dousiyo.dpvptweaks.arsenal.ArsenalMatchManager.activeMatch(event.getServer())
                || SecretShowdownManager.activeMatch() || SecretConvoyManager.activeMatch();
        if (matchEnabled == enabled) return;
        matchEnabled = enabled;
        syncAll(event.getServer());
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        matchEnabled = false;
    }

}
