package com.dousiyo.dpvptweaks.network.loadout;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.network.inteldraft.ActivateIntelTechPacket;
import com.dousiyo.dpvptweaks.network.inteldraft.CloseIntelDraftGuiPacket;
import com.dousiyo.dpvptweaks.network.inteldraft.IntelDraftStatePacket;
import com.dousiyo.dpvptweaks.network.inteldraft.OpenIntelDraftGuiPacket;
import com.dousiyo.dpvptweaks.network.inteldraft.RerollIntelDraftPacket;
import com.dousiyo.dpvptweaks.network.inteldraft.SelectIntelDraftPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class LoadoutGuiNetwork {
    private static final String PROTOCOL_VERSION = "4";
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

        CHANNEL.messageBuilder(OpenIntelDraftGuiPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenIntelDraftGuiPacket::encode)
                .decoder(OpenIntelDraftGuiPacket::decode)
                .consumerMainThread(OpenIntelDraftGuiPacket::handle)
                .add();

        CHANNEL.messageBuilder(CloseIntelDraftGuiPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CloseIntelDraftGuiPacket::encode)
                .decoder(CloseIntelDraftGuiPacket::decode)
                .consumerMainThread(CloseIntelDraftGuiPacket::handle)
                .add();

        CHANNEL.messageBuilder(RerollIntelDraftPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RerollIntelDraftPacket::encode)
                .decoder(RerollIntelDraftPacket::decode)
                .consumerMainThread(RerollIntelDraftPacket::handle)
                .add();

        CHANNEL.messageBuilder(SelectIntelDraftPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectIntelDraftPacket::encode)
                .decoder(SelectIntelDraftPacket::decode)
                .consumerMainThread(SelectIntelDraftPacket::handle)
                .add();

        CHANNEL.messageBuilder(ActivateIntelTechPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ActivateIntelTechPacket::encode)
                .decoder(ActivateIntelTechPacket::decode)
                .consumerMainThread(ActivateIntelTechPacket::handle)
                .add();

        CHANNEL.messageBuilder(IntelDraftStatePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(IntelDraftStatePacket::encode)
                .decoder(IntelDraftStatePacket::decode)
                .consumerMainThread(IntelDraftStatePacket::handle)
                .add();

        CHANNEL.messageBuilder(SelectLoadoutGuiPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectLoadoutGuiPacket::encode)
                .decoder(SelectLoadoutGuiPacket::decode)
                .consumerMainThread(SelectLoadoutGuiPacket::handle)
                .add();

        DpvpTweaks.LOGGER.info("[{}] Loadout GUI network packets registered", DpvpTweaks.MOD_NAME);
    }
}
