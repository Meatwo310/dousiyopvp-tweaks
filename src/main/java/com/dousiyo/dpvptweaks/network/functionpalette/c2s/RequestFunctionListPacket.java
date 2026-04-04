package com.dousiyo.dpvptweaks.network.functionpalette.c2s;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.network.FunctionPaletteNetwork;
import com.dousiyo.dpvptweaks.network.functionpalette.s2c.FunctionListPacket;
import com.dousiyo.dpvptweaks.server.function.FunctionPermissionService;
import com.dousiyo.dpvptweaks.server.function.FunctionService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Supplier;

public final class RequestFunctionListPacket {
    public static void encode(RequestFunctionListPacket packet, FriendlyByteBuf buf) {
    }

    public static RequestFunctionListPacket decode(FriendlyByteBuf buf) {
        return new RequestFunctionListPacket();
    }

    public static void handle(RequestFunctionListPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        ServerPlayer player = context.getSender();
        if (player == null) {
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> {
            if (!FunctionPermissionService.canUse(player)) {
                player.sendSystemMessage(Component.translatable("message.dpvptweaks.function_palette.no_permission"));
                return;
            }

            try {
                FunctionService.PaletteData paletteData = FunctionService.getPaletteData(player.server);
                FunctionPaletteNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new FunctionListPacket(paletteData.categories()));
            } catch (Exception e) {
                DpvpTweaks.LOGGER.error("[{}] Failed to build function list for {}", DpvpTweaks.MOD_NAME, player.getGameProfile().getName(), e);
                player.sendSystemMessage(Component.translatable("message.dpvptweaks.function_palette.list_failed"));
            }
        });
        context.setPacketHandled(true);
    }
}
