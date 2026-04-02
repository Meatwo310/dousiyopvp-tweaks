package com.dousiyo.dpvptweaks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class PlayerPointFocusS2CPacket {
    public static IntConsumer CLIENT_HANDLER = ignored -> {};

    private final byte slotIndex;

    public PlayerPointFocusS2CPacket(byte slotIndex) {
        this.slotIndex = slotIndex;
    }

    public byte getSlotIndex() {
        return slotIndex;
    }

    public static void encode(PlayerPointFocusS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.slotIndex);
    }

    public static PlayerPointFocusS2CPacket decode(FriendlyByteBuf buf) {
        return new PlayerPointFocusS2CPacket(buf.readByte());
    }

    public static void handle(PlayerPointFocusS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> CLIENT_HANDLER.accept(msg.slotIndex));
        ctx.get().setPacketHandled(true);
    }
}

