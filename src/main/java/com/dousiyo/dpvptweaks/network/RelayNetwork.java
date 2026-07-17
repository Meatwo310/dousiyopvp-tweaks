package com.dousiyo.dpvptweaks.network;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.network.session.SessionSeedPacket;
import com.dousiyo.dpvptweaks.network.session.SessionStatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class RelayNetwork {

    public static final String CHANNEL_NAMESPACE = "dousiyoanticheat";
    public static final String CHANNEL_NAME = "session_consistency";
    public static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(CHANNEL_NAMESPACE, CHANNEL_NAME),
            () -> PROTOCOL_VERSION,
            RelayNetwork::isCompatibleVersion,
            RelayNetwork::isCompatibleVersion
    );

    private static int packetId = 0;

    private static int nextId() {
        return packetId++;
    }

    private static boolean isCompatibleVersion(String remoteVersion) {
        return PROTOCOL_VERSION.equals(remoteVersion)
                || NetworkRegistry.ABSENT.equals(remoteVersion)
                || NetworkRegistry.ACCEPTVANILLA.equals(remoteVersion);
    }

    public static void register() {
        // S -> C: session seed
        CHANNEL.registerMessage(
                nextId(),
                SessionSeedPacket.class,
                SessionSeedPacket::encode,
                SessionSeedPacket::decode,
                SessionSeedPacket::handle
        );

        // C -> S: session state and heartbeat
        CHANNEL.registerMessage(
                nextId(),
                SessionStatePacket.class,
                SessionStatePacket::encode,
                SessionStatePacket::decode,
                SessionStatePacket::handle
        );

        DpvpTweaks.LOGGER.info("[{}] Relay network packets registered", DpvpTweaks.MOD_NAME);
    }
}


