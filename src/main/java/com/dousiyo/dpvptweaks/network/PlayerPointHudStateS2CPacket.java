package com.dousiyo.dpvptweaks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class PlayerPointHudStateS2CPacket {
    public static Consumer<Boolean> CLIENT_HANDLER = ignored -> {};

    private final boolean boosted;

    public PlayerPointHudStateS2CPacket(boolean boosted) {
        this.boosted = boosted;
    }

    public boolean isBoosted() {
        return boosted;
    }

    public static void encode(PlayerPointHudStateS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.boosted);
    }

    public static PlayerPointHudStateS2CPacket decode(FriendlyByteBuf buf) {
        return new PlayerPointHudStateS2CPacket(buf.readBoolean());
    }

    public static void handle(PlayerPointHudStateS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> CLIENT_HANDLER.accept(msg.boosted));
        ctx.get().setPacketHandled(true);
    }
}