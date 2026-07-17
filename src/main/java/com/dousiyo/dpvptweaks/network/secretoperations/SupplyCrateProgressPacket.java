package com.dousiyo.dpvptweaks.network.secretoperations;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SupplyCrateProgressPacket(int entityId, int progressTicks, int totalTicks, boolean active) {
    public static void encode(SupplyCrateProgressPacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId);
        buffer.writeVarInt(message.progressTicks);
        buffer.writeVarInt(message.totalTicks);
        buffer.writeBoolean(message.active);
    }

    public static SupplyCrateProgressPacket decode(FriendlyByteBuf buffer) {
        return new SupplyCrateProgressPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(SupplyCrateProgressPacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.dousiyo.dpvptweaks.client.secretoperations.ClientSupplyCrateState.update(message)));
        context.setPacketHandled(true);
    }
}
