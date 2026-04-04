package com.dousiyo.dpvptweaks.network;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class LoadoutGuiNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static boolean registered;
    private static int packetId;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(DpvpTweaks.MODID, "loadout_gui"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private LoadoutGuiNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        CHANNEL.messageBuilder(OpenLoadoutGuiPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenLoadoutGuiPacket::encode)
                .decoder(OpenLoadoutGuiPacket::decode)
                .consumerMainThread(OpenLoadoutGuiPacket::handle)
                .add();

        CHANNEL.messageBuilder(OpenMiniLoadoutGuiPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenMiniLoadoutGuiPacket::encode)
                .decoder(OpenMiniLoadoutGuiPacket::decode)
                .consumerMainThread(OpenMiniLoadoutGuiPacket::handle)
                .add();

        CHANNEL.messageBuilder(CloseLoadoutGuiPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CloseLoadoutGuiPacket::encode)
                .decoder(CloseLoadoutGuiPacket::decode)
                .consumerMainThread(CloseLoadoutGuiPacket::handle)
                .add();

        DpvpTweaks.LOGGER.info("[{}] Loadout GUI network packets registered", DpvpTweaks.MOD_NAME);
    }
}