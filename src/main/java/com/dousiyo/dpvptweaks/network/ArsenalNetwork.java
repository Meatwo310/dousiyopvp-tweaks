package com.dousiyo.dpvptweaks.network;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.arsenal.ArsenalMatchState;
import com.dousiyo.dpvptweaks.arsenal.ArsenalPlayerData;
import com.dousiyo.dpvptweaks.arsenal.ArsenalSavedData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ArsenalNetwork {
    private static final String VERSION = "2";
    private static boolean registered;
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(DpvpTweaks.MODID, "arsenal"), () -> VERSION, VERSION::equals, VERSION::equals);

    private ArsenalNetwork() {}

    public static void register() {
        if (registered) return;
        registered = true;
        CHANNEL.messageBuilder(ArsenalStatePacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ArsenalStatePacket::encode).decoder(ArsenalStatePacket::decode)
                .consumerMainThread(ArsenalStatePacket::handle).add();
        CHANNEL.messageBuilder(OpenArsenalAdminPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenArsenalAdminPacket::encode).decoder(OpenArsenalAdminPacket::decode)
                .consumerMainThread(OpenArsenalAdminPacket::handle).add();
        CHANNEL.messageBuilder(ArsenalAdminActionPacket.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ArsenalAdminActionPacket::encode).decoder(ArsenalAdminActionPacket::decode)
                .consumerMainThread(ArsenalAdminActionPacket::handle).add();
    }

    public static void sync(ServerPlayer player, ArsenalSavedData match, ArsenalPlayerData data) {
        boolean protectedState = match.state == ArsenalMatchState.RUNNING
                && data.protectedAt(player.server.overworld().getGameTime());
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ArsenalStatePacket(true, protectedState,
                match.state == ArsenalMatchState.FINISHED, data.stage + 1, data.kills, data.deaths));
    }

    public static void syncInactive(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ArsenalStatePacket(false, false, false, 0, 0, 0));
    }
}
