package com.dousiyo.dpvptweaks.secretoperations;

import com.dousiyo.airstrike.entity.AirdropCrateEntity;
import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.arsenal.ArsenalWeaponFactory;
import com.dousiyo.dpvptweaks.arsenal.ArsenalWeaponStage;
import com.dousiyo.dpvptweaks.network.secretoperations.SecretOperationsNetwork;
import com.dousiyo.dpvptweaks.network.secretoperations.SupplyCrateProgressPacket;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.FireMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Server-authoritative periodic Air Strike crate support for SECRET: SHOWDOWN only. */
public final class SecretShowdownSupplyManager {
    private static final String TAG_MARKED = "dpvptweaksShowdownSupply";
    private static final String TAG_UNLOCKED = "dpvptweaksShowdownSupplyUnlocked";
    private static final ResourceLocation EMPTY_LOOT = ResourceLocation.fromNamespaceAndPath(DpvpTweaks.MODID, "secret_showdown/supply_empty");
    private static final int TARGET_ATTEMPTS = 32;
    private static final int MAX_HEARTBEAT_AGE = 3;
    private static final double MAX_OPEN_DISTANCE_SQR = 16.0D;
    private static final double MIN_LOOK_DOT = 0.92D;
    private static final long FORCE_TIMEOUT_TICKS = 30L * 20L;

    private static final Map<UUID, ActiveDrop> ACTIVE_DROPS = new LinkedHashMap<>();
    private static final Map<UUID, OpeningSession> OPENINGS = new LinkedHashMap<>();
    private static ResolvedConfig config;
    private static long nextDropTick;

    private SecretShowdownSupplyManager() {}

    public static String validationError(MinecraftServer server, SecretOperationsConfig.Validation showdown) {
        Resolution result = resolve(server, showdown);
        return result.error;
    }

    public static void begin(MinecraftServer server, SecretOperationsConfig.Validation showdown, long now) {
        cleanup(server);
        Resolution result = resolve(server, showdown);
        if (!result.valid()) {
            DpvpTweaks.LOGGER.error("SECRET SHOWDOWN supply drop was not started: {}", result.error);
            config = null;
            return;
        }
        config = result.config;
        nextDropTick = config.enabled ? now + config.intervalTicks : Long.MAX_VALUE;
    }

    public static void tick(MinecraftServer server, long now) {
        tickOpenings(server, now);
        tickForcedChunks(server, now);
        if (config == null || !config.enabled || SecretShowdownManager.phase() != SecretShowdownPhase.ACTIVE) return;
        if (now < nextDropTick) return;
        if (config.waitForClaimBeforeNextDrop && hasUnclaimedDrop()) return;
        nextDropTick = now + config.intervalTicks;
        spawnDrop(server, now);
    }

    public static void cleanup(MinecraftServer server) {
        for (OpeningSession session : List.copyOf(OPENINGS.values())) clearProgress(server, session.playerId);
        OPENINGS.clear();
        for (ActiveDrop drop : List.copyOf(ACTIVE_DROPS.values())) {
            ServerLevel level = server.getLevel(drop.dimension);
            if (level == null) continue;
            if (!drop.forced) {
                ForgeChunkManager.forceChunk(level, DpvpTweaks.MODID, drop.crateId, drop.chunk.x, drop.chunk.z, true, true);
                level.getChunk(drop.chunk.x, drop.chunk.z);
            }
            Entity entity = level.getEntity(drop.crateId);
            if (entity != null) entity.discard();
            ForgeChunkManager.forceChunk(level, DpvpTweaks.MODID, drop.crateId, drop.chunk.x, drop.chunk.z, false, true);
        }
        ACTIVE_DROPS.clear();
        config = null;
        nextDropTick = 0L;
    }

    public static boolean isMarked(Entity entity) {
        return entity instanceof AirdropCrateEntity && entity.getPersistentData().getBoolean(TAG_MARKED);
    }

    public static boolean isUnlocked(Entity entity) {
        return isMarked(entity) && entity.getPersistentData().getBoolean(TAG_UNLOCKED);
    }

