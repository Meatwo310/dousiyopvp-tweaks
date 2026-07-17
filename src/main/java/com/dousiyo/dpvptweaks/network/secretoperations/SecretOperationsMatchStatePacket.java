package com.dousiyo.dpvptweaks.network.secretoperations;

import com.dousiyo.dpvptweaks.secretoperations.SecretShowdownPhase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SecretOperationsMatchStatePacket(boolean participating, SecretShowdownPhase phase,
        int redScore, int blueScore, long remainingTicks, int personalPoints, int pendingDrafts, boolean waiting, boolean dropProtected) {
    public static void encode(SecretOperationsMatchStatePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.participating); buf.writeEnum(msg.phase); buf.writeVarInt(msg.redScore); buf.writeVarInt(msg.blueScore);
        buf.writeVarLong(msg.remainingTicks); buf.writeVarInt(msg.personalPoints); buf.writeVarInt(msg.pendingDrafts);
        buf.writeBoolean(msg.waiting); buf.writeBoolean(msg.dropProtected);
    }
    public static SecretOperationsMatchStatePacket decode(FriendlyByteBuf buf) {
        return new SecretOperationsMatchStatePacket(buf.readBoolean(), buf.readEnum(SecretShowdownPhase.class),
                buf.readVarInt(), buf.readVarInt(), buf.readVarLong(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean());
    }
    public static void handle(SecretOperationsMatchStatePacket msg, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.dousiyo.dpvptweaks.client.secretoperations.ClientShowdownState.update(msg)));
        ctx.setPacketHandled(true);
    }
}
