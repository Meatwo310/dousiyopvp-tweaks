package com.dousiyo.dpvptweaks.arsenal;

import com.dousiyo.dpvptweaks.network.arsenal.ArsenalNetwork;
import com.dousiyo.dpvptweaks.network.arsenal.OpenArsenalAdminPacket;
import com.dousiyo.dpvptweaks.pvpstats.service.PvpStatsMutationService;
import com.dousiyo.dpvptweaks.pvpstats.util.SavedDataAccessor;
import com.dousiyo.dpvptweaks.secretoperations.SecretShowdownManager;
import com.dousiyo.dpvptweaks.secretoperations.SecretConvoyManager;
import com.dousiyo.dpvptweaks.secretoperations.SecretOperationsParachuteManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraftforge.network.PacketDistributor;

public final class ArsenalMatchManager {
    public static final int PROTECTION_TICKS = 100;
    public static final int START_COUNTDOWN_TICKS = 100;
    private static final String ARSENAL_TEAM = "arsenal";
    private static final String RANKING_OBJECTIVE = "arsenal_stage";
    private static final UUID HEALTH_MODIFIER_ID = UUID.fromString("dfb08073-52dd-42b6-ae14-f9f68f3a5298");
    private static final double ARSENAL_MAX_HEALTH = 60.0D;
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
        String parachuteError = SecretOperationsParachuteManager.validationError(participants);
        if (parachuteError != null) return ActionResult.error(parachuteError);

