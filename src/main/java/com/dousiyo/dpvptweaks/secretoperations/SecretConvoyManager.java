package com.dousiyo.dpvptweaks.secretoperations;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.arsenal.ArsenalMatchManager;
import com.dousiyo.dpvptweaks.entity.ModEntities;
import com.dousiyo.dpvptweaks.entity.SecretConvoyTruckEntity;
import com.dousiyo.dpvptweaks.inteldraft.IntelDraftManager;
import com.dousiyo.dpvptweaks.temporarybuilding.TemporaryBuildingLoadout;
import com.dousiyo.dpvptweaks.temporarybuilding.TemporaryBuildingManager;
import com.dousiyo.dpvptweaks.temporarybuilding.TemporaryBuildingMatchContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.dousiyo.dpvptweaks.network.secretoperations.OpenSecretOperationsAdminPacket;
import com.dousiyo.dpvptweaks.network.secretoperations.SecretOperationsNetwork;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Complete server-authoritative lifecycle for SECRET: CONVOY. */
@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SecretConvoyManager {
    public static final int DEFAULT_ROUND_MINUTES = 10;
    public static final int DEFAULT_DRAFT_INTERVAL_MINUTES = 2;
    private static final int PREPARE_TICKS = 30 * 20;
    private static final int RESPAWN_TICKS = 3 * 20;
    private static final double WIN_EPSILON = 1.0E-4D;

    private static final Map<UUID, Participant> PARTICIPANTS = new LinkedHashMap<>();
    private static final Map<UUID, TeamSide> PREVIEW = new LinkedHashMap<>();
    private static final Set<Long> FORCED_CHUNKS = new LinkedHashSet<>();
    private static SecretConvoyPhase phase = SecretConvoyPhase.IDLE;
    private static SecretOperationsConfig.Convoy config;
    private static ServerLevel level;
    private static SecretConvoyTruckEntity truck;
    private static UUID matchId;
    private static int round = 0;
    private static int roundMinutes = DEFAULT_ROUND_MINUTES;
    private static int draftIntervalMinutes = DEFAULT_DRAFT_INTERVAL_MINUTES;
    private static long phaseDeadline, roundStartedAt, nextDraftAt, overtimeLostAt, lastHudSync;
    private static double totalDistance, travelledDistance;
    private static FirstRoundRecord firstRecord;
    private static int nearbyEscorts;
    private static boolean enemyBlocking;

    private SecretConvoyManager() {}

    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || phase == SecretConvoyPhase.IDLE) return;
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        for (Participant p : PARTICIPANTS.values()) {
            if (p.respawnAt > 0 && now >= p.respawnAt) beginRespawn(server, p);
        }
        if ((phase == SecretConvoyPhase.PREPARING || phase == SecretConvoyPhase.INTERMISSION) && now >= phaseDeadline) {
            beginRound(server, now);
            return;
        }
        if (phase != SecretConvoyPhase.ACTIVE && phase != SecretConvoyPhase.OVERTIME) return;

        updateProximity(server);
        resolvePlayerCollisions();
        boolean canMove = nearbyEscorts > 0 && !enemyBlocking;
        if (phase == SecretConvoyPhase.ACTIVE && now >= phaseDeadline) {
            if (canMove) { phase = SecretConvoyPhase.OVERTIME; overtimeLostAt = 0L; broadcast(server, "OVERTIME", ChatFormatting.GOLD); }
            else { completeRound(server, now, false); return; }
        }
        if (phase == SecretConvoyPhase.OVERTIME) {
            if (canMove) overtimeLostAt = 0L;
            else {
                if (overtimeLostAt == 0L) overtimeLostAt = now;
                if (now - overtimeLostAt >= config.overtimeGraceSeconds * 20L) { completeRound(server, now, false); return; }
            }
        }
        if (canMove) {
            double blocksPerTick = escortSpeed(nearbyEscorts, config) / 20.0D;
            moveTruck(server, Math.min(totalDistance, travelledDistance + blocksPerTick));
            if (travelledDistance >= totalDistance - WIN_EPSILON) { completeRound(server, now, true); return; }
            if (round == 2 && firstRecord != null && !firstRecord.reached
                    && travelledDistance > firstRecord.distance + WIN_EPSILON) {
                finish(server, TeamSide.BLUE, false); return;
            }
        }
        if (round == 2 && firstRecord != null && firstRecord.reached
                && now - roundStartedAt >= firstRecord.elapsedTicks) {
            finish(server, TeamSide.RED, false); return;
        }
        if (now >= nextDraftAt) {
            for (Participant p : PARTICIPANTS.values()) p.pendingDrafts++;
            nextDraftAt += draftIntervalMinutes * 60L * 20L;
            broadcast(server, "SECRET TECH DRAFTを獲得しました [I]", ChatFormatting.AQUA);
        }
        if (now - lastHudSync >= 10L) { lastHudSync = now; syncHud(server); }
    }

    public static ActionResult randomize(MinecraftServer server) {
        if (activeMatch()) return ActionResult.error("試合中は編成できません");
        List<ServerPlayer> players = new ArrayList<>(eligible(server));
        if (players.size() < 2) return ActionResult.error("参加者が2人以上必要です");
        Collections.shuffle(players); PREVIEW.clear(); ensureTeams(server);
        int redCount = players.size() / 2 + (players.size() % 2 == 1 && ThreadLocalRandom.current().nextBoolean() ? 1 : 0);
        for (int i = 0; i < players.size(); i++) { TeamSide side = i < redCount ? TeamSide.RED : TeamSide.BLUE; PREVIEW.put(players.get(i).getUUID(), side); applyTeam(players.get(i), side); }
        return ActionResult.ok("CONVOYチームをランダム編成しました");
    }

    public static ActionResult start(MinecraftServer server, int requestedMinutes, int requestedDraftInterval) {
        if (SecretShowdownManager.activeMatch()) return ActionResult.error("SECRET SHOWDOWNの試合中です");
        if (activeMatch()) return ActionResult.error("CONVOYはすでに進行中です");
        if (ArsenalMatchManager.activeMatch(server)) return ActionResult.error("アーセナルの試合中です");
        if (requestedMinutes < 1 || requestedMinutes > 60 || requestedDraftInterval < 1 || requestedDraftInterval > 10)
            return ActionResult.error("時間は1～60分、ドラフト間隔は1～10分です");
        SecretOperationsConfig.ConvoyValidation validation = SecretOperationsConfig.validateConvoy(server);
        if (!validation.valid()) return ActionResult.error(validation.error());
        String draftError = IntelDraftManager.matchValidationError();
        if (draftError != null) return ActionResult.error(draftError);
        if (!TemporaryBuildingManager.canStartMatch(server)) return ActionResult.error("仮設ブロックをリセット中です");
        List<ServerPlayer> players = eligible(server);
        if (players.size() < 2) return ActionResult.error("参加者が2人以上必要です");
        Set<UUID> ids = players.stream().map(Player::getUUID).collect(java.util.stream.Collectors.toSet());
        if (!PREVIEW.isEmpty() && !PREVIEW.keySet().equals(ids)) return ActionResult.error("参加者が変化しました。再編成してください");
        if (PREVIEW.isEmpty()) { ActionResult r = randomize(server); if (!r.success) return r; }

        config = validation.convoy(); level = validation.level(); matchId = UUID.randomUUID();
        int padding = Math.max(8, config.buildingBoundsPadding);
        int minX = config.route.stream().mapToInt(p -> (int)Math.floor(p.x)).min().orElse(0) - padding;
        int maxX = config.route.stream().mapToInt(p -> (int)Math.ceil(p.x)).max().orElse(0) + padding;
        int minZ = config.route.stream().mapToInt(p -> (int)Math.floor(p.z)).min().orElse(0) - padding;
        int maxZ = config.route.stream().mapToInt(p -> (int)Math.ceil(p.z)).max().orElse(0) + padding;
        if (!TemporaryBuildingManager.beginMatch(server, new TemporaryBuildingMatchContext(matchId, "secret_convoy",
                level.dimension(), minX, maxX, minZ, maxZ), SecretConvoyManager::canBuild))
            return ActionResult.error("仮設ブロック管理を開始できませんでした");

        roundMinutes = requestedMinutes; draftIntervalMinutes = requestedDraftInterval; round = 1;
        totalDistance = routeTotal(); travelledDistance = 0; firstRecord = null; PARTICIPANTS.clear();
        ensureTeams(server);
        phase = SecretConvoyPhase.PREPARING;
        phaseDeadline = server.overworld().getGameTime() + PREPARE_TICKS;
        for (ServerPlayer player : players) {
            Participant p = new Participant(player.getUUID(), PREVIEW.get(player.getUUID())); PARTICIPANTS.put(p.id, p);
            preparePlayer(server, player, p); IntelDraftManager.openMatch(player, false, System.currentTimeMillis() + 30_000L);
        }
        broadcast(server, "SECRET: CONVOY - ROUND 1 TECH選択 30秒", ChatFormatting.GOLD);
        return ActionResult.ok("SECRET: CONVOYの準備を開始しました");
    }

    public static ActionResult stop(MinecraftServer server) {
        if (!activeMatch()) {
            IntelDraftManager.endAll(server);
            return ActionResult.ok("CONVOYは進行していません。全プレイヤーの技術をリセットしました");
        }
        finish(server, null, true);
        return ActionResult.ok("CONVOYを中止し、全プレイヤーの技術をリセットしました");
    }

    public static ActionResult reload(MinecraftServer server) {
        SecretOperationsConfig.reload(); String error = SecretOperationsConfig.convoyError(server);
        return error == null ? ActionResult.ok("CONVOY設定を再読み込みしました") : ActionResult.error(error);
    }

    public static void openAdmin(ServerPlayer player, String notice) {
        MinecraftServer server = player.server; List<String> red = roster(server, true), blue = roster(server, false);
        String error = SecretOperationsConfig.convoyError(server);
        SecretOperationsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenSecretOperationsAdminPacket(SecretOperationMode.CONVOY, phase.name(), round,
                        roundMinutes, draftIntervalMinutes, red.size() + blue.size(), 0, 0,
                        error == null ? "" : error, notice == null ? "" : notice, red, blue));
    }

    private static void beginRound(MinecraftServer server, long now) {
        for (Participant p : PARTICIPANTS.values()) {
            ServerPlayer player = server.getPlayerList().getPlayer(p.id);
            if (player == null) continue;
            if (IntelDraftManager.hasSession(player)) IntelDraftManager.autoSelectCurrent(player);
            p.waiting = false; p.respawnAt = 0; player.setInvulnerable(false);
            replenishAndSpawn(player, p);
        }
        travelledDistance = 0; truck = ModEntities.SECRET_CONVOY_TRUCK.get().create(level);
        if (truck == null) { finish(server, null, true); return; }
        Vec3 start = config.route.get(0).vec(); truck.moveTo(start.x, start.y, start.z, routeYaw(0), 0); level.addFreshEntity(truck);
        phase = SecretConvoyPhase.ACTIVE; roundStartedAt = now; phaseDeadline = now + roundMinutes * 60L * 20L;
        nextDraftAt = now + draftIntervalMinutes * 60L * 20L; overtimeLostAt = 0; updateForcedChunks(); syncHud(server);
        broadcast(server, "SECRET: CONVOY ROUND " + round + " START - " + attacker().id.toUpperCase() + " ESCORT", ChatFormatting.GOLD);
    }

    private static void moveTruck(MinecraftServer server, double newDistance) {
        Vec3 old = truck.position(); travelledDistance = newDistance; Position position = routePosition(newDistance);
        truck.setYRot(position.yaw); truck.setPos(position.position.x, position.position.y, position.position.z);
        AABB swept = new AABB(old, position.position).inflate(2.4D, 1.3D, 2.4D).move(0, 1.0D, 0);
        TemporaryBuildingManager.removeByConvoy(level, swept);
        resolvePlayerCollisions();
        updateForcedChunks();
    }

    private static void resolvePlayerCollisions() {
        if (truck == null || level == null) return;
        AABB vehicleBounds = truck.getBoundingBox().inflate(1.15D, 0.0D, 1.15D);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, vehicleBounds)) {
            if (!PARTICIPANTS.containsKey(player.getUUID()) || player.isSpectator()) continue;
            if (player.getBoundingBox().intersects(vehicleBounds)) {
                player.teleportTo(level, player.getX(), truck.getY() + 2.55D, player.getZ(), player.getYRot(), player.getXRot());
                player.setDeltaMovement(player.getDeltaMovement().x, Math.max(0.1D, player.getDeltaMovement().y), player.getDeltaMovement().z);
            }
        }
    }

    private static void updateProximity(MinecraftServer server) {
        nearbyEscorts = 0; enemyBlocking = false;
        if (truck == null) return;
        TeamSide attacking = attacker();
        for (Participant p : PARTICIPANTS.values()) {
            ServerPlayer player = server.getPlayerList().getPlayer(p.id);
            if (player == null || p.waiting || player.isSpectator() || player.level() != level || !player.isAlive()) continue;
            Vec3 d = player.position().subtract(truck.position());
            if (Math.abs(d.y) > config.verticalRange || d.x * d.x + d.z * d.z > config.escortRadius * config.escortRadius) continue;
            if (p.team == attacking) nearbyEscorts++; else enemyBlocking = true;
        }
    }

    private static void completeRound(MinecraftServer server, long now, boolean reached) {
        long elapsed = now - roundStartedAt;
        if (round == 1) {
            firstRecord = new FirstRoundRecord(reached, travelledDistance, elapsed);
            removeTruck(); round = 2; phase = SecretConvoyPhase.INTERMISSION; phaseDeadline = now + PREPARE_TICKS;
            for (Participant p : PARTICIPANTS.values()) {
                ServerPlayer player = server.getPlayerList().getPlayer(p.id); if (player == null) continue;
                p.waiting = true; player.setInvulnerable(true); teleportWaiting(server, player);
                IntelDraftManager.openMatch(player, false, System.currentTimeMillis() + 30_000L);
            }
            SecretConvoyHudManager.clearAll(server.getPlayerList().getPlayers());
            broadcast(server, "攻守交代 - ROUND 2 TECH選択 30秒", ChatFormatting.YELLOW);
            return;
        }
        TeamSide winner;
        if (firstRecord.reached != reached) winner = reached ? TeamSide.BLUE : TeamSide.RED;
        else if (reached) winner = elapsed < firstRecord.elapsedTicks ? TeamSide.BLUE : elapsed > firstRecord.elapsedTicks ? TeamSide.RED : null;
        else winner = travelledDistance > firstRecord.distance + WIN_EPSILON ? TeamSide.BLUE
                : travelledDistance + WIN_EPSILON < firstRecord.distance ? TeamSide.RED : null;
        finish(server, winner, false);
    }

    private static void finish(MinecraftServer server, TeamSide winner, boolean canceled) {
        phase = SecretConvoyPhase.ENDING; removeTruck(); releaseChunks(); TemporaryBuildingManager.endMatch(server);
        if (!canceled) {
            String text = winner == null ? "DRAW" : winner.id.toUpperCase() + " VICTORY";
            Component title = Component.literal(text).withStyle(winner == TeamSide.RED ? ChatFormatting.RED
                    : winner == TeamSide.BLUE ? ChatFormatting.BLUE : ChatFormatting.GOLD);
            for (Participant p : PARTICIPANTS.values()) { ServerPlayer player = server.getPlayerList().getPlayer(p.id); if (player != null) player.connection.send(new ClientboundSetTitleTextPacket(title)); }
        }
        List<ServerPlayer> online = new ArrayList<>();
        for (Participant p : PARTICIPANTS.values()) {
            ServerPlayer player = server.getPlayerList().getPlayer(p.id); if (player == null) continue;
            player.setInvulnerable(false); player.getInventory().clearContent(); player.setGameMode(GameType.ADVENTURE);
            online.add(player);
        }
        PARTICIPANTS.clear(); PREVIEW.clear(); config = null; level = null; matchId = null; firstRecord = null;
        round = 0; travelledDistance = totalDistance = 0; phase = SecretConvoyPhase.IDLE;
        for (ServerPlayer player : online) {
            IntelDraftManager.end(player); SecretConvoyHudManager.clear(player); SecretOperationsManager.sync(player);
        }
    }

    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        Participant p = PARTICIPANTS.get(victim.getUUID()); if (p == null || !phase.running()) return;
        event.setCanceled(true); victim.setHealth(Math.max(1, victim.getMaxHealth())); victim.clearFire(); victim.getFoodData().setFoodLevel(20);
        p.waiting = true; p.respawnAt = victim.server.overworld().getGameTime() + RESPAWN_TICKS;
        victim.setInvulnerable(true); teleportWaiting(victim.server, victim); syncHud(victim.server);
    }

    @SubscribeEvent public static void hurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        Participant v = PARTICIPANTS.get(victim.getUUID());
        if (v == null) return;
        if (v.waiting || event.getSource().getEntity() instanceof ServerPlayer attacker
                && PARTICIPANTS.get(attacker.getUUID()) != null && PARTICIPANTS.get(attacker.getUUID()).team == v.team) event.setCanceled(true);
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Participant p = PARTICIPANTS.get(player.getUUID()); if (p == null) return;
        applyTeam(player, p.team); IntelDraftManager.syncOnLogin(player);
        if (phase == SecretConvoyPhase.ACTIVE || phase == SecretConvoyPhase.OVERTIME) { p.waiting = false; replenishAndSpawn(player, p); }
        else { p.waiting = true; teleportWaiting(player.server, player); }
        SecretOperationsManager.sync(player); syncHud(player.server);
    }

    @SubscribeEvent public static void stopping(ServerStoppingEvent event) { if (activeMatch()) finish(event.getServer(), null, true); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { PARTICIPANTS.clear(); PREVIEW.clear(); FORCED_CHUNKS.clear(); phase = SecretConvoyPhase.IDLE; }

    private static void beginRespawn(MinecraftServer server, Participant p) {
        p.respawnAt = 0; ServerPlayer player = server.getPlayerList().getPlayer(p.id); if (player == null) return;
        IntelDraftManager.grantRespawnAmmo(player);
        if (p.pendingDrafts > 0) { IntelDraftManager.openMatch(player, false); return; }
        p.waiting = false; player.setInvulnerable(false); replenishAndSpawn(player, p);
    }

    public static void openPendingDraft(ServerPlayer player) {
        Participant p = PARTICIPANTS.get(player.getUUID());
        if (p == null || p.pendingDrafts <= 0 || p.waiting || IntelDraftManager.hasSession(player)) return;
        IntelDraftManager.openMatch(player, true);
    }

    public static void onDraftSelected(ServerPlayer player) {
        Participant p = PARTICIPANTS.get(player.getUUID()); if (p == null) return;
        if (p.pendingDrafts > 0) p.pendingDrafts--;
        if (p.waiting && p.respawnAt == 0 && (phase == SecretConvoyPhase.ACTIVE || phase == SecretConvoyPhase.OVERTIME)) {
            p.waiting = false; player.setInvulnerable(false); replenishAndSpawn(player, p);
        }
        syncHud(player.server);
    }

    private static void preparePlayer(MinecraftServer server, ServerPlayer player, Participant p) {
        applyTeam(player, p.team); player.setGameMode(GameType.ADVENTURE); player.getInventory().clearContent();
        TemporaryBuildingLoadout.grantInitial(player); p.waiting = true; player.setInvulnerable(true); teleportWaiting(server, player);
        SecretOperationsManager.sync(player);
    }

    private static void replenishAndSpawn(ServerPlayer player, Participant p) {
        TemporaryBuildingLoadout.grantExtraMaterials(player); IntelDraftManager.grantRespawnAmmo(player);
        SecretOperationsConfig.SpawnPoint spawn = p.team == attacker() ? config.escortSpawn : config.defenderSpawn;
        player.teleportTo(level, spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch); player.setDeltaMovement(Vec3.ZERO);
    }

    private static void teleportWaiting(MinecraftServer server, ServerPlayer player) {
        SecretOperationsConfig.Validation v = SecretOperationsConfig.validate(server); if (!v.valid()) return;
        SecretOperationsConfig.SpawnPoint s = v.waiting(); player.teleportTo(SecretOperationsConfig.waitingLevel(server, v), s.x, s.y, s.z, s.yaw, s.pitch);
        player.setDeltaMovement(Vec3.ZERO);
    }

    private static void syncHud(MinecraftServer server) {
        long now = server.overworld().getGameTime(); long remaining = phase == SecretConvoyPhase.ACTIVE ? Math.max(0, phaseDeadline - now) : 0;
        for (Participant p : PARTICIPANTS.values()) {
            ServerPlayer player = server.getPlayerList().getPlayer(p.id); if (player == null) continue;
            if (p.team == attacker() && (phase == SecretConvoyPhase.ACTIVE || phase == SecretConvoyPhase.OVERTIME))
                SecretConvoyHudManager.syncEscort(player, totalDistance == 0 ? 0 : travelledDistance / totalDistance,
                        nearbyEscorts, enemyBlocking, remaining, Math.max(0, totalDistance - travelledDistance));
            else SecretConvoyHudManager.clear(player);
        }
    }

    private static void removeTruck() { if (truck != null) { truck.discard(); truck = null; } }
    private static double routeTotal() { double d = 0; for (int i=1;i<config.route.size();i++) d += config.route.get(i-1).vec().distanceTo(config.route.get(i).vec()); return d; }
    private static Position routePosition(double distance) {
        double remaining = distance;
        for (int i=1;i<config.route.size();i++) { Vec3 a=config.route.get(i-1).vec(), b=config.route.get(i).vec(); double len=a.distanceTo(b); if (remaining <= len || i==config.route.size()-1) { double t=len<1e-8?1:Math.min(1,remaining/len); return new Position(a.lerp(b,t), routeYaw(i-1)); } remaining-=len; }
        return new Position(config.route.get(config.route.size()-1).vec(), 0);
    }
    private static float routeYaw(int i) { Vec3 d=config.route.get(Math.min(i+1,config.route.size()-1)).vec().subtract(config.route.get(i).vec()); return (float)(Math.toDegrees(Math.atan2(-d.x,d.z))); }
    private static void updateForcedChunks() {
        if (level == null || truck == null || matchId == null) return;
        Set<Long> wanted = new LinkedHashSet<>(); BlockPos p=truck.blockPosition(); wanted.add(net.minecraft.world.level.ChunkPos.asLong(p.getX()>>4,p.getZ()>>4));
        Vec3 ahead = routePosition(Math.min(totalDistance, travelledDistance + 32.0D)).position;
        wanted.add(net.minecraft.world.level.ChunkPos.asLong(((int)Math.floor(ahead.x)) >> 4, ((int)Math.floor(ahead.z)) >> 4));
        for (long packed : List.copyOf(FORCED_CHUNKS)) if (!wanted.contains(packed)) { var c=new net.minecraft.world.level.ChunkPos(packed); ForgeChunkManager.forceChunk(level,DpvpTweaks.MODID,matchId,c.x,c.z,false,true); FORCED_CHUNKS.remove(packed); }
        for (long packed:wanted) if (FORCED_CHUNKS.add(packed)) { var c=new net.minecraft.world.level.ChunkPos(packed); ForgeChunkManager.forceChunk(level,DpvpTweaks.MODID,matchId,c.x,c.z,true,true); }
    }
    private static void releaseChunks() { if(level!=null&&matchId!=null) for(long packed:List.copyOf(FORCED_CHUNKS)){var c=new net.minecraft.world.level.ChunkPos(packed);ForgeChunkManager.forceChunk(level,DpvpTweaks.MODID,matchId,c.x,c.z,false,true);} FORCED_CHUNKS.clear(); }
    private static TeamSide attacker() { return round == 2 ? TeamSide.BLUE : TeamSide.RED; }
    public static boolean isParticipant(ServerPlayer p) { return PARTICIPANTS.containsKey(p.getUUID()); }
    public static boolean activeMatch() { return phase.running(); }
    public static SecretConvoyPhase phase() { return phase; }
    public static int round() { return round; }
    public static boolean canBuild(ServerPlayer p) { Participant s=PARTICIPANTS.get(p.getUUID()); return s!=null&&!s.waiting&&(phase==SecretConvoyPhase.ACTIVE||phase==SecretConvoyPhase.OVERTIME); }
    public static List<String> roster(MinecraftServer server, boolean red) { Map<UUID,TeamSide> source=phase==SecretConvoyPhase.IDLE?PREVIEW:participantTeams(); List<String> out=new ArrayList<>(); source.forEach((id,t)->{if((t==TeamSide.RED)==red)out.add(server.getProfileCache().get(id).map(x->x.getName()).orElse(id.toString().substring(0,8)));}); Collections.sort(out); return out; }
    private static Map<UUID,TeamSide> participantTeams(){Map<UUID,TeamSide> m=new LinkedHashMap<>();PARTICIPANTS.forEach((id,p)->m.put(id,p.team));return m;}
    private static List<ServerPlayer> eligible(MinecraftServer s){return s.getPlayerList().getPlayers().stream().filter(p->p.getTeam()==null||!"admin".equals(p.getTeam().getName())).toList();}
    private static void ensureTeams(MinecraftServer s){Scoreboard b=s.getScoreboard();PlayerTeam r=b.getPlayerTeam("red");if(r==null)r=b.addPlayerTeam("red");r.setColor(ChatFormatting.RED);r.setAllowFriendlyFire(false);PlayerTeam bl=b.getPlayerTeam("blue");if(bl==null)bl=b.addPlayerTeam("blue");bl.setColor(ChatFormatting.BLUE);bl.setAllowFriendlyFire(false);}
    private static void applyTeam(ServerPlayer p,TeamSide s){ensureTeams(p.server);p.server.getScoreboard().addPlayerToTeam(p.getScoreboardName(),p.server.getScoreboard().getPlayerTeam(s.id));}
    private static void broadcast(MinecraftServer s,String text,ChatFormatting color){for(Participant p:PARTICIPANTS.values()){ServerPlayer x=s.getPlayerList().getPlayer(p.id);if(x!=null)x.sendSystemMessage(Component.literal(text).withStyle(color));}}
    static double escortSpeed(int nearby, SecretOperationsConfig.Convoy settings) {
        return Math.max(0, Math.min(nearby, settings.maxSpeedEscorts)) * settings.speedPerEscort;
    }

    private enum TeamSide { RED("red"), BLUE("blue"); final String id; TeamSide(String id){this.id=id;} }
    private static final class Participant { final UUID id; final TeamSide team; boolean waiting; int pendingDrafts; long respawnAt; Participant(UUID id,TeamSide team){this.id=id;this.team=team;} }
    private record Position(Vec3 position,float yaw){}
    private record FirstRoundRecord(boolean reached,double distance,long elapsedTicks){}
    public record ActionResult(boolean success,String message){public static ActionResult ok(String m){return new ActionResult(true,m);}public static ActionResult error(String m){return new ActionResult(false,m);}}
}
