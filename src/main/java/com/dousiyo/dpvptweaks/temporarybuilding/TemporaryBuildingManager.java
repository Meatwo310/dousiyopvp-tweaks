package com.dousiyo.dpvptweaks.temporarybuilding;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative temporary structure graph shared by SECRET OPERATIONS modes. */
@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TemporaryBuildingManager {
    public static final int MAX_BLOCKS_PER_PLAYER = 128;
    public static final int MAX_BLOCKS_PER_MATCH = 4096;
    private static final int MAX_CANDIDATE_SCAN = 20_000;
    private static final int MAX_COLLAPSE_PER_TICK = 256;
    private static final int MAX_RESET_PER_TICK = 512;
    private static final Direction[] SUPPORT_ORDER = {
            Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP
    };
    private static final Map<MinecraftServer, MatchState> STATES = new IdentityHashMap<>();

    private TemporaryBuildingManager() {}

    public static synchronized boolean beginMatch(MinecraftServer server, TemporaryBuildingMatchContext context,
                                                  TemporaryBuildingMatchBridge bridge) {
        MatchState state = state(server);
        if (state.resetting || state.matchLive || state.savedData.size() != 0) return false;
        state.context = context;
        state.bridge = bridge;
        state.matchLive = true;
        state.ticketOwner = context.matchId();
        state.savedData.begin(context.matchId());
        return true;
    }

    public static synchronized void endMatch(MinecraftServer server) {
        MatchState state = state(server);
        state.matchLive = false;
        state.bridge = null;
        state.resetting = true;
        state.removedThisTick.clear();
        state.collapseQueue.clear();
        state.collapseSet.clear();
        state.nodes.forEach((dimension, nodes) -> nodes.keySet().forEach(position ->
                state.resetQueue.addLast(new BlockRef(dimension, position))));
        if (state.resetQueue.isEmpty()) completeReset(server, state);
    }

    public static synchronized boolean canStartMatch(MinecraftServer server) {
        MatchState state = state(server);
        return !state.matchLive && !state.resetting && state.savedData.size() == 0;
    }

    public static synchronized int registeredBlocks(MinecraftServer server) {
        return state(server).totalBlocks;
    }

    /** Removes only match-registered temporary blocks intersected by the convoy truck. */
    public static synchronized int removeByConvoy(ServerLevel level, AABB sweptBounds) {
        MatchState state = state(level.getServer());
        if (!state.matchLive || state.context == null || !state.context.dimension().equals(level.dimension())) return 0;
        Long2ObjectOpenHashMap<Node> registered = nodes(state, level.dimension());
        int removed = 0;
        BlockPos min = BlockPos.containing(sweptBounds.minX, sweptBounds.minY, sweptBounds.minZ);
        BlockPos max = BlockPos.containing(sweptBounds.maxX, sweptBounds.maxY, sweptBounds.maxZ);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            long packed = pos.asLong();
            if (!registered.containsKey(packed)) continue;
            BlockState blockState = level.getBlockState(pos);
            if (!blockState.is(TemporaryBuildingTags.TEMPORARY_BLOCKS)) continue;
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, blockState),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.08);
            level.playSound(null, pos, blockState.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.8F, 1.0F);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            state.removedThisTick.add(new BlockRef(level.dimension(), packed));
            removed++;
        }
        return removed;
    }

    /** Records a non-player removal while preserving the same end-of-tick collapse batching. */
    public static synchronized void recordRemoval(ServerLevel level, BlockPos pos) {
        MatchState state = state(level.getServer());
        if (nodes(state, level.dimension()).containsKey(pos.asLong()))
            state.removedThisTick.add(new BlockRef(level.dimension(), pos.asLong()));
    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        MatchState state = state(event.getServer());
        if (!state.savedData.needsRecovery()) return;
        state.resetting = true;
        state.ticketOwner = state.savedData.matchId() == null ? UUID.randomUUID() : state.savedData.matchId();
        state.savedData.snapshot().forEach((dimension, positions) -> {
            ResourceLocation id = ResourceLocation.tryParse(dimension);
            if (id == null) return;
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
            ServerLevel level = event.getServer().getLevel(key);
            if (level == null) return;
            for (long position : positions) {
                state.resetQueue.addLast(new BlockRef(key, position));
                retainChunk(state, level, position);
            }
        });
        DpvpTweaks.LOGGER.warn("[TemporaryBuilds] Recovering {} blocks left by an interrupted match",
                state.savedData.size());
    }

    @SubscribeEvent
    public static synchronized void serverStopped(ServerStoppedEvent event) {
        STATES.remove(event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static synchronized void blockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        BlockState placed = level.getBlockState(event.getPos());
        if (!placed.is(TemporaryBuildingTags.TEMPORARY_BLOCKS)) return;
        MatchState state = state(player.server);
        Support support = validatePlacement(state, player, level, event.getPos());
        if (support == null) {
            event.setCanceled(true);
            return;
        }
        registerPlacement(state, level, event.getPos(), player.getUUID(), support);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static synchronized void blockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        MatchState state = state(level.getServer());
        if (!event.getState().is(TemporaryBuildingTags.TEMPORARY_BLOCKS)) {
            if (event.getPlayer() instanceof ServerPlayer player && state.matchLive && state.bridge != null
                    && state.bridge.canBuild(player) && inside(state, level, event.getPos())) event.setCanceled(true);
            return;
        }
        Player player = event.getPlayer();
        boolean toolAllowed = player.getMainHandItem().is(Items.IRON_PICKAXE)
                || player.getMainHandItem().is(Items.NETHERITE_PICKAXE);
        if (!(player instanceof ServerPlayer serverPlayer) || !state.matchLive || state.bridge == null
                || !state.bridge.canBuild(serverPlayer) || !inside(state, level, event.getPos()) || !toolAllowed) {
            event.setCanceled(true);
            return;
        }
        event.setExpToDrop(0);
        state.removedThisTick.add(new BlockRef(level.dimension(), event.getPos().asLong()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static synchronized void explosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        MatchState state = state(level.getServer());
        if (!state.matchLive || state.context == null || !state.context.dimension().equals(level.dimension())) return;
        event.getAffectedBlocks().removeIf(position -> {
            if (!inside(state, level, position)) return false;
            if (level.getBlockState(position).is(TemporaryBuildingTags.TEMPORARY_BLOCKS)) {
                state.removedThisTick.add(new BlockRef(level.dimension(), position.asLong()));
                return false;
            }
            return true;
        });
    }

    @SubscribeEvent
    public static synchronized void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MatchState state = STATES.get(event.getServer());
        if (state == null) return;
        if (!state.resetting) {
            processRemovals(event.getServer(), state);
            processCollapse(event.getServer(), state);
        }
        if (state.resetting) processReset(event.getServer(), state);
    }

    private static MatchState state(MinecraftServer server) {
        return STATES.computeIfAbsent(server, ignored -> new MatchState(TemporaryBuildingSavedData.get(server)));
    }

    private static Support validatePlacement(MatchState state, ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!state.matchLive || state.bridge == null || !state.bridge.canBuild(player) || !inside(state, level, pos)) return null;
        if (state.totalBlocks >= MAX_BLOCKS_PER_MATCH || state.playerCounts.getOrDefault(player.getUUID(), 0) >= MAX_BLOCKS_PER_PLAYER)
            return null;
        Long2ObjectOpenHashMap<Node> nodes = nodes(state, level.dimension());
        Support best = null;
        for (Direction direction : SUPPORT_ORDER) {
            BlockPos adjacent = pos.relative(direction);
            BlockState adjacentState = level.getBlockState(adjacent);
            if (adjacentState.is(TemporaryBuildingTags.TEMPORARY_BLOCKS)) {
                Node parent = nodes.get(adjacent.asLong());
                if (parent != null && !parent.collapseQueued) {
                    Support candidate = new Support(direction, false, parent.depth + 1);
                    if (best == null || (!best.terrain && candidate.depth < best.depth)) best = candidate;
                }
            } else if (isTerrainSupport(level, adjacent, adjacentState, direction.getOpposite())) {
                return new Support(direction, true, 0);
            }
        }
        return best;
    }

    private static boolean isTerrainSupport(ServerLevel level, BlockPos pos, BlockState state, Direction face) {
        return !state.isAir() && state.getFluidState().isEmpty() && !state.canBeReplaced()
                && state.isFaceSturdy(level, pos, face);
    }

    private static void registerPlacement(MatchState state, ServerLevel level, BlockPos pos, UUID owner, Support support) {
        Long2ObjectOpenHashMap<Node> nodes = nodes(state, level.dimension());
        long packed = pos.asLong();
        if (nodes.containsKey(packed)) return;
        nodes.put(packed, new Node(owner, support.parentDirection, support.terrain, support.depth));
        state.totalBlocks++;
        state.playerCounts.merge(owner, 1, Integer::sum);
        String dimension = level.dimension().location().toString();
        state.savedData.add(dimension, packed);
        retainChunk(state, level, packed);
    }

    private static void processRemovals(MinecraftServer server, MatchState state) {
        if (state.removedThisTick.isEmpty()) return;
        List<BlockRef> removed = List.copyOf(state.removedThisTick);
        state.removedThisTick.clear();
        Map<ResourceKey<Level>, LongOpenHashSet> seeds = new java.util.HashMap<>();
        for (BlockRef ref : removed) {
            ServerLevel level = server.getLevel(ref.dimension);
            if (level == null) continue;
            if (level.getBlockState(BlockPos.of(ref.position)).is(TemporaryBuildingTags.TEMPORARY_BLOCKS)) continue;
            removeNode(state, level, ref.position);
            LongOpenHashSet set = seeds.computeIfAbsent(ref.dimension, ignored -> new LongOpenHashSet());
            BlockPos pos = BlockPos.of(ref.position);
            for (Direction direction : SUPPORT_ORDER) {
                long neighbor = pos.relative(direction).asLong();
                if (nodes(state, ref.dimension).containsKey(neighbor)) set.add(neighbor);
            }
        }
        seeds.forEach((dimension, positions) -> {
            ServerLevel level = server.getLevel(dimension);
            if (level != null) recheckComponents(state, level, positions);
        });
    }

    private static void recheckComponents(MatchState state, ServerLevel level, LongOpenHashSet seeds) {
        Long2ObjectOpenHashMap<Node> nodes = nodes(state, level.dimension());
        LongOpenHashSet visited = new LongOpenHashSet();
        for (long seed : seeds) {
            if (visited.contains(seed) || !nodes.containsKey(seed)) continue;
            LongOpenHashSet component = new LongOpenHashSet();
            ArrayDeque<Long> queue = new ArrayDeque<>();
            queue.add(seed);
            visited.add(seed);
            boolean exceeded = false;
            while (!queue.isEmpty()) {
                long current = queue.removeFirst();
                component.add(current);
                if (component.size() > MAX_CANDIDATE_SCAN) { exceeded = true; break; }
                BlockPos pos = BlockPos.of(current);
                for (Direction direction : SUPPORT_ORDER) {
                    long adjacent = pos.relative(direction).asLong();
                    if (nodes.containsKey(adjacent) && visited.add(adjacent)) queue.addLast(adjacent);
                }
            }
            if (exceeded || !rebuildSupportedComponent(level, nodes, component)) {
                if (exceeded) DpvpTweaks.LOGGER.warn("[TemporaryBuilds] Candidate scan exceeded {} blocks", MAX_CANDIDATE_SCAN);
                for (long position : component) queueCollapse(state, level.dimension(), position);
            }
        }
    }

    private static boolean rebuildSupportedComponent(ServerLevel level, Long2ObjectOpenHashMap<Node> nodes,
                                                     LongOpenHashSet component) {
        ArrayDeque<Long> queue = new ArrayDeque<>();
        LongOpenHashSet assigned = new LongOpenHashSet();
        for (long packed : component) {
            BlockPos pos = BlockPos.of(packed);
            Direction terrainDirection = terrainDirection(level, pos);
            if (terrainDirection != null) {
                Node node = nodes.get(packed);
                node.parentDirection = terrainDirection;
                node.terrainRoot = true;
                node.depth = 0;
                assigned.add(packed);
                queue.addLast(packed);
            }
        }
        if (queue.isEmpty()) return false;
        while (!queue.isEmpty()) {
            long parentPacked = queue.removeFirst();
            Node parent = nodes.get(parentPacked);
            BlockPos parentPos = BlockPos.of(parentPacked);
            for (Direction direction : SUPPORT_ORDER) {
                long childPacked = parentPos.relative(direction).asLong();
                if (!component.contains(childPacked) || !assigned.add(childPacked)) continue;
                Node child = nodes.get(childPacked);
                child.parentDirection = direction.getOpposite();
                child.terrainRoot = false;
                child.depth = parent.depth + 1;
                queue.addLast(childPacked);
            }
        }
        return assigned.size() == component.size();
    }

    private static Direction terrainDirection(ServerLevel level, BlockPos pos) {
        for (Direction direction : SUPPORT_ORDER) {
            BlockPos adjacent = pos.relative(direction);
            BlockState state = level.getBlockState(adjacent);
            if (!state.is(TemporaryBuildingTags.TEMPORARY_BLOCKS)
                    && isTerrainSupport(level, adjacent, state, direction.getOpposite())) return direction;
        }
        return null;
    }

    private static void queueCollapse(MatchState state, ResourceKey<Level> dimension, long position) {
        Long2ObjectOpenHashMap<Node> nodes = nodes(state, dimension);
        Node node = nodes.get(position);
        if (node == null || node.collapseQueued) return;
        node.collapseQueued = true;
        BlockRef ref = new BlockRef(dimension, position);
        state.collapseSet.add(ref);
        state.collapseQueue.addLast(ref);
    }

    private static void processCollapse(MinecraftServer server, MatchState state) {
        int processed = 0;
        while (processed++ < MAX_COLLAPSE_PER_TICK && !state.collapseQueue.isEmpty()) {
            BlockRef ref = state.collapseQueue.removeFirst();
            state.collapseSet.remove(ref);
            ServerLevel level = server.getLevel(ref.dimension);
            if (level == null) continue;
            BlockPos pos = BlockPos.of(ref.position);
            BlockState blockState = level.getBlockState(pos);
            if (blockState.is(TemporaryBuildingTags.TEMPORARY_BLOCKS)) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, blockState),
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        8, 0.25, 0.25, 0.25, 0.05);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            removeNode(state, level, ref.position);
        }
    }

    private static void processReset(MinecraftServer server, MatchState state) {
        int processed = 0;
        while (processed++ < MAX_RESET_PER_TICK && !state.resetQueue.isEmpty()) {
            BlockRef ref = state.resetQueue.removeFirst();
            ServerLevel level = server.getLevel(ref.dimension);
            if (level == null) continue;
            BlockPos pos = BlockPos.of(ref.position);
            BlockState blockState = level.getBlockState(pos);
            if (blockState.is(TemporaryBuildingTags.TEMPORARY_BLOCKS)) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, blockState),
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        8, 0.25, 0.25, 0.25, 0.05);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            removeNode(state, level, ref.position);
        }
        if (state.resetQueue.isEmpty()) completeReset(server, state);
    }

    private static void removeNode(MatchState state, ServerLevel level, long position) {
        Node removed = nodes(state, level.dimension()).remove(position);
        if (removed != null) {
            state.totalBlocks = Math.max(0, state.totalBlocks - 1);
            state.playerCounts.computeIfPresent(removed.owner, (id, count) -> count <= 1 ? null : count - 1);
        }
        state.savedData.remove(level.dimension().location().toString(), position);
        releaseChunk(state, level, position);
    }

    private static void completeReset(MinecraftServer server, MatchState state) {
        state.nodes.clear();
        state.playerCounts.clear();
        state.totalBlocks = 0;
        state.resetting = false;
        state.context = null;
        state.bridge = null;
        state.ticketOwner = null;
        state.chunkReferences.clear();
        state.collapseQueue.clear();
        state.collapseSet.clear();
        state.savedData.completeReset();
        DpvpTweaks.LOGGER.info("[TemporaryBuilds] Match reset completed");
    }

    private static boolean inside(MatchState state, ServerLevel level, BlockPos pos) {
        return state.context != null && state.context.contains(level.dimension(), pos.getX(), pos.getZ());
    }

    private static Long2ObjectOpenHashMap<Node> nodes(MatchState state, ResourceKey<Level> dimension) {
        return state.nodes.computeIfAbsent(dimension, ignored -> new Long2ObjectOpenHashMap<>());
    }

    private static void retainChunk(MatchState state, ServerLevel level, long position) {
        long chunk = new ChunkPos(BlockPos.of(position)).toLong();
        Long2IntOpenHashMap refs = state.chunkReferences.computeIfAbsent(level.dimension(), ignored -> new Long2IntOpenHashMap());
        int previous = refs.get(chunk);
        refs.put(chunk, previous + 1);
        if (previous == 0 && state.ticketOwner != null) {
            ChunkPos chunkPos = new ChunkPos(chunk);
            ForgeChunkManager.forceChunk(level, DpvpTweaks.MODID, state.ticketOwner,
                    chunkPos.x, chunkPos.z, true, true);
        }
    }

    private static void releaseChunk(MatchState state, ServerLevel level, long position) {
        Long2IntOpenHashMap refs = state.chunkReferences.get(level.dimension());
        if (refs == null) return;
        long chunk = new ChunkPos(BlockPos.of(position)).toLong();
        int count = refs.get(chunk);
        if (count > 1) refs.put(chunk, count - 1);
        else if (count == 1) {
            refs.remove(chunk);
            if (state.ticketOwner != null) {
                ChunkPos chunkPos = new ChunkPos(chunk);
                ForgeChunkManager.forceChunk(level, DpvpTweaks.MODID, state.ticketOwner,
                        chunkPos.x, chunkPos.z, false, true);
            }
        }
    }

    private static final class MatchState {
        final TemporaryBuildingSavedData savedData;
        final Map<ResourceKey<Level>, Long2ObjectOpenHashMap<Node>> nodes = new java.util.HashMap<>();
        final Map<ResourceKey<Level>, Long2IntOpenHashMap> chunkReferences = new java.util.HashMap<>();
        final Map<UUID, Integer> playerCounts = new java.util.HashMap<>();
        final java.util.Set<BlockRef> removedThisTick = new java.util.LinkedHashSet<>();
        final ArrayDeque<BlockRef> collapseQueue = new ArrayDeque<>();
        final java.util.Set<BlockRef> collapseSet = new java.util.HashSet<>();
        final ArrayDeque<BlockRef> resetQueue = new ArrayDeque<>();
        TemporaryBuildingMatchContext context;
        TemporaryBuildingMatchBridge bridge;
        UUID ticketOwner;
        boolean matchLive;
        boolean resetting;
        int totalBlocks;

        MatchState(TemporaryBuildingSavedData savedData) { this.savedData = savedData; }
    }

    private static final class Node {
        final UUID owner;
        Direction parentDirection;
        boolean terrainRoot;
        int depth;
        boolean collapseQueued;

        Node(UUID owner, Direction parentDirection, boolean terrainRoot, int depth) {
            this.owner = owner;
            this.parentDirection = parentDirection;
            this.terrainRoot = terrainRoot;
            this.depth = depth;
        }
    }

    private record Support(Direction parentDirection, boolean terrain, int depth) {}
    private record BlockRef(ResourceKey<Level> dimension, long position) {}
}
