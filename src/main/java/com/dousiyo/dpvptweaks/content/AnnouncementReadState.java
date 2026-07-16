package com.dousiyo.dpvptweaks.content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.BitSet;

@Mod.EventBusSubscriber
public final class AnnouncementReadState {
    private static final String TAG = "dpvptweaksReadAnnouncements";

    private AnnouncementReadState() {}

    public static boolean isRead(ServerPlayer player, int id) {
        return read(player).get(id);
    }

    public static void markRead(ServerPlayer player, int id) {
        BitSet bits = read(player);
        bits.set(id);
        player.getPersistentData().putLongArray(TAG, bits.toLongArray());
    }

    private static BitSet read(ServerPlayer player) {
        return BitSet.valueOf(player.getPersistentData().getLongArray(TAG));
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer) || !(event.getEntity() instanceof ServerPlayer newPlayer)) return;
        CompoundTag oldData = oldPlayer.getPersistentData();
        if (oldData.contains(TAG)) newPlayer.getPersistentData().putLongArray(TAG, oldData.getLongArray(TAG));
    }
}
