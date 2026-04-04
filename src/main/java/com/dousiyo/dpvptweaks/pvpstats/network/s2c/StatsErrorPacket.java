package com.dousiyo.dpvptweaks.pvpstats.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class StatsErrorPacket {
    private final Component message;

    public StatsErrorPacket(Component message) {
        this.message = message;
    }

    public static void encode(StatsErrorPacket packet, FriendlyByteBuf buf) {
        buf.writeComponent(packet.message);
    }

    public static StatsErrorPacket decode(FriendlyByteBuf buf) {
        return new StatsErrorPacket(buf.readComponent());
    }

    public static void handle(StatsErrorPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!context.getDirection().getReceptionSide().isClient()) {
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.dousiyo.dpvptweaks.client.pvpstats.PvpStatsClient.showError(packet.message)));
        context.setPacketHandled(true);
    }
}
