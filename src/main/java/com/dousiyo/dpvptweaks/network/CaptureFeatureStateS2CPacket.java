package com.dousiyo.dpvptweaks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class CaptureFeatureStateS2CPacket {
    public static Consumer<Boolean> CLIENT_HANDLER = ignored -> {};

    private final boolean enabled;

    public CaptureFeatureStateS2CPacket(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static void encode(CaptureFeatureStateS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.enabled);
    }

    public static CaptureFeatureStateS2CPacket decode(FriendlyByteBuf buf) {
        return new CaptureFeatureStateS2CPacket(buf.readBoolean());
    }

    public static void handle(CaptureFeatureStateS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> CLIENT_HANDLER.accept(msg.enabled));
        ctx.get().setPacketHandled(true);
    }
}