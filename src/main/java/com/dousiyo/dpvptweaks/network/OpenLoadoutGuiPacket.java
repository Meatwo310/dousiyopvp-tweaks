package com.dousiyo.dpvptweaks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenLoadoutGuiPacket {

    public OpenLoadoutGuiPacket() {
    }

    public static void encode(OpenLoadoutGuiPacket msg, FriendlyByteBuf buf) {
    }

    public static OpenLoadoutGuiPacket decode(FriendlyByteBuf buf) {
        return new OpenLoadoutGuiPacket();
    }

    public static void handle(OpenLoadoutGuiPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!context.getDirection().getReceptionSide().isClient()) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.dousiyo.dpvptweaks.client.ClientLoadoutGuiHandler.openLoadoutScreen()));
        context.setPacketHandled(true);
    }
}