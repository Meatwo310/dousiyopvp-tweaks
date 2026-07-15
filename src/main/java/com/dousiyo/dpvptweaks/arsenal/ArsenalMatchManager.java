package com.dousiyo.dpvptweaks.arsenal;

import com.dousiyo.dpvptweaks.network.ArsenalNetwork;
import com.dousiyo.dpvptweaks.network.OpenArsenalAdminPacket;
import com.dousiyo.dpvptweaks.pvpstats.service.PvpStatsMutationService;
import com.dousiyo.dpvptweaks.pvpstats.util.SavedDataAccessor;
import com.dousiyo.dpvptweaks.secretoperations.SecretShowdownManager;
import com.dousiyo.dpvptweaks.secretoperations.SecretConvoyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraftforge.network.PacketDistributor;

public final class ArsenalMatchManager {
    public static final int PROTECTION_TICKS = 100;
    private static final Map<UUID, Long> LAST_DEATH_TICKS = new HashMap<>();

    private ArsenalMatchManager() {}

    public static ActionResult start(MinecraftServer server, String weaponSetId) {
        ArsenalSavedData data = ArsenalSavedData.get(server);
        if (data.state != ArsenalMatchState.WAITING) return ActionResult.error("アーセナルを開始する前にresetしてください");
        if (SecretShowdownManager.activeMatch()) return ActionResult.error("SECRET SHOWDOWNの試合中です");
        if (SecretConvoyManager.activeMatch()) return ActionResult.error("SECRET CONVOYの試合中です");
        ArsenalConfig.Validation config = ArsenalConfig.validate(server);
        if (!config.valid()) return ActionResult.error(config.error());
        ArsenalWeaponSet set = ArsenalWeaponSetManager.get(weaponSetId).orElse(null);
        if (set == null) return ActionResult.error("有効な武器セットがありません: " + weaponSetId);
        var participants = server.getPlayerList().getPlayers().stream().filter(p -> !isAdmin(p)).toList();
        if (participants.size() < 2) return ActionResult.error("adminチーム以外の参加者が2人以上必要です");

        data.clearMatch();
        data.state = ArsenalMatchState.RUNNING;
        data.matchId = UUID.randomUUID();
        data.weaponSetId = set.id();
        data.snapshot = set;
        for (ServerPlayer player : participants) {
            ArsenalPlayerData participant = new ArsenalPlayerData(player.getUUID(), player.getGameProfile().getName());
            data.players.put(player.getUUID(), participant);
            player.setGameMode(GameType.ADVENTURE);
            if (!ArsenalEquipmentService.giveStage(player, data.matchId, 0, set.stages().get(0))) {
                for (ServerPlayer rollback : participants) {
                    ArsenalEquipmentService.removeMatchEquipment(rollback);
                    ArsenalNetwork.syncInactive(rollback);
                }
                data.clearMatch();
                return ActionResult.error("インベントリに第1段階の装備を支給できません: " + player.getGameProfile().getName());
            }
            ArsenalNetwork.sync(player, data, participant);
        }
        data.setDirty();
        broadcast(server, data, Component.literal("ARSENAL START - " + set.displayName()).withStyle(ChatFormatting.GOLD));
        return ActionResult.ok("アーセナルを開始しました: 参加者=" + participants.size());
    }

    public static ActionResult stop(MinecraftServer server) {
        ArsenalSavedData data = ArsenalSavedData.get(server);
        if (data.state != ArsenalMatchState.RUNNING) return ActionResult.error("アーセナルは進行していません");
        finishWithoutWinner(server, data);
        return ActionResult.ok("アーセナルを中止しました");
    }

