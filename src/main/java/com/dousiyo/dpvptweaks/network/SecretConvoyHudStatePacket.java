package com.dousiyo.dpvptweaks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server-authoritative HUD snapshot for a SECRET: CONVOY escort player. */
public record SecretConvoyHudStatePacket(boolean visible, float progress, int nearbyEscorts,
        boolean enemyBlocking, long remainingTicks, double remainingRouteDistance) {

    public static void encode(SecretConvoyHudStatePacket message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.visible);
        buffer.writeFloat(message.progress);
        buffer.writeVarInt(message.nearbyEscorts);
        buffer.writeBoolean(message.enemyBlocking);
        buffer.writeVarLong(message.remainingTicks);
        buffer.writeDouble(message.remainingRouteDistance);
    }

    public static SecretConvoyHudStatePacket decode(FriendlyByteBuf buffer) {
        return new SecretConvoyHudStatePacket(buffer.readBoolean(), buffer.readFloat(), buffer.readVarInt(),
                buffer.readBoolean(), buffer.readVarLong(), buffer.readDouble());
    }

    public static void handle(SecretConvoyHudStatePacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.dousiyo.dpvptweaks.client.secretoperations.ClientSecretConvoyHudState.update(message)));
        context.setPacketHandled(true);
    }
}
