package com.dousiyo.dpvptweaks.network;

import com.dousiyo.dpvptweaks.gui.LoadoutScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

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
        if (!context.getDirection().getReceptionSide().isClient() || !FMLEnvironment.dist.isClient()) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }
            mc.setScreen(new LoadoutScreen());
        });
        context.setPacketHandled(true);
    }
}
