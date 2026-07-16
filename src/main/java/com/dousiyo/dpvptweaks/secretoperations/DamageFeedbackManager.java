package com.dousiyo.dpvptweaks.secretoperations;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.config.ServerConfig;
import com.dousiyo.dpvptweaks.network.DamageFeedbackStatePacket;
import com.dousiyo.dpvptweaks.network.SecretOperationsNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/** Server-authoritative global state for the damage feedback HUD. */
@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DamageFeedbackManager {
    private DamageFeedbackManager() {}

    public static boolean isEnabled() {
        return ServerConfig.DAMAGE_FEEDBACK_ENABLED.get();
    }

    public static boolean setEnabled(MinecraftServer server, boolean enabled) {
        boolean changed = isEnabled() != enabled;
        ServerConfig.DAMAGE_FEEDBACK_ENABLED.set(enabled);
        syncAll(server);
        return changed;
    }

    public static boolean toggle(MinecraftServer server) {
        boolean enabled = !isEnabled();
        setEnabled(server, enabled);
        return enabled;
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

}
