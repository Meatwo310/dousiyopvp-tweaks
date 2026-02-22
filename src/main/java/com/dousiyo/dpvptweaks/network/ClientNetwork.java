package com.dousiyo.dpvptweaks.network;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ClientNetwork {

    public static final String CHANNEL_NAMESPACE = "dousiyopvp";
    public static final String CHANNEL_NAME = "main";
    public static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(CHANNEL_NAMESPACE, CHANNEL_NAME),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private static int nextId() {
        return packetId++;
    }

    public static void register() {
        // S -> C: open loadout GUI
        CHANNEL.registerMessage(
                nextId(),
                OpenLoadoutGuiPacket.class,
                OpenLoadoutGuiPacket::encode,
                OpenLoadoutGuiPacket::decode,
                OpenLoadoutGuiPacket::handle
        );

        // C -> S: select loadout
        CHANNEL.registerMessage(
                nextId(),
                SelectLoadoutPacket.class,
                SelectLoadoutPacket::encode,
                SelectLoadoutPacket::decode,
                SelectLoadoutPacket::handle
        );

        // S -> C: session sync seed
        CHANNEL.registerMessage(
                nextId(),
                SessionSyncSeedPacket.class,
                SessionSyncSeedPacket::encode,
                SessionSyncSeedPacket::decode,
                SessionSyncSeedPacket::handle
        );

        // C -> S: session sync state and heartbeat
        CHANNEL.registerMessage(
                nextId(),
                SessionSyncStatePacket.class,
                SessionSyncStatePacket::encode,
                SessionSyncStatePacket::decode,
                SessionSyncStatePacket::handle
        );

        DpvpTweaks.LOGGER.info("[{}] Client network packets registered", DpvpTweaks.MOD_NAME);
    }
}
