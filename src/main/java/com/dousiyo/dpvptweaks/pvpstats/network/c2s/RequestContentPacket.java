package com.dousiyo.dpvptweaks.pvpstats.network.c2s;

import com.dousiyo.dpvptweaks.content.AnnouncementReadState;
import com.dousiyo.dpvptweaks.content.ContentEntry;
import com.dousiyo.dpvptweaks.content.ContentService;
import com.dousiyo.dpvptweaks.content.ContentType;
import com.dousiyo.dpvptweaks.pvpstats.network.PvpStatsNetwork;
import com.dousiyo.dpvptweaks.pvpstats.network.s2c.ContentDetailPacket;
import com.dousiyo.dpvptweaks.pvpstats.network.s2c.ContentListPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Supplier;

public record RequestContentPacket(Action action, ContentType type, String key) {
    public enum Action { LIST, DETAIL }

    public static void encode(RequestContentPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.action); buf.writeEnum(packet.type); buf.writeUtf(packet.key, 128);
    }

    public static RequestContentPacket decode(FriendlyByteBuf buf) {
        return new RequestContentPacket(buf.readEnum(Action.class), buf.readEnum(ContentType.class), buf.readUtf(128));
    }

    public static void handle(RequestContentPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null) return;
            if (packet.action == Action.LIST) {
                List<ContentEntry> entries = packet.type == ContentType.ANNOUNCEMENT
                        ? ContentService.visibleAnnouncements(player).stream().map(a -> new ContentEntry(Integer.toString(a.id()), a.title(),
                                a.updatedAt(), a.importance().name(), !AnnouncementReadState.isRead(player, a.id()))).toList()
                        : ContentService.visibleRules().stream().map(r -> new ContentEntry(r.modeId(), r.title(), r.updatedAt(), "", false)).toList();
                PvpStatsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ContentListPacket(packet.type, entries));
                return;
            }
            if (packet.type == ContentType.ANNOUNCEMENT) {
                int id;
                try { id = Integer.parseInt(packet.key); } catch (NumberFormatException e) { return; }
                var announcement = ContentService.announcement(player, id);
                if (announcement == null) return;
                AnnouncementReadState.markRead(player, id);
                PvpStatsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new ContentDetailPacket(packet.type, packet.key, announcement.title(), ContentService.body(announcement)));
            } else {
                var rule = ContentService.rule(packet.key);
                if (rule == null) return;
                PvpStatsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new ContentDetailPacket(packet.type, packet.key, rule.title(), ContentService.body(rule)));
            }
        });
        context.setPacketHandled(true);
    }
}
