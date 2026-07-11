package com.dousiyo.dpvptweaks.network.functionpalette.c2s;

import com.dousiyo.dpvptweaks.network.FunctionPaletteNetwork;
import com.dousiyo.dpvptweaks.network.functionpalette.s2c.FunctionResultPacket;
import com.dousiyo.dpvptweaks.server.function.FunctionService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import java.util.function.Supplier;

public record RunFunctionPacket(String buttonId, long revision) {
    public static void encode(RunFunctionPacket p, FriendlyByteBuf b) { b.writeUtf(p.buttonId, 64); b.writeLong(p.revision); }
    public static RunFunctionPacket decode(FriendlyByteBuf b) { return new RunFunctionPacket(b.readUtf(64), b.readLong()); }
    public static void handle(RunFunctionPacket p, Supplier<NetworkEvent.Context> supplier) {
        var ctx = supplier.get(); var player = ctx.getSender();
        if (player != null) ctx.enqueueWork(() -> {
            var result = FunctionService.runButton(player, p.buttonId, p.revision);
            FunctionPaletteNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new FunctionResultPacket(result.success(), result.message()));
        });
        ctx.setPacketHandled(true);
    }
}
