package com.dousiyo.dpvptweaks.network.arsenal;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;
import java.util.List;

public record ArsenalStatePacket(boolean active, boolean participant, boolean protectedState, boolean finished,
                                 int stage, int kills, int deaths,
                                 int countdownTicks, String leaderName, int leaderStage,
                                 List<Integer> occupiedStages) {
    public ArsenalStatePacket {
        occupiedStages = List.copyOf(occupiedStages);
    }

    public static void encode(ArsenalStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active); buffer.writeBoolean(packet.participant);
        buffer.writeBoolean(packet.protectedState); buffer.writeBoolean(packet.finished);
        buffer.writeVarInt(packet.stage); buffer.writeVarInt(packet.kills); buffer.writeVarInt(packet.deaths);
        buffer.writeVarInt(packet.countdownTicks); buffer.writeUtf(packet.leaderName, 64); buffer.writeVarInt(packet.leaderStage);
        buffer.writeCollection(packet.occupiedStages, FriendlyByteBuf::writeVarInt);
    }

    public static ArsenalStatePacket decode(FriendlyByteBuf buffer) {
        return new ArsenalStatePacket(buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readUtf(64), buffer.readVarInt(), buffer.readList(FriendlyByteBuf::readVarInt));
    }

    public static void handle(ArsenalStatePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.dousiyo.dpvptweaks.client.arsenal.ClientArsenalState.update(packet)));
        context.setPacketHandled(true);
    }
}
