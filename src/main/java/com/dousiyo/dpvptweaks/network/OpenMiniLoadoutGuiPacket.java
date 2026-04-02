package com.dousiyo.dpvptweaks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenMiniLoadoutGuiPacket {

    public OpenMiniLoadoutGuiPacket() {
    }

    public static void encode(OpenMiniLoadoutGuiPacket msg, FriendlyByteBuf buf) {
    }

    public static OpenMiniLoadoutGuiPacket decode(FriendlyByteBuf buf) {
        return new OpenMiniLoadoutGuiPacket();
    }

    public static void handle(OpenMiniLoadoutGuiPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!context.getDirection().getReceptionSide().isClient()) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.dousiyo.dpvptweaks.client.ClientLoadoutGuiHandler.openMiniLoadoutScreen()));
        context.setPacketHandled(true);
    }
}