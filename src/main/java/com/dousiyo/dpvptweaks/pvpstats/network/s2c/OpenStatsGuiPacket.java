package com.dousiyo.dpvptweaks.pvpstats.network.s2c;

import com.dousiyo.dpvptweaks.pvpstats.model.AggregateStats;
import com.dousiyo.dpvptweaks.pvpstats.model.MatchRecord;
import com.dousiyo.dpvptweaks.pvpstats.model.StatsGuiPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class OpenStatsGuiPacket {
    private final StatsGuiPayload payload;

    public OpenStatsGuiPacket(StatsGuiPayload payload) {
        this.payload = payload;
    }

    public static void encode(OpenStatsGuiPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.payload.targetName(), 64);
        writeAggregate(buf, packet.payload.global());

        buf.writeVarInt(packet.payload.modeStats().size());
        for (Map.Entry<String, AggregateStats> entry : packet.payload.modeStats().entrySet()) {
            buf.writeUtf(entry.getKey(), 128);
            writeAggregate(buf, entry.getValue());
        }

        buf.writeVarInt(packet.payload.recentMatches().size());
        for (MatchRecord record : packet.payload.recentMatches()) {
            buf.writeUtf(record.matchId(), 64);
            buf.writeUtf(record.modeId(), 128);
            buf.writeUtf(record.result(), 16);
            buf.writeVarInt(record.kills());
            buf.writeVarInt(record.deaths());
            buf.writeLong(record.timestamp());
        }
    }

    public static OpenStatsGuiPacket decode(FriendlyByteBuf buf) {
        String targetName = buf.readUtf(64);
        AggregateStats global = readAggregate(buf);

        int modeCount = buf.readVarInt();
        Map<String, AggregateStats> modes = new LinkedHashMap<>();
        for (int i = 0; i < modeCount; i++) {
            modes.put(buf.readUtf(128), readAggregate(buf));
        }

        int recentCount = buf.readVarInt();
        List<MatchRecord> recentMatches = new ArrayList<>(recentCount);
        for (int i = 0; i < recentCount; i++) {
            recentMatches.add(new MatchRecord(
                    buf.readUtf(64),
                    buf.readUtf(128),
                    buf.readUtf(16),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readLong()
            ));
        }
        return new OpenStatsGuiPacket(new StatsGuiPayload(targetName, global, modes, recentMatches));
    }

    public static void handle(OpenStatsGuiPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!context.getDirection().getReceptionSide().isClient()) {
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.dousiyo.dpvptweaks.client.pvpstats.PvpStatsClient.openStatsScreen(packet.payload)));
        context.setPacketHandled(true);
    }

    private static void writeAggregate(FriendlyByteBuf buf, AggregateStats stats) {
        buf.writeLong(stats.wins());
        buf.writeLong(stats.losses());
        buf.writeLong(stats.draws());
        buf.writeLong(stats.kills());
        buf.writeLong(stats.deaths());
        buf.writeLong(stats.matches());
    }

    private static AggregateStats readAggregate(FriendlyByteBuf buf) {
        return new AggregateStats(
                buf.readLong(),
                buf.readLong(),
                buf.readLong(),
                buf.readLong(),
                buf.readLong(),
                buf.readLong()
        );
    }
}