    public static void handleHold(ServerPlayer player, int entityId, boolean holding) {
        if (!holding) {
            cancelForPlayer(player.server, player.getUUID());
            return;
        }
        Entity raw = player.serverLevel().getEntity(entityId);
        if (!(raw instanceof AirdropCrateEntity crate) || !isMarked(crate) || isUnlocked(crate)
                || !validOpener(player, crate)) {
            cancelForPlayer(player.server, player.getUUID());
            return;
        }
        OpeningSession session = OPENINGS.get(crate.getUUID());
        long now = player.serverLevel().getGameTime();
        if (session == null) {
            cancelForPlayer(player.server, player.getUUID());
            session = new OpeningSession(crate.getUUID(), player.getUUID(), crate.getId(), now);
            OPENINGS.put(crate.getUUID(), session);
            sendProgress(player, crate.getId(), 0, openTicks(), true);
        } else if (!session.playerId.equals(player.getUUID())) {
            sendProgress(player, crate.getId(), 0, openTicks(), false);
            return;
        }
        session.lastHeartbeat = now;
    }

    public static void cancelForPlayer(MinecraftServer server, UUID playerId) {
        OpeningSession found = null;
        for (OpeningSession session : OPENINGS.values()) {
            if (session.playerId.equals(playerId)) { found = session; break; }
        }
        if (found != null) {
            OPENINGS.remove(found.crateId);
            clearProgress(server, playerId);
        }
    }

    private static void tickOpenings(MinecraftServer server, long now) {
        for (OpeningSession session : List.copyOf(OPENINGS.values())) {
            ServerPlayer player = server.getPlayerList().getPlayer(session.playerId);
            Entity raw = player == null ? null : player.serverLevel().getEntity(session.entityId);
            if (!(raw instanceof AirdropCrateEntity crate) || !crate.getUUID().equals(session.crateId)
                    || now - session.lastHeartbeat > MAX_HEARTBEAT_AGE || !validOpener(player, crate)) {
                OPENINGS.remove(session.crateId);
                clearProgress(server, session.playerId);
                continue;
            }
            session.progress++;
            if ((session.progress & 1) == 0) sendProgress(player, crate.getId(), session.progress, openTicks(), true);
            if (openingComplete(session.progress, openTicks())) complete(player, crate, session);
        }
    }

    private static void complete(ServerPlayer player, AirdropCrateEntity crate, OpeningSession session) {
        OPENINGS.remove(session.crateId);
        if (isUnlocked(crate) || !SecretShowdownManager.addSupplyPoints(player,
                config == null ? 20 : config.teamPoints, config == null ? 20 : config.personalPoints)) {
            clearProgress(player.server, player.getUUID());
            return;
        }
        markUnlocked(crate);
        ActiveDrop drop = ACTIVE_DROPS.get(crate.getUUID());
        if (drop != null) drop.claimed = true;
        if (config != null && config.waitForClaimBeforeNextDrop)
            nextDropTick = player.server.overworld().getGameTime() + config.intervalTicks;
        releaseForcedChunk(player.server, crate.getUUID());
        SecretShowdownManager.broadcastSupplyClaim(player, config == null ? 20 : config.teamPoints);
        sendProgress(player, crate.getId(), openTicks(), openTicks(), false);
        crate.interact(player, InteractionHand.MAIN_HAND);
    }

    private static boolean validOpener(ServerPlayer player, AirdropCrateEntity crate) {
        if (player == null || config == null || SecretShowdownManager.phase() != SecretShowdownPhase.ACTIVE
                || !SecretShowdownManager.canOpenSupply(player) || !crate.isAlive() || !crate.isNoGravity()
                || player.level() != crate.level() || player.distanceToSqr(crate) > MAX_OPEN_DISTANCE_SQR
                || !player.hasLineOfSight(crate)) return false;
        Vec3 eye = player.getEyePosition();
        Vec3 toward = crate.getBoundingBox().getCenter().subtract(eye);
        if (toward.lengthSqr() < 1.0E-6D) return true;
        return player.getViewVector(1.0F).dot(toward.normalize()) >= MIN_LOOK_DOT;
    }

    private static int openTicks() {
        return config == null ? 200 : config.openTicks;
    }

