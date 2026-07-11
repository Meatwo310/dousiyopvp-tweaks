package com.dousiyo.dpvptweaks.pvpstats.network.c2s;

import com.dousiyo.dpvptweaks.pvpstats.model.PlayerPrivacySettings;
import com.dousiyo.dpvptweaks.pvpstats.model.PlayerStats;
import com.dousiyo.dpvptweaks.pvpstats.model.StatsGuiPayload;
import com.dousiyo.dpvptweaks.pvpstats.network.PvpStatsNetwork;
import com.dousiyo.dpvptweaks.pvpstats.network.s2c.OpenStatsGuiPacket;
import com.dousiyo.dpvptweaks.pvpstats.service.PvpStatsQueryService;
import com.dousiyo.dpvptweaks.pvpstats.util.SavedDataAccessor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public final class UpdatePrivacySettingsPacket {
    private final PlayerPrivacySettings settings;

    public UpdatePrivacySettingsPacket(PlayerPrivacySettings settings) {
        this.settings = settings == null ? PlayerPrivacySettings.DEFAULT : settings;
    }

    public static void encode(UpdatePrivacySettingsPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.settings.showRank());
        buf.writeBoolean(packet.settings.showStats());
        buf.writeBoolean(packet.settings.showMatchHistory());
        buf.writeBoolean(packet.settings.joinLeaderboards());
    }

    public static UpdatePrivacySettingsPacket decode(FriendlyByteBuf buf) {
        return new UpdatePrivacySettingsPacket(new PlayerPrivacySettings(
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean()
        ));
    }

    public static void handle(UpdatePrivacySettingsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        ServerPlayer player = context.getSender();
        if (player == null) {
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> {
            var savedData = SavedDataAccessor.get(player.serverLevel());
            PlayerStats playerStats = savedData.getOrCreate(player.getUUID());
            playerStats.setLastKnownName(player.getGameProfile().getName());
            playerStats.setPrivacySettings(packet.settings);
            savedData.setDirty();

            StatsGuiPayload payload = PvpStatsQueryService.query(
                    player.serverLevel(),
                    player.getUUID(),
                    player.getUUID(),
                    player.getGameProfile().getName()
            );
            PvpStatsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenStatsGuiPacket(payload));
        });
        context.setPacketHandled(true);
    }
}
