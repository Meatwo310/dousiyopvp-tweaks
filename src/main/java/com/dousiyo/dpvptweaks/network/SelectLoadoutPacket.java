package com.dousiyo.dpvptweaks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SelectLoadoutPacket {

    private final String loadoutId;

    public SelectLoadoutPacket(String loadoutId) {
        this.loadoutId = loadoutId;
    }

    public String getLoadoutId() {
        return loadoutId;
    }

    public static void encode(SelectLoadoutPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.loadoutId, 64);
    }

    public static SelectLoadoutPacket decode(FriendlyByteBuf buf) {
        return new SelectLoadoutPacket(buf.readUtf(64));
    }

    public static void handle(SelectLoadoutPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
    }
}