    private static void spawnDrop(MinecraftServer server, long now) {
        ServerLevel level = server.getLevel(config.dimension);
        if (level == null) return;
        BlockPos target = findSafeTarget(level);
        if (target == null) {
            DpvpTweaks.LOGGER.warn("Could not find a safe SECRET SHOWDOWN supply-drop target in 32 attempts");
            return;
        }
        WeightedWeapon selected = selectWeapon();
        ArsenalWeaponFactory.Result generated = ArsenalWeaponFactory.create(selected.stage);
        if (!generated.valid()) {
            DpvpTweaks.LOGGER.error("Could not generate supply-drop weapon: {}", generated.error());
            return;
        }
        AirdropCrateEntity crate = com.dousiyo.airstrike.registry.ModEntities.AIRDROP_CRATE.get().create(level);
        if (crate == null) return;
        crate.setLootTable(EMPTY_LOOT);
        crate.setSmokeOrigin(target);
        crate.setPos(target.getX() + 0.5D, target.getY() + config.dropHeight, target.getZ() + 0.5D);
        markSupply(crate);
        putContents(crate, generated);
        ChunkPos chunk = new ChunkPos(target);
        ForgeChunkManager.forceChunk(level, DpvpTweaks.MODID, crate.getUUID(), chunk.x, chunk.z, true, true);
        if (!level.addFreshEntity(crate)) {
            ForgeChunkManager.forceChunk(level, DpvpTweaks.MODID, crate.getUUID(), chunk.x, chunk.z, false, true);
            return;
        }
        ACTIVE_DROPS.put(crate.getUUID(), new ActiveDrop(crate.getUUID(), level.dimension(), chunk, now));
        SecretShowdownManager.broadcastSupplyDrop(server);
    }

    private static void putContents(AirdropCrateEntity crate, ArsenalWeaponFactory.Result generated) {
        int slot = 0;
        crate.setItem(slot++, generated.gun().copy());
        for (ItemStack ammo : generated.ammo()) {
            if (slot >= crate.getContainerSize()) break;
            crate.setItem(slot++, ammo.copy());
        }
    }

