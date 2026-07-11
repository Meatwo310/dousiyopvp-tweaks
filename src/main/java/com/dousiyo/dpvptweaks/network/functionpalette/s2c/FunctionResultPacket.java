package com.dousiyo.dpvptweaks.network.functionpalette.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record FunctionResultPacket(boolean success, Component message) {
    public static void encode(FunctionResultPacket p, FriendlyByteBuf b) { b.writeBoolean(p.success); b.writeComponent(p.message); }
    public static FunctionResultPacket decode(FriendlyByteBuf b) { return new FunctionResultPacket(b.readBoolean(), b.readComponent()); }
    public static void handle(FunctionResultPacket p, Supplier<NetworkEvent.Context> supplier) {
        var ctx=supplier.get(); ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.dousiyo.dpvptweaks.client.function.FunctionPaletteClient.showResult(p.message)));
        ctx.setPacketHandled(true);
    }
}
