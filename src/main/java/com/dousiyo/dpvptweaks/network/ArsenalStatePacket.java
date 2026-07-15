package com.dousiyo.dpvptweaks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ArsenalStatePacket(boolean active, boolean protectedState, boolean finished, int stage, int kills, int deaths) {
    public static void encode(ArsenalStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active); buffer.writeBoolean(packet.protectedState); buffer.writeBoolean(packet.finished);
        buffer.writeVarInt(packet.stage); buffer.writeVarInt(packet.kills); buffer.writeVarInt(packet.deaths);
    }

    public static ArsenalStatePacket decode(FriendlyByteBuf buffer) {
        return new ArsenalStatePacket(buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(ArsenalStatePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.dousiyo.dpvptweaks.client.arsenal.ClientArsenalState.update(packet)));
        context.setPacketHandled(true);
    }
}