    public static ActionResult reset(MinecraftServer server) {
        ArsenalSavedData data = ArsenalSavedData.get(server);
        for (UUID id : data.players.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) { ArsenalEquipmentService.removeMatchEquipment(player); ArsenalNetwork.syncInactive(player); }
        }
        data.clearMatch();
        LAST_DEATH_TICKS.clear();
        return ActionResult.ok("アーセナルをWAITINGへリセットしました");
    }

    public static String status(MinecraftServer server) {
        ArsenalSavedData data = ArsenalSavedData.get(server);
        return "state=" + data.state + ", weapon_set=" + data.weaponSetId + ", players=" + data.players.size()
                + (data.winner == null ? "" : ", winner=" + data.winner);
    }

    public static void openAdmin(ServerPlayer player, String notice, String preferredWeaponSet) {
        ArsenalSavedData data = ArsenalSavedData.get(player.server);
        String selected = preferredWeaponSet == null || preferredWeaponSet.isBlank()
                ? (data.weaponSetId.isBlank() ? "default" : data.weaponSetId) : preferredWeaponSet;
        String configError = ArsenalConfig.validate(player.server).error();
        var participants = data.state == ArsenalMatchState.WAITING
                ? player.server.getPlayerList().getPlayers().stream().filter(candidate -> !isAdmin(candidate))
                        .map(candidate -> "● " + candidate.getGameProfile().getName() + "  (開始対象)").toList()
                : data.players.values().stream().map(entry -> {
                    boolean online = player.server.getPlayerList().getPlayer(entry.playerId) != null;
                    return (online ? "● " : "○ ") + entry.lastKnownName + "  段階 " + (entry.stage + 1)
                            + "  K " + entry.kills + " / D " + entry.deaths;
                }).toList();
        ArsenalNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenArsenalAdminPacket(data.state.name(), selected, data.weaponSetId,
                        configError == null ? "" : configError, notice == null ? "" : notice,
                        ArsenalWeaponSetManager.list(), participants));
    }

    public static void playerLogin(ServerPlayer player) {
        ArsenalSavedData data = ArsenalSavedData.get(player.server);
        ArsenalPlayerData participant = data.players.get(player.getUUID());
        if (data.state == ArsenalMatchState.RUNNING) {
            if (participant == null && !isAdmin(player)) {
                participant = new ArsenalPlayerData(player.getUUID(), player.getGameProfile().getName());
                data.players.put(player.getUUID(), participant);
                data.setDirty();
            }
            if (participant != null) {
                participant.lastKnownName = player.getGameProfile().getName();
                player.setGameMode(GameType.ADVENTURE);
                ArsenalEquipmentService.giveStage(player, data.matchId, participant.stage, data.snapshot.stages().get(participant.stage));
                ArsenalNetwork.sync(player, data, participant);
                data.setDirty();
                return;
            }
        } else if (participant != null) {
            ArsenalNetwork.sync(player, data, participant);
            return;
        }
        ArsenalNetwork.syncInactive(player);
    }

    public static void handleDeath(ServerPlayer victim) {
        ArsenalSavedData data = ArsenalSavedData.get(victim.server);
        if (data.state != ArsenalMatchState.RUNNING) return;
        ArsenalPlayerData victimData = data.players.get(victim.getUUID());
        if (victimData == null) return;
        long tick = victim.server.overworld().getGameTime();
        if (LAST_DEATH_TICKS.getOrDefault(victim.getUUID(), Long.MIN_VALUE) == tick) return;
        LAST_DEATH_TICKS.put(victim.getUUID(), tick);
        victimData.deaths++;

        ServerPlayer killer = victim.getKillCredit() instanceof ServerPlayer candidate ? candidate : null;
        ArsenalPlayerData killerData = killer == null ? null : data.players.get(killer.getUUID());
        if (killerData != null && killer != victim) {
            killerData.kills++;
            if (killerData.stage >= ArsenalWeaponSet.STAGE_COUNT - 1) {
                win(victim.server, data, killer);
                return;
            }
            killerData.stage++;
            ArsenalEquipmentService.giveStage(killer, data.matchId, killerData.stage, data.snapshot.stages().get(killerData.stage));
            ArsenalNetwork.sync(killer, data, killerData);
        }
        data.setDirty();
    }

    public static void playerRespawn(ServerPlayer player) {
        ArsenalSavedData data = ArsenalSavedData.get(player.server);
        if (data.state != ArsenalMatchState.RUNNING) return;
        ArsenalPlayerData participant = data.players.get(player.getUUID());
        if (participant == null) return;
        ArsenalConfig.Validation validation = ArsenalConfig.validate(player.server);
        if (!validation.valid()) return;
        ArsenalConfig.AirSpawn spawn = validation.spawn();
        double x = ThreadLocalRandom.current().nextDouble(spawn.minX, spawn.maxX);
        double z = ThreadLocalRandom.current().nextDouble(spawn.minZ, spawn.maxZ);
        player.teleportTo(validation.level(), x, spawn.y, z, spawn.yaw, spawn.pitch);
        player.setDeltaMovement(0, 0, 0);
        player.setGameMode(GameType.ADVENTURE);
        participant.protectionEndGameTime = player.server.overworld().getGameTime() + PROTECTION_TICKS;
        ArsenalEquipmentService.giveStage(player, data.matchId, participant.stage, data.snapshot.stages().get(participant.stage));
        data.setDirty();
        ArsenalNetwork.sync(player, data, participant);
    }

    public static void serverTick(MinecraftServer server) {
        ArsenalSavedData data = ArsenalSavedData.get(server);
        if (data.state != ArsenalMatchState.RUNNING) return;
        long now = server.overworld().getGameTime();
        for (ArsenalPlayerData participant : data.players.values()) {
            if (participant.protectionEndGameTime > 0 && participant.protectionEndGameTime <= now) {
                participant.protectionEndGameTime = 0;
                ServerPlayer player = server.getPlayerList().getPlayer(participant.playerId);
                if (player != null) ArsenalNetwork.sync(player, data, participant);
                data.setDirty();
            }
        }
    }

    public static boolean isParticipant(ServerPlayer player) {
        ArsenalSavedData data = ArsenalSavedData.get(player.server);
        return data.state != ArsenalMatchState.WAITING && data.players.containsKey(player.getUUID());
    }

    public static boolean isProtected(ServerPlayer player) {
        ArsenalSavedData data = ArsenalSavedData.get(player.server);
        ArsenalPlayerData participant = data.players.get(player.getUUID());
        return data.state == ArsenalMatchState.RUNNING && participant != null
                && participant.protectedAt(player.server.overworld().getGameTime());
    }

    public static boolean finishedParticipant(ServerPlayer player) {
        ArsenalSavedData data = ArsenalSavedData.get(player.server);
        return data.state == ArsenalMatchState.FINISHED && data.players.containsKey(player.getUUID());
    }

    public static boolean activeMatch(MinecraftServer server) {
        return ArsenalSavedData.get(server).state == ArsenalMatchState.RUNNING;
    }

    private static void win(MinecraftServer server, ArsenalSavedData data, ServerPlayer winner) {
        data.state = ArsenalMatchState.FINISHED;
        data.winner = winner.getUUID();
        data.players.values().forEach(p -> p.protectionEndGameTime = 0);
        Component title = Component.literal(winner.getGameProfile().getName() + " VICTORY").withStyle(ChatFormatting.GOLD);
        for (UUID id : data.players.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                player.connection.send(new ClientboundSetTitleTextPacket(title));
                ArsenalEquipmentService.removeMatchEquipment(player);
                ArsenalNetwork.sync(player, data, data.players.get(id));
            }
        }
        broadcast(server, data, Component.literal("勝者: " + winner.getGameProfile().getName()).withStyle(ChatFormatting.GOLD));
        recordStats(server, data);
        data.setDirty();
    }

    private static void finishWithoutWinner(MinecraftServer server, ArsenalSavedData data) {
        data.state = ArsenalMatchState.FINISHED;
        data.players.values().forEach(p -> p.protectionEndGameTime = 0);
        for (UUID id : data.players.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                ArsenalEquipmentService.removeMatchEquipment(player);
                ArsenalNetwork.sync(player, data, data.players.get(id));
            }
        }
        data.setDirty();
    }

    private static void recordStats(MinecraftServer server, ArsenalSavedData data) {
        if (data.statsRecorded || data.winner == null || data.matchId == null) return;
        var stats = SavedDataAccessor.get(server.overworld());
        String matchId = "arsenal_" + data.matchId;
        if (stats.hasProcessedMatch(matchId)) { data.statsRecorded = true; return; }
        long timestamp = System.currentTimeMillis();
        for (ArsenalPlayerData player : data.players.values()) {
            boolean won = player.playerId.equals(data.winner);
            PvpStatsMutationService.importBundle(stats, player.playerId, player.lastKnownName, "gun_game",
                    won ? 1 : 0, won ? 0 : 1, player.kills, player.deaths, timestamp, matchId);
        }
        stats.markProcessedMatch(matchId);
        data.statsRecorded = true;
    }

    private static void broadcast(MinecraftServer server, ArsenalSavedData data, Component message) {
        for (UUID id : data.players.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) player.sendSystemMessage(message);
        }
    }

    private static boolean isAdmin(ServerPlayer player) {
        return player.getTeam() != null && "admin".equals(player.getTeam().getName());
    }

    public record ActionResult(boolean success, String message) {
        public static ActionResult ok(String message) { return new ActionResult(true, message); }
        public static ActionResult error(String message) { return new ActionResult(false, message); }
    }
}
