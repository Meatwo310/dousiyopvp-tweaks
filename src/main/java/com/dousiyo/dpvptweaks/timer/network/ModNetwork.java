package com.dousiyo.dpvptweaks.timer.network;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private ModNetwork() {}

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(DpvpTweaks.MODID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static int id = 0;

    public static void register() {
        CHANNEL.messageBuilder(TimerHudUpdateS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TimerHudUpdateS2CPacket::encode)
                .decoder(TimerHudUpdateS2CPacket::decode)
                .consumerMainThread(TimerHudUpdateS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(CountdownHudS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CountdownHudS2CPacket::encode)
                .decoder(CountdownHudS2CPacket::decode)
                .consumerMainThread(CountdownHudS2CPacket::handle)
                .add();
    }
}
