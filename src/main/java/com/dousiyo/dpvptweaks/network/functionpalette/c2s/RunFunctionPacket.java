package com.dousiyo.dpvptweaks.network.functionpalette.c2s;

import com.dousiyo.dpvptweaks.server.function.FunctionService;
import com.dousiyo.dpvptweaks.server.function.FunctionPaletteServerConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class RunFunctionPacket {
    private static final int MAX_ID_LENGTH = 512;

    private final String functionId;

    public RunFunctionPacket(String functionId) {
        this.functionId = functionId;
    }

    public static void encode(RunFunctionPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.functionId, MAX_ID_LENGTH);
    }

    public static RunFunctionPacket decode(FriendlyByteBuf buf) {
        return new RunFunctionPacket(buf.readUtf(MAX_ID_LENGTH));
    }

    public static void handle(RunFunctionPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        ServerPlayer player = context.getSender();
        if (player == null) {
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> {
            FunctionService.RunResult result = FunctionService.runFunction(player, packet.functionId);
            if (!result.success() || FunctionPaletteServerConfig.SHOW_RUN_RESULT.get()) {
                player.sendSystemMessage(result.message());
            }
        });
        context.setPacketHandled(true);
    }
}
