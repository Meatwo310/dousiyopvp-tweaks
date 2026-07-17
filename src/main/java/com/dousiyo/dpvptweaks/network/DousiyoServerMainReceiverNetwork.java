package com.dousiyo.dpvptweaks.network;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.network.loadout.SelectLoadoutPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class DousiyoServerMainReceiverNetwork {
    private static final String CHANNEL_NAMESPACE = "dousiyoserver";
    private static final String CHANNEL_NAME = "main";
    private static final String PROTOCOL_VERSION = "1";

    private static boolean registered;
    private static int packetId;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(CHANNEL_NAMESPACE, CHANNEL_NAME),
            () -> PROTOCOL_VERSION,
            DousiyoServerMainReceiverNetwork::isCompatibleVersion,
            DousiyoServerMainReceiverNetwork::isCompatibleVersion
    );

    private DousiyoServerMainReceiverNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        CHANNEL.messageBuilder(EmptyMainPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EmptyMainPacket::encode)
                .decoder(EmptyMainPacket::decode)
                .consumerMainThread(EmptyMainPacket::handle)
                .add();

        CHANNEL.messageBuilder(EmptyMainPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EmptyMainPacket::encode)
                .decoder(EmptyMainPacket::decode)
                .consumerMainThread(EmptyMainPacket::handle)
                .add();

        CHANNEL.messageBuilder(SelectLoadoutPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectLoadoutPacket::encode)
                .decoder(SelectLoadoutPacket::decode)
                .consumerMainThread(SelectLoadoutPacket::handle)
                .add();

        DpvpTweaks.LOGGER.info("[{}] Registered fallback receiver for {}:{}", DpvpTweaks.MOD_NAME, CHANNEL_NAMESPACE, CHANNEL_NAME);
    }

    private static boolean isCompatibleVersion(String remoteVersion) {
        return PROTOCOL_VERSION.equals(remoteVersion)
                || NetworkRegistry.ABSENT.equals(remoteVersion)
                || NetworkRegistry.ACCEPTVANILLA.equals(remoteVersion);
    }

    private static final class EmptyMainPacket {
        private static void encode(EmptyMainPacket msg, FriendlyByteBuf buf) {
        }

        private static EmptyMainPacket decode(FriendlyByteBuf buf) {
            return new EmptyMainPacket();
        }

        private static void handle(EmptyMainPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().setPacketHandled(true);
        }
    }
}
