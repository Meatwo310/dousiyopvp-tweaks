package com.dousiyo.dpvptweaks.network;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.network.functionpalette.c2s.RequestFunctionListPacket;
import com.dousiyo.dpvptweaks.network.functionpalette.c2s.RunFunctionPacket;
import com.dousiyo.dpvptweaks.network.functionpalette.s2c.FunctionListPacket;
import com.dousiyo.dpvptweaks.network.functionpalette.s2c.FunctionResultPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class FunctionPaletteNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static boolean registered;
    private static int packetId;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(DpvpTweaks.MODID, "function_palette"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private FunctionPaletteNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        CHANNEL.messageBuilder(RequestFunctionListPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestFunctionListPacket::encode)
                .decoder(RequestFunctionListPacket::decode)
                .consumerMainThread(RequestFunctionListPacket::handle)
                .add();

        CHANNEL.messageBuilder(RunFunctionPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RunFunctionPacket::encode)
                .decoder(RunFunctionPacket::decode)
                .consumerMainThread(RunFunctionPacket::handle)
                .add();

        CHANNEL.messageBuilder(FunctionListPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FunctionListPacket::encode)
                .decoder(FunctionListPacket::decode)
                .consumerMainThread(FunctionListPacket::handle)
                .add();

        CHANNEL.messageBuilder(FunctionResultPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FunctionResultPacket::encode).decoder(FunctionResultPacket::decode)
                .consumerMainThread(FunctionResultPacket::handle).add();

        DpvpTweaks.LOGGER.info("[{}] Function palette network packets registered", DpvpTweaks.MOD_NAME);
    }
}
