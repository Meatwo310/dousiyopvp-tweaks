package com.dousiyo.dpvptweaks.network.secretoperations;

import com.dousiyo.dpvptweaks.secretoperations.SecretShowdownSupplyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SupplyCrateHoldPacket(int entityId, boolean holding) {
    public static void encode(SupplyCrateHoldPacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId);
        buffer.writeBoolean(message.holding);
    }

    public static SupplyCrateHoldPacket decode(FriendlyByteBuf buffer) {
        return new SupplyCrateHoldPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(SupplyCrateHoldPacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null)
                SecretShowdownSupplyManager.handleHold(context.getSender(), message.entityId, message.holding);
        });
        context.setPacketHandled(true);
    }
}
