package com.dousiyo.dpvptweaks.pvpstats.network.s2c;

import com.dousiyo.dpvptweaks.content.ContentEntry;
import com.dousiyo.dpvptweaks.content.ContentType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record ContentListPacket(ContentType type, List<ContentEntry> entries) {
    public ContentListPacket { entries = List.copyOf(entries.stream().limit(512).toList()); }

    public static void encode(ContentListPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.type); buf.writeVarInt(packet.entries.size());
        for (ContentEntry entry : packet.entries) {
            buf.writeUtf(entry.key(), 128); buf.writeUtf(entry.title(), 256); buf.writeLong(entry.updatedAt());
            buf.writeUtf(entry.badge(), 32); buf.writeBoolean(entry.unread());
        }
    }

    public static ContentListPacket decode(FriendlyByteBuf buf) {
        ContentType type = buf.readEnum(ContentType.class);
        int count = Math.min(buf.readVarInt(), 512);
        List<ContentEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) entries.add(new ContentEntry(buf.readUtf(128), buf.readUtf(256), buf.readLong(), buf.readUtf(32), buf.readBoolean()));
        return new ContentListPacket(type, entries);
    }

    public static void handle(ContentListPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.dousiyo.dpvptweaks.client.content.ContentClient.receiveList(packet.type, packet.entries)));
        context.setPacketHandled(true);
    }
}
