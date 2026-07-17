package com.dousiyo.dpvptweaks.network.secretoperations;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SecretOperationsStatePacket(boolean active) {
    public static void encode(SecretOperationsStatePacket message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.active);
    }

    public static SecretOperationsStatePacket decode(FriendlyByteBuf buffer) {
        return new SecretOperationsStatePacket(buffer.readBoolean());
    }

    public static void handle(SecretOperationsStatePacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.dousiyo.dpvptweaks.client.secretoperations.ClientSecretOperationsState.update(message.active)));
        context.setPacketHandled(true);
    }
}
