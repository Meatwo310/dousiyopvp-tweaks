package com.dousiyo.dpvptweaks.network;

import com.dousiyo.dpvptweaks.gui.MiniLoadoutScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.loading.FMLEnvironment;
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
        if (!context.getDirection().getReceptionSide().isClient() || !FMLEnvironment.dist.isClient()) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }
            mc.setScreen(new MiniLoadoutScreen());
        });
        context.setPacketHandled(true);
    }
}

