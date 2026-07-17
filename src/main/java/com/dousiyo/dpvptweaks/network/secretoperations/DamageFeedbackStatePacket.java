package com.dousiyo.dpvptweaks.network.secretoperations;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DamageFeedbackStatePacket(boolean enabled) {
    public static void encode(DamageFeedbackStatePacket message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.enabled);
    }

    public static DamageFeedbackStatePacket decode(FriendlyByteBuf buffer) {
        return new DamageFeedbackStatePacket(buffer.readBoolean());
    }

    public static void handle(DamageFeedbackStatePacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.dousiyo.dpvptweaks.client.secretoperations.ClientDamageFeedbackState.update(message.enabled)));
        context.setPacketHandled(true);
    }
}
