package com.dousiyo.dpvptweaks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RerollIntelDraftPacket {
    private final long sessionId;

    public RerollIntelDraftPacket(long sessionId) {
        this.sessionId = sessionId;
    }

    public static void encode(RerollIntelDraftPacket msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.sessionId);
    }

    public static RerollIntelDraftPacket decode(FriendlyByteBuf buf) {
        return new RerollIntelDraftPacket(buf.readLong());
    }

    public static void handle(RerollIntelDraftPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        var sender = context.getSender();
        if (sender != null) context.enqueueWork(() ->
                com.dousiyo.dpvptweaks.inteldraft.IntelDraftManager.reroll(sender, msg.sessionId));
        context.setPacketHandled(true);
    }
}
