package com.dousiyo.dpvptweaks.network;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ClientNetwork {

    public static final String CHANNEL_NAMESPACE = "dousiyoserver";
    public static final String CHANNEL_NAME = "main";
    public static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(CHANNEL_NAMESPACE, CHANNEL_NAME),
            () -> PROTOCOL_VERSION,
            ClientNetwork::isCompatibleVersion,
            ClientNetwork::isCompatibleVersion
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
        CHANNEL.registerMessage(
                nextId(),
                OpenLoadoutGuiPacket.class,
                OpenLoadoutGuiPacket::encode,
                OpenLoadoutGuiPacket::decode,
                OpenLoadoutGuiPacket::handle
        );

        CHANNEL.registerMessage(
                nextId(),
                OpenMiniLoadoutGuiPacket.class,
                OpenMiniLoadoutGuiPacket::encode,
                OpenMiniLoadoutGuiPacket::decode,
                OpenMiniLoadoutGuiPacket::handle
        );

        CHANNEL.registerMessage(
                nextId(),
                SelectLoadoutPacket.class,
                SelectLoadoutPacket::encode,
                SelectLoadoutPacket::decode,
                SelectLoadoutPacket::handle
        );

        CHANNEL.registerMessage(
                nextId(),
                OpenIntelDraftGuiPacket.class,
                OpenIntelDraftGuiPacket::encode,
                OpenIntelDraftGuiPacket::decode,
                OpenIntelDraftGuiPacket::handle
        );

        CHANNEL.registerMessage(
                nextId(),
                RerollIntelDraftPacket.class,
                RerollIntelDraftPacket::encode,
                RerollIntelDraftPacket::decode,
                RerollIntelDraftPacket::handle
        );

        CHANNEL.registerMessage(
                nextId(),
                SelectIntelDraftPacket.class,
                SelectIntelDraftPacket::encode,
                SelectIntelDraftPacket::decode,
                SelectIntelDraftPacket::handle
        );

        DpvpTweaks.LOGGER.info("[{}] Gameplay network packets registered", DpvpTweaks.MOD_NAME);
    }
}