        Objective previousSidebar = server.getScoreboard().getDisplayObjective(Scoreboard.DISPLAY_SLOT_SIDEBAR);
        String previousSidebarName = previousSidebar == null || RANKING_OBJECTIVE.equals(previousSidebar.getName())
                ? "" : previousSidebar.getName();
        data.clearMatch();
        data.state = ArsenalMatchState.RUNNING;
        data.matchId = UUID.randomUUID();
        data.weaponSetId = set.id();
        data.snapshot = set;
        data.previousSidebarObjective = previousSidebarName;
        data.countdownEndGameTime = server.overworld().getGameTime() + START_COUNTDOWN_TICKS;
        ensureArsenalTeam(server);
        for (ServerPlayer player : participants) {
            ArsenalPlayerData participant = new ArsenalPlayerData(player.getUUID(), player.getGameProfile().getName());
            participant.protectionEndGameTime = data.countdownEndGameTime;
            data.players.put(player.getUUID(), participant);
            player.getInventory().clearContent();
            applyArsenalTeam(player);
            player.setGameMode(GameType.ADVENTURE);
            applyArsenalHealth(player, true);
            if (!SecretOperationsParachuteManager.equip(player)) {
                rollbackStart(participants);
                data.clearMatch();
                return ActionResult.error("パラシュートを装備できません: " + player.getGameProfile().getName());
            }
            teleportToAirSpawn(player, config);
            if (!ArsenalEquipmentService.giveStage(player, data.matchId, 0, set.stages().get(0))) {
                rollbackStart(participants);
                data.clearMatch();
                return ActionResult.error("インベントリに第1段階の装備を支給できません: " + player.getGameProfile().getName());
            }
        }
        createRankingScoreboard(server, data);
        data.setDirty();
        syncViewers(server, data);
        sendTitle(server, data, Component.literal("5").withStyle(ChatFormatting.GOLD));
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
        PlayerTeam arsenalTeam = server.getScoreboard().getPlayerTeam(ARSENAL_TEAM);
        for (UUID id : data.players.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                ArsenalEquipmentService.removeMatchEquipment(player);
                removeArsenalHealth(player);
                removeArsenalTeam(player);
                SecretOperationsParachuteManager.restore(player);
            }
            else if (arsenalTeam != null) {
                ArsenalPlayerData participant = data.players.get(id);
                if (arsenalTeam.getPlayers().contains(participant.lastKnownName))
                    server.getScoreboard().removePlayerFromTeam(participant.lastKnownName, arsenalTeam);
            }
        }
        restorePreviousScoreboard(server, data);
        data.clearMatch();
        LAST_DEATH_TICKS.clear();
        SecretOperationsParachuteManager.restoreAll(server);
        server.getPlayerList().getPlayers().forEach(ArsenalNetwork::syncInactive);
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
        if (data.state != ArsenalMatchState.RUNNING) SecretOperationsParachuteManager.restore(player);
        ArsenalPlayerData participant = data.players.get(player.getUUID());
        if (data.state == ArsenalMatchState.RUNNING) {
            if (participant == null && !isAdmin(player)) {
                participant = new ArsenalPlayerData(player.getUUID(), player.getGameProfile().getName());
                data.players.put(player.getUUID(), participant);
                data.setDirty();
            }
            if (participant != null) {
                participant.lastKnownName = player.getGameProfile().getName();
                applyArsenalTeam(player);
                player.setGameMode(GameType.ADVENTURE);
                applyArsenalHealth(player, false);
                if (!SecretOperationsParachuteManager.equip(player))
                    player.sendSystemMessage(Component.literal("アーセナル用パラシュートを装備できませんでした")
                            .withStyle(ChatFormatting.RED));
                ArsenalEquipmentService.giveStage(player, data.matchId, participant.stage, data.snapshot.stages().get(participant.stage));
                ArsenalNetwork.sync(player, data, participant);
                updateRankingScoreboard(player.server, data);
                data.setDirty();
                return;
            }
            if (isAdmin(player)) {
                ArsenalNetwork.syncObserver(player, data);
                return;
            }
        } else if (participant != null) {
            ArsenalNetwork.sync(player, data, participant);
            return;
        } else if (data.state == ArsenalMatchState.FINISHED && isAdmin(player)) {
            ArsenalNetwork.syncObserver(player, data);
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
        if (data.countdownEndGameTime > tick) return;
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
            updateRankingScoreboard(victim.server, data);
            syncViewers(victim.server, data);
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
        teleportToAirSpawn(player, validation);
        player.setGameMode(GameType.ADVENTURE);
        applyArsenalHealth(player, true);
        if (!SecretOperationsParachuteManager.equip(player))
            player.sendSystemMessage(Component.literal("アーセナル用パラシュートを装備できませんでした")
                    .withStyle(ChatFormatting.RED));
        participant.protectionEndGameTime = Math.max(data.countdownEndGameTime,
                player.server.overworld().getGameTime() + PROTECTION_TICKS);
        ArsenalEquipmentService.giveStage(player, data.matchId, participant.stage, data.snapshot.stages().get(participant.stage));
        data.setDirty();
        ArsenalNetwork.sync(player, data, participant);
    }

    public static void serverTick(MinecraftServer server) {
        ArsenalSavedData data = ArsenalSavedData.get(server);
        if (data.state != ArsenalMatchState.RUNNING) return;
        long now = server.overworld().getGameTime();
        if (data.countdownEndGameTime > 0L) {
            long remaining = data.countdownEndGameTime - now;
            if (remaining <= 0L) {
                data.countdownEndGameTime = 0L;
                data.players.values().forEach(participant -> participant.protectionEndGameTime = 0L);
                data.setDirty();
                sendTitle(server, data, Component.literal("FIGHT!").withStyle(ChatFormatting.RED));
                syncViewers(server, data);
            } else {
                if (remaining % 20L == 0L)
                    sendTitle(server, data, Component.literal(Long.toString((remaining + 19L) / 20L)).withStyle(ChatFormatting.GOLD));
                syncViewers(server, data);
            }
        }
        for (ArsenalPlayerData participant : data.players.values()) {
            if (participant.protectionEndGameTime > 0 && participant.protectionEndGameTime <= now) {
                participant.protectionEndGameTime = 0;
                ServerPlayer player = server.getPlayerList().getPlayer(participant.playerId);
                if (player != null) ArsenalNetwork.sync(player, data, participant);
                data.setDirty();
            }
        }
        if (now % 20L == 0L) {
            updateRankingScoreboard(server, data);
            syncViewers(server, data);
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
        data.countdownEndGameTime = 0L;
        data.winner = winner.getUUID();
        data.players.values().forEach(p -> p.protectionEndGameTime = 0);
        Component title = Component.literal("VICTORY").withStyle(ChatFormatting.GOLD);
        Component subtitle = Component.literal(winner.getGameProfile().getName()).withStyle(ChatFormatting.YELLOW);
        for (UUID id : data.players.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
                player.connection.send(new ClientboundSetTitleTextPacket(title));
                ArsenalEquipmentService.removeMatchEquipment(player);
                removeArsenalHealth(player);
                SecretOperationsParachuteManager.restore(player);
                ArsenalNetwork.sync(player, data, data.players.get(id));
            }
        }
        broadcast(server, data, Component.literal("勝者: " + winner.getGameProfile().getName()).withStyle(ChatFormatting.GOLD));
        recordStats(server, data);
        data.setDirty();
        syncViewers(server, data);
    }

    private static void finishWithoutWinner(MinecraftServer server, ArsenalSavedData data) {
        data.state = ArsenalMatchState.FINISHED;
        data.countdownEndGameTime = 0L;
        data.players.values().forEach(p -> p.protectionEndGameTime = 0);
        for (UUID id : data.players.keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                ArsenalEquipmentService.removeMatchEquipment(player);
                removeArsenalHealth(player);
                SecretOperationsParachuteManager.restore(player);
                ArsenalNetwork.sync(player, data, data.players.get(id));
            }
        }
        data.setDirty();
        syncViewers(server, data);
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

    private static void rollbackStart(Iterable<ServerPlayer> participants) {
        for (ServerPlayer player : participants) {
            ArsenalEquipmentService.removeMatchEquipment(player);
            removeArsenalHealth(player);
            removeArsenalTeam(player);
            SecretOperationsParachuteManager.restore(player);
            ArsenalNetwork.syncInactive(player);
        }
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

    private static void teleportToAirSpawn(ServerPlayer player, ArsenalConfig.Validation validation) {
        ArsenalConfig.AirSpawn spawn = validation.spawn();
        double x = ThreadLocalRandom.current().nextDouble(spawn.minX, spawn.maxX);
        double z = ThreadLocalRandom.current().nextDouble(spawn.minZ, spawn.maxZ);
        player.teleportTo(validation.level(), x, spawn.y, z, spawn.yaw, spawn.pitch);
        player.setDeltaMovement(Vec3.ZERO);
    }

    private static void ensureArsenalTeam(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(ARSENAL_TEAM);
        if (team == null) team = scoreboard.addPlayerTeam(ARSENAL_TEAM);
        team.setColor(ChatFormatting.GOLD);
        team.setAllowFriendlyFire(true);
    }

    private static void applyArsenalTeam(ServerPlayer player) {
        ensureArsenalTeam(player.server);
        player.server.getScoreboard().addPlayerToTeam(player.getScoreboardName(),
                player.server.getScoreboard().getPlayerTeam(ARSENAL_TEAM));
    }

    private static void removeArsenalTeam(ServerPlayer player) {
        PlayerTeam team = player.server.getScoreboard().getPlayerTeam(ARSENAL_TEAM);
        if (team != null && player.getTeam() == team)
            player.server.getScoreboard().removePlayerFromTeam(player.getScoreboardName(), team);
    }

    private static void syncViewers(MinecraftServer server, ArsenalSavedData data) {
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            ArsenalPlayerData participant = data.players.get(viewer.getUUID());
            if (participant != null) ArsenalNetwork.sync(viewer, data, participant);
            else if (isAdmin(viewer)) ArsenalNetwork.syncObserver(viewer, data);
        }
    }

    private static void sendTitle(MinecraftServer server, ArsenalSavedData data, Component title) {
        for (ServerPlayer viewer : server.getPlayerList().getPlayers())
            if (data.players.containsKey(viewer.getUUID()) || isAdmin(viewer))
                viewer.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    private static void createRankingScoreboard(MinecraftServer server, ArsenalSavedData data) {
        Scoreboard scoreboard = server.getScoreboard();
        Objective existing = scoreboard.getObjective(RANKING_OBJECTIVE);
        if (existing != null) scoreboard.removeObjective(existing);
        Objective objective = scoreboard.addObjective(RANKING_OBJECTIVE, ObjectiveCriteria.DUMMY,
                Component.literal("ARSENAL 段階"), ObjectiveCriteria.RenderType.INTEGER);
        scoreboard.setDisplayObjective(Scoreboard.DISPLAY_SLOT_SIDEBAR, objective);
        updateRankingScoreboard(server, data);
    }

    private static void updateRankingScoreboard(MinecraftServer server, ArsenalSavedData data) {
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(RANKING_OBJECTIVE);
        if (objective == null) {
            objective = scoreboard.addObjective(RANKING_OBJECTIVE, ObjectiveCriteria.DUMMY,
                    Component.literal("ARSENAL 段階"), ObjectiveCriteria.RenderType.INTEGER);
            scoreboard.setDisplayObjective(Scoreboard.DISPLAY_SLOT_SIDEBAR, objective);
        }
        for (ArsenalPlayerData participant : data.players.values()) {
            ServerPlayer online = server.getPlayerList().getPlayer(participant.playerId);
            if (online == null) {
                scoreboard.resetPlayerScore(participant.lastKnownName, objective);
                continue;
            }
            int stage = participant.stage + 1;
            var score = scoreboard.getOrCreatePlayerScore(participant.lastKnownName, objective);
            if (score.getScore() != stage) score.setScore(stage);
        }
    }

    private static void restorePreviousScoreboard(MinecraftServer server, ArsenalSavedData data) {
        Scoreboard scoreboard = server.getScoreboard();
        Objective arsenal = scoreboard.getObjective(RANKING_OBJECTIVE);
        if (arsenal == null) return;
        Objective previous = data.previousSidebarObjective.isBlank()
                ? null : scoreboard.getObjective(data.previousSidebarObjective);
        if (scoreboard.getDisplayObjective(Scoreboard.DISPLAY_SLOT_SIDEBAR) == arsenal)
            scoreboard.setDisplayObjective(Scoreboard.DISPLAY_SLOT_SIDEBAR, previous);
        scoreboard.removeObjective(arsenal);
    }

    private static void applyArsenalHealth(ServerPlayer player, boolean healFully) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) return;
        AttributeModifier old = health.getModifier(HEALTH_MODIFIER_ID);
        if (old != null) health.removeModifier(old);
        double amount = ARSENAL_MAX_HEALTH - health.getValue();
        health.addTransientModifier(new AttributeModifier(HEALTH_MODIFIER_ID, "Arsenal max health", amount,
                AttributeModifier.Operation.ADDITION));
        if (healFully) player.setHealth(player.getMaxHealth());
    }

    private static void removeArsenalHealth(ServerPlayer player) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) return;
        AttributeModifier modifier = health.getModifier(HEALTH_MODIFIER_ID);
        if (modifier != null) health.removeModifier(modifier);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    public record ActionResult(boolean success, String message) {
        public static ActionResult ok(String message) { return new ActionResult(true, message); }
        public static ActionResult error(String message) { return new ActionResult(false, message); }
    }
}
