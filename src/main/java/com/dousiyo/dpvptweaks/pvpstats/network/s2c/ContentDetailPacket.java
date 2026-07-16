package com.dousiyo.dpvptweaks.pvpstats.network.s2c;

import com.dousiyo.dpvptweaks.content.ContentType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ContentDetailPacket(ContentType type, String key, String title, String markdown) {
    public static void encode(ContentDetailPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.type); buf.writeUtf(packet.key, 128); buf.writeUtf(packet.title, 256); buf.writeUtf(packet.markdown, 32767);
    }

    public static ContentDetailPacket decode(FriendlyByteBuf buf) {
        return new ContentDetailPacket(buf.readEnum(ContentType.class), buf.readUtf(128), buf.readUtf(256), buf.readUtf(32767));
    }

    public static void handle(ContentDetailPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.dousiyo.dpvptweaks.client.content.ContentClient.receiveDetail(packet.type, packet.key, packet.title, packet.markdown)));
        context.setPacketHandled(true);
    }
}