    private static BlockPos findSafeTarget(ServerLevel level) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int minX = Mth.floor(config.minX);
        int maxX = Mth.ceil(config.maxX);
        int minZ = Mth.floor(config.minZ);
        int maxZ = Mth.ceil(config.maxZ);
        for (int attempt = 0; attempt < TARGET_ATTEMPTS; attempt++) {
            int x = random.nextInt(minX, maxX);
            int z = random.nextInt(minZ, maxZ);
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            BlockPos ground = surface.below();
            if (!level.getWorldBorder().isWithinBounds(surface) || surface.getY() + config.dropHeight >= level.getMaxBuildHeight()) continue;
            var groundState = level.getBlockState(ground);
            if (groundState.isAir() || groundState.is(BlockTags.LEAVES) || !groundState.getFluidState().isEmpty()) continue;
            if (!level.getFluidState(surface).isEmpty() || !level.getFluidState(surface.above()).isEmpty()) continue;
            if (!level.getBlockState(surface).getCollisionShape(level, surface).isEmpty()) continue;
            if (!level.getBlockState(surface.above()).getCollisionShape(level, surface.above()).isEmpty()) continue;
            return surface.immutable();
        }
        return null;
    }

    private static WeightedWeapon selectWeapon() {
        return config.weapons.get(weightedIndex(config.weapons.stream().map(weapon -> weapon.weight).toList(),
                ThreadLocalRandom.current().nextInt(config.totalWeight)));
    }

    static boolean openingComplete(int progressTicks, int requiredTicks) {
        return requiredTicks > 0 && progressTicks >= requiredTicks;
    }

    static void markSupply(AirdropCrateEntity crate) {
        crate.getPersistentData().putBoolean(TAG_MARKED, true);
        crate.getPersistentData().putBoolean(TAG_UNLOCKED, false);
    }

    static void markUnlocked(AirdropCrateEntity crate) {
        crate.getPersistentData().putBoolean(TAG_UNLOCKED, true);
    }

    static int weightedIndex(List<Integer> weights, int draw) {
        if (weights == null || weights.isEmpty() || draw < 0) throw new IllegalArgumentException("invalid weighted draw");
        int remaining = draw;
        for (int i = 0; i < weights.size(); i++) {
            int weight = weights.get(i);
            if (weight < 1) throw new IllegalArgumentException("weight must be positive");
            remaining -= weight;
            if (remaining < 0) return i;
        }
        throw new IllegalArgumentException("draw exceeds total weight");
    }

    private static void tickForcedChunks(MinecraftServer server, long now) {
        for (ActiveDrop drop : ACTIVE_DROPS.values()) {
            if (!drop.forced) continue;
            ServerLevel level = server.getLevel(drop.dimension);
            Entity entity = level == null ? null : level.getEntity(drop.crateId);
            if (entity == null || entity.isNoGravity() || now - drop.spawnTick >= FORCE_TIMEOUT_TICKS) {
                if (level != null) ForgeChunkManager.forceChunk(level, DpvpTweaks.MODID, drop.crateId,
                        drop.chunk.x, drop.chunk.z, false, true);
                drop.forced = false;
            }
        }
    }

    private static void releaseForcedChunk(MinecraftServer server, UUID crateId) {
        ActiveDrop drop = ACTIVE_DROPS.get(crateId);
        if (drop == null || !drop.forced) return;
        ServerLevel level = server.getLevel(drop.dimension);
        if (level != null) ForgeChunkManager.forceChunk(level, DpvpTweaks.MODID, crateId, drop.chunk.x, drop.chunk.z, false, true);
        drop.forced = false;
    }

    private static boolean hasUnclaimedDrop() {
        return ACTIVE_DROPS.values().stream().anyMatch(drop -> !drop.claimed);
    }

    private static void sendProgress(ServerPlayer player, int entityId, int progress, int total, boolean active) {
        SecretOperationsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SupplyCrateProgressPacket(entityId, progress, total, active));
    }

    private static void clearProgress(MinecraftServer server, UUID playerId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) sendProgress(player, -1, 0, openTicks(), false);
    }

    private static Resolution resolve(MinecraftServer server, SecretOperationsConfig.Validation showdown) {
        SecretOperationsConfig.SupplyDrop raw = SecretOperationsConfig.supplyDrop();
        if (raw == null) return Resolution.error("secretShowdown.supplyDropが未設定です");
        SecretOperationsConfig.AirSpawn air = showdown.air();
        String dimensionName = raw.dimension == null || raw.dimension.isBlank() ? air.dimension : raw.dimension;
        ResourceLocation dimensionId = ResourceLocation.tryParse(dimensionName);
        if (dimensionId == null) return Resolution.error("supplyDrop.dimensionが不正です");
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        ServerLevel level = server.getLevel(dimension);
        if (level == null) return Resolution.error("supplyDrop.dimensionが存在しません: " + dimensionId);
        double minX = Double.isFinite(raw.minX) ? raw.minX : air.minX;
        double maxX = Double.isFinite(raw.maxX) ? raw.maxX : air.maxX;
        double minZ = Double.isFinite(raw.minZ) ? raw.minZ : air.minZ;
        double maxZ = Double.isFinite(raw.maxZ) ? raw.maxZ : air.maxZ;
        if (!Double.isFinite(minX) || !Double.isFinite(maxX) || !Double.isFinite(minZ) || !Double.isFinite(maxZ)
                || minX >= maxX || minZ >= maxZ) return Resolution.error("supplyDropの投下範囲が不正です");
        if (minX < -29_999_984 || maxX > 29_999_984 || minZ < -29_999_984 || maxZ > 29_999_984
                || maxX - minX < 1.0D || maxZ - minZ < 1.0D)
            return Resolution.error("supplyDropの投下範囲がワールド座標外または狭すぎます");
        if (raw.dropHeight < 16 || raw.dropHeight > 320) return Resolution.error("supplyDrop.dropHeightは16～320です");
        if (raw.intervalSeconds < 1 || raw.intervalSeconds > 3600) return Resolution.error("supplyDrop.intervalSecondsは1～3600です");
        if (raw.openSeconds < 1 || raw.openSeconds > 60) return Resolution.error("supplyDrop.openSecondsは1～60です");
        if (raw.teamPoints < 0 || raw.personalPoints < 0) return Resolution.error("supplyDropのポイントは0以上です");
        if (raw.weapons == null || raw.weapons.isEmpty()) return Resolution.error("supplyDrop.weaponsが空です");

        List<WeightedWeapon> weapons = new ArrayList<>();
        long totalWeight = 0L;
        for (int i = 0; i < raw.weapons.size(); i++) {
            SecretOperationsConfig.SupplyWeapon entry = raw.weapons.get(i);
            if (entry == null || entry.weight < 1) return Resolution.error("supplyDrop.weapons[" + i + "].weightが不正です");
            ResourceLocation gunId = ResourceLocation.tryParse(entry.gunId == null ? "" : entry.gunId.trim());
            if (gunId == null) return Resolution.error("supplyDrop.weapons[" + i + "].gunIdが不正です");
            FireMode fireMode;
            try { fireMode = FireMode.valueOf(entry.fireMode == null ? "" : entry.fireMode.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException exception) { return Resolution.error("supplyDrop.weapons[" + i + "].fireModeが不正です"); }
            if (fireMode == FireMode.UNKNOWN || entry.reserveMagazines < 0 || entry.reserveMagazines > 256)
                return Resolution.error("supplyDrop.weapons[" + i + "]の射撃モードまたは予備弾倉数が不正です");
            EnumMap<AttachmentType, ResourceLocation> attachments = new EnumMap<>(AttachmentType.class);
            if (entry.attachments != null) for (var attachment : entry.attachments.entrySet()) {
                AttachmentType type;
                try { type = AttachmentType.valueOf(attachment.getKey().toUpperCase(Locale.ROOT)); }
                catch (IllegalArgumentException exception) { return Resolution.error("supplyDrop.weapons[" + i + "]のアタッチメント種別が不正です"); }
                ResourceLocation attachmentId = ResourceLocation.tryParse(attachment.getValue());
                if (type == AttachmentType.NONE || attachmentId == null)
                    return Resolution.error("supplyDrop.weapons[" + i + "]のアタッチメントが不正です");
                attachments.put(type, attachmentId);
            }
            ArsenalWeaponStage stage = new ArsenalWeaponStage(gunId, fireMode, attachments, entry.reserveMagazines);
            ArsenalWeaponFactory.Result generated = ArsenalWeaponFactory.create(stage);
            if (!generated.valid()) return Resolution.error("supplyDrop.weapons[" + i + "]: " + generated.error());
            weapons.add(new WeightedWeapon(stage, entry.weight));
            totalWeight += entry.weight;
            if (totalWeight > Integer.MAX_VALUE) return Resolution.error("supplyDrop.weaponsの重み合計が大きすぎます");
        }
        return Resolution.ok(new ResolvedConfig(raw.enabled, raw.waitForClaimBeforeNextDrop,
                dimension, minX, maxX, minZ, maxZ, raw.dropHeight,
                raw.intervalSeconds * 20, raw.openSeconds * 20, raw.teamPoints, raw.personalPoints,
                List.copyOf(weapons), (int) totalWeight));
    }

    private static final class OpeningSession {
        final UUID crateId;
        final UUID playerId;
        final int entityId;
        int progress;
        long lastHeartbeat;
        OpeningSession(UUID crateId, UUID playerId, int entityId, long now) {
            this.crateId = crateId; this.playerId = playerId; this.entityId = entityId; this.lastHeartbeat = now;
        }
    }

    private static final class ActiveDrop {
        final UUID crateId;
        final ResourceKey<Level> dimension;
        final ChunkPos chunk;
        final long spawnTick;
        boolean forced = true;
        boolean claimed;
        ActiveDrop(UUID crateId, ResourceKey<Level> dimension, ChunkPos chunk, long spawnTick) {
            this.crateId = crateId; this.dimension = dimension; this.chunk = chunk; this.spawnTick = spawnTick;
        }
    }

    private record WeightedWeapon(ArsenalWeaponStage stage, int weight) {}
    private record ResolvedConfig(boolean enabled, boolean waitForClaimBeforeNextDrop,
            ResourceKey<Level> dimension, double minX, double maxX,
            double minZ, double maxZ, int dropHeight, int intervalTicks, int openTicks, int teamPoints,
            int personalPoints, List<WeightedWeapon> weapons, int totalWeight) {}
    private record Resolution(ResolvedConfig config, String error) {
        static Resolution ok(ResolvedConfig config) { return new Resolution(config, null); }
        static Resolution error(String error) { return new Resolution(null, error); }
        boolean valid() { return config != null && error == null; }
    }
}
