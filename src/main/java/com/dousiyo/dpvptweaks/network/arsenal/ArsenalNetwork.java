package com.dousiyo.dpvptweaks.network.arsenal;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.arsenal.ArsenalMatchState;
import com.dousiyo.dpvptweaks.arsenal.ArsenalPlayerData;
import com.dousiyo.dpvptweaks.arsenal.ArsenalSavedData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Comparator;
import java.util.List;

public final class ArsenalNetwork {
    private static final String VERSION = "4";
    private static boolean registered;
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(DpvpTweaks.MODID, "arsenal"), () -> VERSION, VERSION::equals, VERSION::equals);

    private ArsenalNetwork() {}

    public static void register() {
        if (registered) return;
        registered = true;
        CHANNEL.messageBuilder(ArsenalStatePacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ArsenalStatePacket::encode).decoder(ArsenalStatePacket::decode)
                .consumerMainThread(ArsenalStatePacket::handle).add();
        CHANNEL.messageBuilder(OpenArsenalAdminPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenArsenalAdminPacket::encode).decoder(OpenArsenalAdminPacket::decode)
                .consumerMainThread(OpenArsenalAdminPacket::handle).add();
        CHANNEL.messageBuilder(ArsenalAdminActionPacket.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ArsenalAdminActionPacket::encode).decoder(ArsenalAdminActionPacket::decode)
                .consumerMainThread(ArsenalAdminActionPacket::handle).add();
    }

    public static void sync(ServerPlayer player, ArsenalSavedData match, ArsenalPlayerData data) {
        long now = player.server.overworld().getGameTime();
        boolean protectedState = match.state == ArsenalMatchState.RUNNING
                && data.protectedAt(now);
        ArsenalPlayerData leader = match.players.values().stream()
                .max(Comparator.comparingInt((ArsenalPlayerData entry) -> entry.stage)
                        .thenComparingInt(entry -> entry.kills)
                        .thenComparing(entry -> entry.lastKnownName, String.CASE_INSENSITIVE_ORDER.reversed()))
                .orElse(data);
        int countdownTicks = match.state == ArsenalMatchState.RUNNING
                ? (int) Math.max(0L, Math.min(Integer.MAX_VALUE, match.countdownEndGameTime - now)) : 0;
        List<Integer> occupiedStages = occupiedStages(player, match);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ArsenalStatePacket(true, true, protectedState,
                match.state == ArsenalMatchState.FINISHED, data.stage + 1, data.kills, data.deaths,
                countdownTicks, leader.lastKnownName, leader.stage + 1, occupiedStages));
    }

    public static void syncObserver(ServerPlayer player, ArsenalSavedData match) {
        ArsenalPlayerData leader = match.players.values().stream()
                .max(Comparator.comparingInt((ArsenalPlayerData entry) -> entry.stage)
                        .thenComparingInt(entry -> entry.kills)
                        .thenComparing(entry -> entry.lastKnownName, String.CASE_INSENSITIVE_ORDER.reversed()))
                .orElse(null);
        long now = player.server.overworld().getGameTime();
        int countdownTicks = match.state == ArsenalMatchState.RUNNING
                ? (int) Math.max(0L, Math.min(Integer.MAX_VALUE, match.countdownEndGameTime - now)) : 0;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ArsenalStatePacket(true, false, false,
                match.state == ArsenalMatchState.FINISHED, 0, 0, 0, countdownTicks,
                leader == null ? "" : leader.lastKnownName, leader == null ? 0 : leader.stage + 1,
                occupiedStages(player, match)));
    }

    public static void syncInactive(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ArsenalStatePacket(false, false, false, false, 0, 0, 0, 0, "", 0, List.of()));
    }

    private static List<Integer> occupiedStages(ServerPlayer viewer, ArsenalSavedData match) {
        return match.players.values().stream()
                .filter(entry -> viewer.server.getPlayerList().getPlayer(entry.playerId) != null)
                .map(entry -> entry.stage + 1)
                .distinct()
                .sorted()
                .toList();
    }
}
