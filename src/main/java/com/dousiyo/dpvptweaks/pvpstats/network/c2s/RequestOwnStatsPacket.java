package com.dousiyo.dpvptweaks.pvpstats.network.c2s;

import com.dousiyo.dpvptweaks.pvpstats.model.StatsGuiPayload;
import com.dousiyo.dpvptweaks.pvpstats.network.PvpStatsNetwork;
import com.dousiyo.dpvptweaks.pvpstats.network.s2c.OpenStatsGuiPacket;
import com.dousiyo.dpvptweaks.pvpstats.service.PvpStatsQueryService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public final class RequestOwnStatsPacket {
    public static void encode(RequestOwnStatsPacket packet, FriendlyByteBuf buf) {
    }

    public static RequestOwnStatsPacket decode(FriendlyByteBuf buf) {
        return new RequestOwnStatsPacket();
    }

    public static void handle(RequestOwnStatsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        ServerPlayer player = context.getSender();
        if (player == null) {
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> {
            StatsGuiPayload payload = PvpStatsQueryService.query(player.serverLevel(), player.getUUID(), player.getGameProfile().getName());
            PvpStatsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenStatsGuiPacket(payload));
        });
        context.setPacketHandled(true);
    }
}
