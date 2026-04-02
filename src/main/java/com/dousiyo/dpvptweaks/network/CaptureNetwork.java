package com.dousiyo.dpvptweaks.network;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class CaptureNetwork {
    private CaptureNetwork() {}

    private static final String PROTOCOL_VERSION = "4";
    private static boolean registered;
    private static int packetId;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(DpvpTweaks.MODID, "capture"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        CHANNEL.messageBuilder(CapturePointEventS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CapturePointEventS2CPacket::encode)
                .decoder(CapturePointEventS2CPacket::decode)
                .consumerMainThread(CapturePointEventS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(PlayerPointFocusS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PlayerPointFocusS2CPacket::encode)
                .decoder(PlayerPointFocusS2CPacket::decode)
                .consumerMainThread(PlayerPointFocusS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(PlayerPointHudStateS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PlayerPointHudStateS2CPacket::encode)
                .decoder(PlayerPointHudStateS2CPacket::decode)
                .consumerMainThread(PlayerPointHudStateS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(CaptureFeatureStateS2CPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CaptureFeatureStateS2CPacket::encode)
                .decoder(CaptureFeatureStateS2CPacket::decode)
                .consumerMainThread(CaptureFeatureStateS2CPacket::handle)
                .add();
    }
}