package com.dousiyo.dpvptweaks.network.loadout;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CloseLoadoutGuiPacket {

    public CloseLoadoutGuiPacket() {
    }

    public static void encode(CloseLoadoutGuiPacket msg, FriendlyByteBuf buf) {
    }

    public static CloseLoadoutGuiPacket decode(FriendlyByteBuf buf) {
        return new CloseLoadoutGuiPacket();
    }

    public static void handle(CloseLoadoutGuiPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!context.getDirection().getReceptionSide().isClient()) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.dousiyo.dpvptweaks.client.loadout.ClientLoadoutGuiHandler.closeLoadoutScreen()));
        context.setPacketHandled(true);
    }
}