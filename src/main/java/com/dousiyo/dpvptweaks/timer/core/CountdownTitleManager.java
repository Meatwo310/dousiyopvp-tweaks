package com.dousiyo.dpvptweaks.timer.core;

import com.dousiyo.dpvptweaks.timer.network.CountdownHudS2CPacket;
import com.dousiyo.dpvptweaks.timer.network.ModNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class CountdownTitleManager {
    private static final String COUNTDOWN_TIMER_ID = "countdown";
    private static final int COUNTDOWN_DURATION_TICKS = 10 * 20;
    private static final Map<MinecraftServer, CountdownTitleManager> INSTANCES = new WeakHashMap<>();

    private final Map<UUID, ActiveCountdown> activeCountdowns = new HashMap<>();

    private CountdownTitleManager() {}

    public static CountdownTitleManager get(ServerLevel level) {
        return INSTANCES.computeIfAbsent(level.getServer(), ignored -> new CountdownTitleManager());
    }

    public void start(ServerPlayer player, ServerLevel level, @Nullable String finishCommand) {
        activeCountdowns.put(player.getUUID(), new ActiveCountdown(level.getGameTime(), finishCommand));
        syncRunning(player);
    }

    public void stop(ServerPlayer player) {
        activeCountdowns.remove(player.getUUID());
        syncHide(player);
    }

    public void cancel(UUID playerUuid) {
        activeCountdowns.remove(playerUuid);
    }

    public void serverTick(ServerLevel level) {
        if (activeCountdowns.isEmpty()) {
            return;
        }

        long now = level.getGameTime();
        Iterator<Map.Entry<UUID, ActiveCountdown>> iterator = activeCountdowns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveCountdown> entry = iterator.next();
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }

            ActiveCountdown countdown = entry.getValue();
            if (now - countdown.startGameTime() < COUNTDOWN_DURATION_TICKS) {
                continue;
            }

            syncFinished(player);
            if (countdown.finishCommand() != null && !countdown.finishCommand().isBlank()) {
                TimerExecutor.execute(level, player, COUNTDOWN_TIMER_ID, List.of(countdown.finishCommand()));
            }
            iterator.remove();
        }
    }

    private static void syncRunning(ServerPlayer player) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                CountdownHudS2CPacket.running(COUNTDOWN_DURATION_TICKS, COUNTDOWN_DURATION_TICKS));
    }

    private static void syncFinished(ServerPlayer player) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                CountdownHudS2CPacket.finished(COUNTDOWN_DURATION_TICKS));
    }

    private static void syncHide(ServerPlayer player) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), CountdownHudS2CPacket.hide());
    }

    private record ActiveCountdown(long startGameTime, @Nullable String finishCommand) {}
}
