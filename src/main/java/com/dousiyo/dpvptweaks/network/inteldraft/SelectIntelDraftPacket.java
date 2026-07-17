package com.dousiyo.dpvptweaks.network.inteldraft;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SelectIntelDraftPacket {
    private final long sessionId;
    private final int choiceIndex;

    public SelectIntelDraftPacket(long sessionId, int choiceIndex) {
        this.sessionId = sessionId;
        this.choiceIndex = choiceIndex;
    }

    public long getSessionId() {
        return sessionId;
    }

    public int getChoiceIndex() {
        return choiceIndex;
    }

    public static void encode(SelectIntelDraftPacket msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.sessionId);
        buf.writeVarInt(msg.choiceIndex);
    }

    public static SelectIntelDraftPacket decode(FriendlyByteBuf buf) {
        return new SelectIntelDraftPacket(buf.readLong(), buf.readVarInt());
    }

    public static void handle(SelectIntelDraftPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        var sender = context.getSender();
        if (sender != null) context.enqueueWork(() ->
                com.dousiyo.dpvptweaks.inteldraft.IntelDraftManager.select(sender, msg.sessionId, msg.choiceIndex));
        context.setPacketHandled(true);
    }
}
