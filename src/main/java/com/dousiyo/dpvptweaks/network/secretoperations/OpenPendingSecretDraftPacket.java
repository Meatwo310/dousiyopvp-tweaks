package com.dousiyo.dpvptweaks.network.secretoperations;

import com.dousiyo.dpvptweaks.secretoperations.SecretShowdownManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class OpenPendingSecretDraftPacket {
    public static void encode(OpenPendingSecretDraftPacket ignored, FriendlyByteBuf buf) {}
    public static OpenPendingSecretDraftPacket decode(FriendlyByteBuf buf) { return new OpenPendingSecretDraftPacket(); }
    public static void handle(OpenPendingSecretDraftPacket ignored, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> { if (ctx.getSender() != null) {
            SecretShowdownManager.openPendingDraft(ctx.getSender());
            com.dousiyo.dpvptweaks.secretoperations.SecretConvoyManager.openPendingDraft(ctx.getSender());
        }});
        ctx.setPacketHandled(true);
    }
}
