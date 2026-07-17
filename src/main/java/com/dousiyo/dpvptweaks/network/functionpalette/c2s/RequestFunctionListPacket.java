package com.dousiyo.dpvptweaks.network.functionpalette.c2s;

import com.dousiyo.dpvptweaks.network.functionpalette.FunctionPaletteNetwork;
import com.dousiyo.dpvptweaks.network.functionpalette.s2c.FunctionListPacket;
import com.dousiyo.dpvptweaks.network.functionpalette.s2c.FunctionResultPacket;
import com.dousiyo.dpvptweaks.server.function.FunctionPaletteManager;
import com.dousiyo.dpvptweaks.server.function.FunctionPermissionService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import java.util.function.Supplier;

public final class RequestFunctionListPacket {
    public static void encode(RequestFunctionListPacket p, FriendlyByteBuf b) {}
    public static RequestFunctionListPacket decode(FriendlyByteBuf b) { return new RequestFunctionListPacket(); }
    public static void handle(RequestFunctionListPacket p, Supplier<NetworkEvent.Context> supplier) {
        var ctx = supplier.get(); var player = ctx.getSender();
        if (player != null) ctx.enqueueWork(() -> {
            if (!FunctionPermissionService.canUse(player)) {
                FunctionPaletteNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new FunctionResultPacket(false, Component.translatable("message.dpvptweaks.function_palette.no_permission")));
            } else FunctionPaletteNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new FunctionListPacket(FunctionPaletteManager.clientData()));
        });
        ctx.setPacketHandled(true);
    }
}
