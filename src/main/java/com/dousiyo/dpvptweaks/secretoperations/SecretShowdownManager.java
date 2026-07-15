package com.dousiyo.dpvptweaks.secretoperations;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.inteldraft.IntelDraftDefinitionLoader;
import com.dousiyo.dpvptweaks.inteldraft.IntelDraftManager;
import com.dousiyo.dpvptweaks.network.OpenSecretOperationsAdminPacket;
import com.dousiyo.dpvptweaks.network.SecretOperationsMatchStatePacket;
import com.dousiyo.dpvptweaks.network.SecretOperationsNetwork;
import com.dousiyo.dpvptweaks.temporarybuilding.TemporaryBuildingLoadout;
import com.dousiyo.dpvptweaks.temporarybuilding.TemporaryBuildingManager;
import com.dousiyo.dpvptweaks.arsenal.ArsenalMatchManager;
import com.dousiyo.dpvptweaks.temporarybuilding.TemporaryBuildingMatchContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Server-authoritative lifecycle for SECRET: SHOWDOWN. */
@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SecretShowdownManager {
    public static final int DEFAULT_DURATION_MINUTES = 10;
    public static final int DEFAULT_DRAFT_INTERVAL_MINUTES = 2;
    private static final int INITIAL_DRAFT_TICKS = 30 * 20;
    private static final int RESPAWN_DELAY_TICKS = 3 * 20;
    private static final int STARTER_GUN_SLOT = 0;
    private static final ResourceLocation STARTER_GUN_ID = new ResourceLocation("tacz", "glock_17");

    private static final Map<UUID, Participant> PARTICIPANTS = new LinkedHashMap<>();
    private static final Map<UUID, TeamSide> PREVIEW = new LinkedHashMap<>();
    private static final Set<UUID> CLEANUP_ON_LOGIN = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static SecretShowdownPhase phase = SecretShowdownPhase.IDLE;
    private static long phaseDeadline;
    private static long nextDraftGrant;
    private static int durationMinutes = DEFAULT_DURATION_MINUTES;
    private static int draftIntervalMinutes = DEFAULT_DRAFT_INTERVAL_MINUTES;
    private static int redScore;
    private static int blueScore;
    private static long lastHudSync;

    private SecretShowdownManager() {}

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        resetMemory();
        SecretOperationsConfig.reload();
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        PARTICIPANTS.clear();
        PREVIEW.clear();
        phase = SecretShowdownPhase.IDLE;
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event) {
        if (phase != SecretShowdownPhase.IDLE) finish(event.getServer(), null, true);
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || phase == SecretShowdownPhase.IDLE) return;
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();

        for (Participant participant : List.copyOf(PARTICIPANTS.values())) {
            ServerPlayer player = server.getPlayerList().getPlayer(participant.id);
            if (player == null) continue;
            if (participant.waiting) holdAtWaiting(server, player, participant);
            if (participant.respawnAt > 0L && now >= participant.respawnAt) beginRespawn(server, player, participant);
            if (participant.personalDraftDeadline > 0L && now >= participant.personalDraftDeadline && !participant.selected) {
                participant.personalDraftDeadline = 0L;
                IntelDraftManager.autoSelectCurrent(player);
            }
            if (participant.dropProtected && landed(player)) {
                participant.dropProtected = false;
                player.setInvulnerable(false);
                syncPlayer(server, player, participant);
            }
        }

        if (phase == SecretShowdownPhase.PREPARING && now >= phaseDeadline) beginActive(server, now);
        if (phase == SecretShowdownPhase.ACTIVE) {
            if (now >= nextDraftGrant) {
                grantDraftTokens(server);
                nextDraftGrant += draftIntervalMinutes * 60L * 20L;
            }
            if (now >= phaseDeadline) {
                if (redScore == blueScore) {
                    phase = SecretShowdownPhase.OVERTIME;
                    broadcast(server, Component.literal("OVERTIME - 次のキルで決着").withStyle(ChatFormatting.GOLD));
                    syncAll(server);
                } else finish(server, redScore > blueScore ? TeamSide.RED : TeamSide.BLUE, false);
            }
        }
        if (phase.running() && now - lastHudSync >= 20L) {
            lastHudSync = now;
            syncAll(server);
        }
    }

    @SubscribeEvent
    public static void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (CLEANUP_ON_LOGIN.remove(player.getUUID())) {
            player.getInventory().clearContent();
            player.setGameMode(GameType.ADVENTURE);
            player.setInvulnerable(false);
            IntelDraftManager.end(player);
        }
        if (phase == SecretShowdownPhase.IDLE) return;
        Participant retained = PARTICIPANTS.get(player.getUUID());
        if (retained != null) {
            applyTeam(player, retained.team);
            player.setGameMode(GameType.ADVENTURE);
            IntelDraftManager.syncOnLogin(player);
            if (retained.waiting) {
                teleportWaiting(player.server, player);
                if ((phase == SecretShowdownPhase.ACTIVE || phase == SecretShowdownPhase.OVERTIME)
                        && retained.purpose == DraftPurpose.INITIAL) {
                    if (retained.selected) launchFromAir(player.server, player, retained);
                    else { retained.purpose = DraftPurpose.LATE_INITIAL; retained.forcedDraft = true; }
                }
                if (retained.waiting && IntelDraftManager.hasSession(player))
                    IntelDraftManager.reopenCurrent(player, !retained.forcedDraft);
            }
            syncPlayer(player.server, player, retained);
            return;
        }
        if (isAdmin(player)) return;
        TeamSide side = smallerTeam();
        Participant participant = prepareNewParticipant(player.server, player, side, DraftPurpose.LATE_INITIAL);
        if (phase == SecretShowdownPhase.PREPARING) {
            participant.personalDraftDeadline = player.server.overworld().getGameTime() + INITIAL_DRAFT_TICKS;
            openDraft(player, participant, false, System.currentTimeMillis() + 30_000L);
        } else openDraft(player, participant, false, 0L);
        syncAll(player.server);
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        ServerPlayer victim = event.getEntity() instanceof ServerPlayer p ? p : null;
        ServerPlayer attacker = attackingPlayer(event.getSource().getEntity());
        Participant victimState = victim == null ? null : PARTICIPANTS.get(victim.getUUID());
        Participant attackerState = attacker == null ? null : PARTICIPANTS.get(attacker.getUUID());
        if (victimState != null && (victimState.waiting || victimState.dropProtected)) event.setCanceled(true);
        if (attackerState != null && (attackerState.waiting || attackerState.dropProtected)) event.setCanceled(true);
        if (victimState != null && attackerState != null && victimState.team == attackerState.team) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        Participant victimState = PARTICIPANTS.get(victim.getUUID());
        if (victimState == null || !phase.running()) return;
        event.setCanceled(true);
        victim.setHealth(Math.max(1.0F, victim.getMaxHealth()));
        victim.getFoodData().setFoodLevel(20);
        victim.clearFire();

        ServerPlayer attacker = attackingPlayer(event.getSource().getEntity());
        Participant attackerState = attacker == null ? null : PARTICIPANTS.get(attacker.getUUID());
        boolean validKill = attackerState != null && attacker != victim && attackerState.team != victimState.team;
        if (validKill) {
            if (attackerState.team == TeamSide.RED) redScore++; else blueScore++;
            IntelDraftManager.grantEliminationAmmo(attacker);
            if (phase == SecretShowdownPhase.OVERTIME) {
                finish(victim.server, attackerState.team, false);
                return;
            }
        }

        victimState.waiting = true;
        victimState.forcedDraft = false;
        victimState.respawnAt = victim.server.overworld().getGameTime() + RESPAWN_DELAY_TICKS;
        victimState.dropProtected = false;
        victim.setInvulnerable(true);
        teleportWaiting(victim.server, victim);
        syncAll(victim.server);
    }

    public static boolean isParticipant(ServerPlayer player) { return PARTICIPANTS.containsKey(player.getUUID()); }
    public static boolean activeMatch() { return phase.running(); }
    public static SecretShowdownPhase phase() { return phase; }
    public static boolean canBuild(ServerPlayer player) {
        Participant participant = PARTICIPANTS.get(player.getUUID());
        return participant != null && (phase == SecretShowdownPhase.ACTIVE || phase == SecretShowdownPhase.OVERTIME)
                && !participant.waiting && !participant.dropProtected;
    }

    public static ActionResult randomize(MinecraftServer server) {
        if (phase != SecretShowdownPhase.IDLE) return ActionResult.error("試合中は編成できません");
        List<ServerPlayer> eligible = eligible(server);
        if (eligible.size() < 2) return ActionResult.error("参加者が2人以上必要です");
        Collections.shuffle(eligible);
        PREVIEW.clear();
        boolean redGetsExtra = ThreadLocalRandom.current().nextBoolean();
        int redTarget = eligible.size() / 2 + (eligible.size() % 2 == 1 && redGetsExtra ? 1 : 0);
        ensureTeams(server);
        for (int i = 0; i < eligible.size(); i++) {
            TeamSide side = i < redTarget ? TeamSide.RED : TeamSide.BLUE;
            PREVIEW.put(eligible.get(i).getUUID(), side);
            applyTeam(eligible.get(i), side);
        }
        return ActionResult.ok("チームをランダム編成しました");
    }

    public static ActionResult start(MinecraftServer server, int requestedDuration, int requestedDraftInterval) {
        if (phase != SecretShowdownPhase.IDLE) return ActionResult.error("すでに試合が進行中です");
        if (SecretConvoyManager.activeMatch()) return ActionResult.error("SECRET CONVOYの試合中です");
        if (ArsenalMatchManager.activeMatch(server)) return ActionResult.error("アーセナルの試合中です");
        if (!TemporaryBuildingManager.canStartMatch(server)) return ActionResult.error("前試合の仮設ブロックをリセット中です");
        if (requestedDuration < 1 || requestedDuration > 60) return ActionResult.error("試合時間は1～60分です");
        if (requestedDraftInterval < 1 || requestedDraftInterval > 10) return ActionResult.error("ドラフト間隔は1～10分です");
        SecretOperationsConfig.Validation validation = SecretOperationsConfig.validate(server);
        if (!validation.valid()) return ActionResult.error(validation.error());
        if (starterGunStack().isEmpty()) return ActionResult.error("TACZのGlock 17 (tacz:glock_17) が見つかりません");
        List<ServerPlayer> eligible = eligible(server);
        if (eligible.size() < 2) return ActionResult.error("参加者が2人以上必要です");
        Set<UUID> ids = eligible.stream().map(ServerPlayer::getUUID).collect(java.util.stream.Collectors.toSet());
        if (!PREVIEW.isEmpty() && !PREVIEW.keySet().equals(ids)) return ActionResult.error("参加者が変化しました。再編成してください");
        if (PREVIEW.isEmpty()) {
            ActionResult result = randomize(server);
            if (!result.success) return result;
        }
        long red = PREVIEW.values().stream().filter(t -> t == TeamSide.RED).count();
        long blue = PREVIEW.size() - red;
        if (red == 0 || blue == 0) return ActionResult.error("REDとBLUEに最低1人ずつ必要です");

        SecretOperationsConfig.AirSpawn buildingArea = validation.air();
        ServerLevel buildingLevel = SecretOperationsConfig.airLevel(server, validation);
        TemporaryBuildingMatchContext buildingContext = new TemporaryBuildingMatchContext(UUID.randomUUID(),
                "secret_showdown", buildingLevel.dimension(), (int)Math.floor(buildingArea.minX),
                (int)Math.ceil(buildingArea.maxX), (int)Math.floor(buildingArea.minZ), (int)Math.ceil(buildingArea.maxZ));
        if (!TemporaryBuildingManager.beginMatch(server, buildingContext, SecretShowdownManager::canBuild))
            return ActionResult.error("仮設ブロック管理を開始できませんでした");

        durationMinutes = requestedDuration;
        draftIntervalMinutes = requestedDraftInterval;
        redScore = blueScore = 0;
        PARTICIPANTS.clear();
        phase = SecretShowdownPhase.PREPARING;
        long now = server.overworld().getGameTime();
        phaseDeadline = now + INITIAL_DRAFT_TICKS;
        for (ServerPlayer player : eligible) {
            Participant participant = prepareNewParticipant(server, player, PREVIEW.get(player.getUUID()), DraftPurpose.INITIAL);
            openDraft(player, participant, false, System.currentTimeMillis() + 30_000L);
        }
        broadcast(server, Component.literal("SECRET: SHOWDOWN - 技術選択 30秒").withStyle(ChatFormatting.GOLD));
        syncAll(server);
        return ActionResult.ok("SECRET: SHOWDOWNの準備を開始しました");
    }

    public static ActionResult stop(MinecraftServer server) {
        if (phase == SecretShowdownPhase.IDLE) return ActionResult.error("試合は進行していません");
        finish(server, null, true);
        return ActionResult.ok("試合を中止しました");
    }

    public static ActionResult reload(MinecraftServer server) {
        SecretOperationsConfig.reload();
        String error = SecretOperationsConfig.error(server);
        return error == null ? ActionResult.ok("設定を再読み込みしました") : ActionResult.error(error);
    }

    public static void openAdmin(ServerPlayer player, String notice) {
        MinecraftServer server = player.server;
        List<String> red = new ArrayList<>();
        List<String> blue = new ArrayList<>();
        Map<UUID, TeamSide> source = phase == SecretShowdownPhase.IDLE ? PREVIEW : participantTeams();
        for (Map.Entry<UUID, TeamSide> entry : source.entrySet()) {
            var profile = server.getProfileCache().get(entry.getKey());
            String name = profile.map(p -> p.getName()).orElse(entry.getKey().toString().substring(0, 8));
            (entry.getValue() == TeamSide.RED ? red : blue).add(name);
        }
        Collections.sort(red); Collections.sort(blue);
        String error = SecretOperationsConfig.error(server);
        SecretOperationsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenSecretOperationsAdminPacket(SecretOperationMode.SHOWDOWN, phase.name(), 0, durationMinutes, draftIntervalMinutes,
                        source.size(), redScore, blueScore, error == null ? "" : error,
                        notice == null ? "" : notice, red, blue));
    }

    public static void openPendingDraft(ServerPlayer player) {
        Participant participant = PARTICIPANTS.get(player.getUUID());
        if (participant == null || !phase.running() || participant.waiting || participant.pendingTokens <= 0) return;
        if (IntelDraftManager.hasSession(player)) {
            IntelDraftManager.reopenCurrent(player, true);
            return;
        }
        participant.purpose = DraftPurpose.OPTIONAL;
        participant.sessionReserved = true;
        IntelDraftManager.openMatch(player, true);
        syncPlayer(player.server, player, participant);
    }

    /** Called only after IntelDraftManager has atomically consumed a valid session. */
    public static void onDraftSelected(ServerPlayer player) {
        Participant participant = PARTICIPANTS.get(player.getUUID());
        if (participant == null) return;
        participant.selected = true;
        switch (participant.purpose) {
            case INITIAL -> { /* initial roster always waits for the shared deadline */ }
            case LATE_INITIAL -> {
                participant.personalDraftDeadline = 0L;
                if (phase == SecretShowdownPhase.ACTIVE || phase == SecretShowdownPhase.OVERTIME) launchFromAir(player.server, player, participant);
            }
            case OPTIONAL -> consumeReserved(participant);
            case RESPAWN -> {
                consumeReserved(participant);
                participant.respawnDraftRemaining = Math.max(0, participant.respawnDraftRemaining - 1);
                if (participant.respawnDraftRemaining > 0) {
                    participant.sessionReserved = true;
                    IntelDraftManager.openMatch(player, false);
                } else launchFromAir(player.server, player, participant);
            }
        }
        syncPlayer(player.server, player, participant);
    }

    private static void beginActive(MinecraftServer server, long now) {
        for (Participant participant : PARTICIPANTS.values()) {
            if (participant.purpose != DraftPurpose.INITIAL) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(participant.id);
            if (player != null && !participant.selected) IntelDraftManager.autoSelectCurrent(player);
        }
        phase = SecretShowdownPhase.ACTIVE;
        phaseDeadline = now + durationMinutes * 60L * 20L;
        nextDraftGrant = now + draftIntervalMinutes * 60L * 20L;
        for (Participant participant : PARTICIPANTS.values()) {
            ServerPlayer player = server.getPlayerList().getPlayer(participant.id);
            if (player != null && participant.selected && (participant.purpose == DraftPurpose.INITIAL
                    || participant.purpose == DraftPurpose.LATE_INITIAL))
                launchFromAir(server, player, participant);
        }
        broadcast(server, Component.literal("SECRET: SHOWDOWN START").withStyle(ChatFormatting.GOLD));
        syncAll(server);
    }

    private static void beginRespawn(MinecraftServer server, ServerPlayer player, Participant participant) {
        participant.respawnAt = 0L;
        IntelDraftManager.grantRespawnAmmo(player);
        participant.respawnDraftRemaining = participant.pendingTokens;
        if (participant.respawnDraftRemaining <= 0) {
            launchFromAir(server, player, participant);
            return;
        }
        participant.forcedDraft = true;
        participant.purpose = DraftPurpose.RESPAWN;
        if (IntelDraftManager.hasSession(player)) {
            participant.sessionReserved = true;
            IntelDraftManager.reopenCurrent(player, false);
        } else {
            participant.sessionReserved = true;
            IntelDraftManager.openMatch(player, false);
        }
    }

    private static void launchFromAir(MinecraftServer server, ServerPlayer player, Participant participant) {
        SecretOperationsConfig.Validation validation = SecretOperationsConfig.validate(server);
        if (!validation.valid()) {
            player.sendSystemMessage(Component.literal("空中スポーン失敗: " + validation.error()).withStyle(ChatFormatting.RED));
            return;
        }
        SecretOperationsConfig.AirSpawn air = validation.air();
        double x = ThreadLocalRandom.current().nextDouble(air.minX, air.maxX);
        double z = ThreadLocalRandom.current().nextDouble(air.minZ, air.maxZ);
        ServerLevel level = SecretOperationsConfig.airLevel(server, validation);
        player.teleportTo(level, x, air.y, z, air.yaw, air.pitch);
        player.setDeltaMovement(0, 0, 0);
        player.setOnGround(false);
        participant.waiting = false;
        participant.forcedDraft = false;
        participant.dropProtected = true;
        participant.respawnAt = 0L;
        participant.respawnDraftRemaining = 0;
        player.setInvulnerable(true);
    }

    private static Participant prepareNewParticipant(MinecraftServer server, ServerPlayer player, TeamSide side, DraftPurpose purpose) {
        ensureTeams(server);
        applyTeam(player, side);
        player.setGameMode(GameType.ADVENTURE);
        player.getInventory().clearContent();
        grantStarterGun(player);
        TemporaryBuildingLoadout.grantInitial(player);
        player.containerMenu.broadcastChanges();
        Participant participant = new Participant(player.getUUID(), side);
        participant.waiting = true;
        participant.purpose = purpose;
        PARTICIPANTS.put(player.getUUID(), participant);
        player.setInvulnerable(true);
        teleportWaiting(server, player);
        return participant;
    }

    private static ItemStack starterGunStack() {
        return IntelDraftDefinitionLoader.loadedGunStack(STARTER_GUN_ID, 1);
    }

    private static void grantStarterGun(ServerPlayer player) {
        ItemStack stack = starterGunStack();
        if (stack.isEmpty()) return;
        if (player.getInventory().getItem(STARTER_GUN_SLOT).isEmpty()) {
            player.getInventory().setItem(STARTER_GUN_SLOT, stack);
        } else if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void openDraft(ServerPlayer player, Participant participant, boolean closeAllowed, long expiresAtMillis) {
        participant.sessionReserved = false;
        IntelDraftManager.openMatch(player, closeAllowed, expiresAtMillis);
    }

    private static void holdAtWaiting(MinecraftServer server, ServerPlayer player, Participant participant) {
        player.setDeltaMovement(0, 0, 0);
        player.fallDistance = 0;
        player.setInvulnerable(true);
        SecretOperationsConfig.Validation validation = SecretOperationsConfig.validate(server);
        if (!validation.valid()) return;
        SecretOperationsConfig.SpawnPoint point = validation.waiting();
        ServerLevel level = SecretOperationsConfig.waitingLevel(server, validation);
        if (player.level() != level || player.distanceToSqr(point.x, point.y, point.z) > 0.04)
            player.teleportTo(level, point.x, point.y, point.z, point.yaw, point.pitch);
    }

    private static void teleportWaiting(MinecraftServer server, ServerPlayer player) {
        SecretOperationsConfig.Validation validation = SecretOperationsConfig.validate(server);
        if (!validation.valid()) return;
        SecretOperationsConfig.SpawnPoint point = validation.waiting();
        player.teleportTo(SecretOperationsConfig.waitingLevel(server, validation), point.x, point.y, point.z, point.yaw, point.pitch);
        player.setDeltaMovement(0, 0, 0);
    }

    private static boolean landed(ServerPlayer player) {
        return player.onGround() || player.isInWater() || player.onClimbable();
    }

    private static void grantDraftTokens(MinecraftServer server) {
        for (Participant participant : PARTICIPANTS.values()) participant.pendingTokens++;
        broadcast(server, Component.literal("SECRET TECH DRAFTを獲得しました [I]").withStyle(ChatFormatting.AQUA));
        syncAll(server);
    }

    private static void consumeReserved(Participant participant) {
        if (participant.sessionReserved && participant.pendingTokens > 0) participant.pendingTokens--;
        participant.sessionReserved = false;
    }

    private static void finish(MinecraftServer server, TeamSide winner, boolean canceled) {
        phase = SecretShowdownPhase.ENDING;
        TemporaryBuildingManager.endMatch(server);
        if (!canceled && winner != null) {
            Component title = Component.literal((winner == TeamSide.RED ? "RED" : "BLUE") + " VICTORY")
                    .withStyle(winner == TeamSide.RED ? ChatFormatting.RED : ChatFormatting.BLUE);
            for (ServerPlayer player : server.getPlayerList().getPlayers())
                if (PARTICIPANTS.containsKey(player.getUUID())) player.connection.send(new ClientboundSetTitleTextPacket(title));
            broadcast(server, Component.literal("最終得点 RED " + redScore + " - " + blueScore + " BLUE"));
        } else broadcast(server, Component.literal("SECRET: SHOWDOWNを中止しました").withStyle(ChatFormatting.YELLOW));

        List<ServerPlayer> onlineParticipants = new ArrayList<>();
        for (Participant participant : List.copyOf(PARTICIPANTS.values())) {
            ServerPlayer player = server.getPlayerList().getPlayer(participant.id);
            if (player == null) { CLEANUP_ON_LOGIN.add(participant.id); continue; }
            onlineParticipants.add(player);
            player.setInvulnerable(false);
            player.getInventory().clearContent();
            player.containerMenu.broadcastChanges();
            player.setGameMode(GameType.ADVENTURE);
            IntelDraftManager.end(player);
        }
        IntelDraftManager.endAll(server);
        PARTICIPANTS.clear();
        PREVIEW.clear();
        phase = SecretShowdownPhase.IDLE;
        phaseDeadline = nextDraftGrant = 0L;
        onlineParticipants.forEach(SecretShowdownManager::syncInactive);
    }

    private static void syncAll(MinecraftServer server) {
        for (Participant participant : PARTICIPANTS.values()) {
            ServerPlayer player = server.getPlayerList().getPlayer(participant.id);
            if (player != null) syncPlayer(server, player, participant);
        }
    }

    private static void syncPlayer(MinecraftServer server, ServerPlayer player, Participant participant) {
        long now = server.overworld().getGameTime();
        long remaining = phase == SecretShowdownPhase.PREPARING || phase == SecretShowdownPhase.ACTIVE
                ? Math.max(0L, phaseDeadline - now) : 0L;
        SecretOperationsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SecretOperationsMatchStatePacket(true, phase, redScore, blueScore, remaining,
                        participant.pendingTokens, participant.waiting, participant.dropProtected));
        SecretOperationsManager.sync(player);
    }

    private static void syncInactive(ServerPlayer player) {
        SecretOperationsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SecretOperationsMatchStatePacket(false, SecretShowdownPhase.IDLE, 0, 0, 0, 0, false, false));
        SecretOperationsManager.sync(player);
    }

    private static List<ServerPlayer> eligible(MinecraftServer server) {
        return server.getPlayerList().getPlayers().stream().filter(p -> !isAdmin(p)).toList();
    }

    private static boolean isAdmin(ServerPlayer player) {
        return player.getTeam() != null && "admin".equals(player.getTeam().getName());
    }

    private static void ensureTeams(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam red = scoreboard.getPlayerTeam("red");
        if (red == null) red = scoreboard.addPlayerTeam("red");
        red.setColor(ChatFormatting.RED); red.setAllowFriendlyFire(false);
        PlayerTeam blue = scoreboard.getPlayerTeam("blue");
        if (blue == null) blue = scoreboard.addPlayerTeam("blue");
        blue.setColor(ChatFormatting.BLUE); blue.setAllowFriendlyFire(false);
    }

    private static void applyTeam(ServerPlayer player, TeamSide side) {
        Scoreboard scoreboard = player.server.getScoreboard();
        ensureTeams(player.server);
        scoreboard.addPlayerToTeam(player.getScoreboardName(), scoreboard.getPlayerTeam(side.id));
    }

    private static TeamSide smallerTeam() {
        long red = PARTICIPANTS.values().stream().filter(p -> p.team == TeamSide.RED).count();
        long blue = PARTICIPANTS.size() - red;
        if (red == blue) return ThreadLocalRandom.current().nextBoolean() ? TeamSide.RED : TeamSide.BLUE;
        return red < blue ? TeamSide.RED : TeamSide.BLUE;
    }

    private static ServerPlayer attackingPlayer(Entity entity) {
        if (entity instanceof ServerPlayer player) return player;
        if (entity instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer owner) return owner;
        return null;
    }

    private static void broadcast(MinecraftServer server, Component message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers())
            if (PARTICIPANTS.containsKey(player.getUUID())) player.sendSystemMessage(message);
    }

    private static Map<UUID, TeamSide> participantTeams() {
        Map<UUID, TeamSide> result = new LinkedHashMap<>();
        PARTICIPANTS.forEach((id, value) -> result.put(id, value.team));
        return result;
    }

    private static void resetMemory() {
        PARTICIPANTS.clear(); PREVIEW.clear(); CLEANUP_ON_LOGIN.clear(); phase = SecretShowdownPhase.IDLE;
        durationMinutes = DEFAULT_DURATION_MINUTES; draftIntervalMinutes = DEFAULT_DRAFT_INTERVAL_MINUTES;
        redScore = blueScore = 0; phaseDeadline = nextDraftGrant = 0L;
    }

    private enum TeamSide {
        RED("red"), BLUE("blue");
        final String id; TeamSide(String id) { this.id = id; }
    }
    private enum DraftPurpose { INITIAL, LATE_INITIAL, OPTIONAL, RESPAWN }

    private static final class Participant {
        final UUID id; final TeamSide team;
        DraftPurpose purpose = DraftPurpose.INITIAL;
        boolean waiting; boolean selected; boolean forcedDraft; boolean dropProtected; boolean sessionReserved;
        int pendingTokens; int respawnDraftRemaining;
        long personalDraftDeadline; long respawnAt;
        Participant(UUID id, TeamSide team) { this.id = id; this.team = team; }
    }

    public record ActionResult(boolean success, String message) {
        public static ActionResult ok(String message) { return new ActionResult(true, message); }
        public static ActionResult error(String message) { return new ActionResult(false, message); }
    }
}
