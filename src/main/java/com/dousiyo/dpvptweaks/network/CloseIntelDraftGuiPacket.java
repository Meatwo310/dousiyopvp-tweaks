package com.dousiyo.dpvptweaks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CloseIntelDraftGuiPacket {
    public CloseIntelDraftGuiPacket() {
    }

    public static void encode(CloseIntelDraftGuiPacket msg, FriendlyByteBuf buf) {
    }

    public static CloseIntelDraftGuiPacket decode(FriendlyByteBuf buf) {
        return new CloseIntelDraftGuiPacket();
    }

    public static void handle(CloseIntelDraftGuiPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!context.getDirection().getReceptionSide().isClient()) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.dousiyo.dpvptweaks.client.ClientLoadoutGuiHandler.closeIntelDraftScreen()));
        context.setPacketHandled(true);
    }
}
