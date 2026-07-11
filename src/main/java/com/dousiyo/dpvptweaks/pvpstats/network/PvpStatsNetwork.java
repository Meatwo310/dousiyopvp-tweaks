package com.dousiyo.dpvptweaks.pvpstats.network;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.pvpstats.network.c2s.RequestOwnStatsPacket;
import com.dousiyo.dpvptweaks.pvpstats.network.c2s.UpdatePrivacySettingsPacket;
import com.dousiyo.dpvptweaks.pvpstats.network.s2c.OpenStatsGuiPacket;
import com.dousiyo.dpvptweaks.pvpstats.network.s2c.StatsErrorPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class PvpStatsNetwork {
    private static final String PROTOCOL_VERSION = "5";
    private static boolean registered;
    private static int packetId;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(DpvpTweaks.MODID, "pvp_stats"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private PvpStatsNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        CHANNEL.messageBuilder(RequestOwnStatsPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestOwnStatsPacket::encode)
                .decoder(RequestOwnStatsPacket::decode)
                .consumerMainThread(RequestOwnStatsPacket::handle)
                .add();

        CHANNEL.messageBuilder(UpdatePrivacySettingsPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UpdatePrivacySettingsPacket::encode)
                .decoder(UpdatePrivacySettingsPacket::decode)
                .consumerMainThread(UpdatePrivacySettingsPacket::handle)
                .add();

        CHANNEL.messageBuilder(OpenStatsGuiPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenStatsGuiPacket::encode)
                .decoder(OpenStatsGuiPacket::decode)
                .consumerMainThread(OpenStatsGuiPacket::handle)
                .add();

        CHANNEL.messageBuilder(StatsErrorPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StatsErrorPacket::encode)
                .decoder(StatsErrorPacket::decode)
                .consumerMainThread(StatsErrorPacket::handle)
                .add();

        DpvpTweaks.LOGGER.info("[{}] PvP stats network packets registered", DpvpTweaks.MOD_NAME);
    }
}
