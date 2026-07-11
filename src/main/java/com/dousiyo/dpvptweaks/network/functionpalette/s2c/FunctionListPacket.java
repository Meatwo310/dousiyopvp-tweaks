package com.dousiyo.dpvptweaks.network.functionpalette.s2c;

import com.dousiyo.dpvptweaks.functionpalette.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.*;
import java.util.function.Supplier;

public record FunctionListPacket(FunctionPaletteCategory data) {
    public static void encode(FunctionListPacket p, FriendlyByteBuf b) {
        b.writeLong(p.data.revision()); b.writeVarInt(p.data.actions().size());
        for (var a : p.data.actions()) { b.writeUtf(a.id(),64); b.writeUtf(a.name(),128); b.writeUtf(a.description(),512); b.writeUtf(a.icon(),256); b.writeBoolean(a.confirmation()); }
    }
    public static FunctionListPacket decode(FriendlyByteBuf b) {
        long revision=b.readLong(); int count=b.readVarInt(); if(count<0||count>256) throw new IllegalArgumentException("Invalid button count");
        List<FunctionPaletteAction> list=new ArrayList<>(count);
        for(int i=0;i<count;i++) list.add(new FunctionPaletteAction(b.readUtf(64),b.readUtf(128),b.readUtf(512),b.readUtf(256),b.readBoolean()));
        return new FunctionListPacket(new FunctionPaletteCategory(revision,list));
    }
    public static void handle(FunctionListPacket p, Supplier<NetworkEvent.Context> supplier) {
        var ctx=supplier.get(); ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.dousiyo.dpvptweaks.client.function.FunctionPaletteClient.applyPaletteData(p.data)));
        ctx.setPacketHandled(true);
    }
}
