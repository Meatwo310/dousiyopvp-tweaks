package com.dousiyo.dpvptweaks.network;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class SecretOperationsNetwork {
    private static final String PROTOCOL_VERSION = "4";
    private static boolean registered;
    private static int packetId;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(DpvpTweaks.MODID, "secret_operations"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private SecretOperationsNetwork() {}

    public static void register() {
        if (registered) return;
        registered = true;

        CHANNEL.messageBuilder(DamageFeedbackPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DamageFeedbackPacket::encode)
                .decoder(DamageFeedbackPacket::decode)
                .consumerMainThread(DamageFeedbackPacket::handle)
                .add();

        CHANNEL.messageBuilder(SecretOperationsStatePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SecretOperationsStatePacket::encode)
                .decoder(SecretOperationsStatePacket::decode)
                .consumerMainThread(SecretOperationsStatePacket::handle)
                .add();

        CHANNEL.messageBuilder(DamageFeedbackStatePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DamageFeedbackStatePacket::encode)
                .decoder(DamageFeedbackStatePacket::decode)
                .consumerMainThread(DamageFeedbackStatePacket::handle)
                .add();

        CHANNEL.messageBuilder(OpenSecretOperationsAdminPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenSecretOperationsAdminPacket::encode).decoder(OpenSecretOperationsAdminPacket::decode)
                .consumerMainThread(OpenSecretOperationsAdminPacket::handle).add();
        CHANNEL.messageBuilder(SecretOperationsAdminActionPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SecretOperationsAdminActionPacket::encode).decoder(SecretOperationsAdminActionPacket::decode)
                .consumerMainThread(SecretOperationsAdminActionPacket::handle).add();
        CHANNEL.messageBuilder(SecretOperationsMatchStatePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SecretOperationsMatchStatePacket::encode).decoder(SecretOperationsMatchStatePacket::decode)
                .consumerMainThread(SecretOperationsMatchStatePacket::handle).add();
        CHANNEL.messageBuilder(SecretConvoyHudStatePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SecretConvoyHudStatePacket::encode).decoder(SecretConvoyHudStatePacket::decode)
                .consumerMainThread(SecretConvoyHudStatePacket::handle).add();
        CHANNEL.messageBuilder(OpenPendingSecretDraftPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(OpenPendingSecretDraftPacket::encode).decoder(OpenPendingSecretDraftPacket::decode)
                .consumerMainThread(OpenPendingSecretDraftPacket::handle).add();

        DpvpTweaks.LOGGER.info("[{}] SECRET OPERATIONS network packets registered", DpvpTweaks.MOD_NAME);
    }
}
