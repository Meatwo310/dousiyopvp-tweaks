package com.dousiyo.dpvptweaks.capture.core;

import com.dousiyo.dpvptweaks.capture.data.CapturePointsDefinition;
import com.dousiyo.dpvptweaks.config.ServerConfig;
import com.dousiyo.dpvptweaks.network.CapturePointEventS2CPacket;
import com.dousiyo.dpvptweaks.network.CaptureFeatureStateS2CPacket;
import com.dousiyo.dpvptweaks.network.CaptureNetwork;
import com.dousiyo.dpvptweaks.network.PlayerPointFocusS2CPacket;
import com.dousiyo.dpvptweaks.network.PlayerPointHudStateS2CPacket;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Team;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

public final class CaptureManager {
    private static final float EPSILON = 1.0e-4F;
    private static final float CAPTURE_STEP_PROGRESS = 0.1F;
    private static final float DECAP_RATE_PER_TICK = 0.0125F; // 5% every 2 ticks
    private static final Map<MinecraftServer, CaptureManager> INSTANCES = new WeakHashMap<>();
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Path definitionPath;
    private CapturePointsDefinition definition;
    private final Map<Integer, PointRuntime> points = new LinkedHashMap<>();
    private final Map<UUID, Integer> focusByPlayer = new HashMap<>();
    private final Map<UUID, Boolean> focusBoostByPlayer = new HashMap<>();

    private long nextOccupancyCheckGameTime;
    private long nextTicketBleedGameTime;
    private boolean blueTicketsDepletedHandled;
    private boolean redTicketsDepletedHandled;
    private boolean captureEnabled = true;

    private CaptureManager(Path definitionPath) {
        this.definitionPath = definitionPath;
        this.definition = CapturePointsDefinition.empty();
        reloadInMemoryDefinition(CapturePointsDefinition.empty());
    }

    public static CaptureManager get(ServerLevel level) {
        return INSTANCES.computeIfAbsent(level.getServer(), ignored -> {
            CaptureManager manager = new CaptureManager(FMLPaths.CONFIGDIR.get().resolve("capture_points.json"));
            manager.reloadFromDisk(level, false);
            return manager;
        });
    }

    public int reloadFromDisk(ServerLevel level, boolean broadcast) {
        CapturePointsDefinition loaded;
        try {
            loaded = CapturePointsDefinition.load(definitionPath);
        } catch (Exception e) {
            LOGGER.error("Failed to load capture points from {}", definitionPath, e);
            loaded = CapturePointsDefinition.empty();
        }

        reloadInMemoryDefinition(loaded);
        nextOccupancyCheckGameTime = 0L;
        nextTicketBleedGameTime = 0L;
        blueTicketsDepletedHandled = false;
        redTicketsDepletedHandled = false;

        if (broadcast) {
            syncAllPlayers(level);
        }
        return loaded.size();
    }

    public Collection<CapturePointsDefinition.PointDefinition> listPoints() {
        return definition.points();
    }

    public Optional<CapturePointsDefinition.PointDefinition> getPoint(int slot) {
        return definition.get(slot);
    }

    public void setPointArea(ServerLevel level, ResourceKey<Level> dimension, int slot, int x1, int y1, int z1, int x2, int y2, int z2) {
        String id = definition.get(slot).map(CapturePointsDefinition.PointDefinition::id).orElse("slot_" + slot);
        CapturePointsDefinition.PointDefinition point = new CapturePointsDefinition.PointDefinition(slot, id, dimension.location().toString(), x1, y1, z1, x2, y2, z2).normalized();
        definition = definition.withPoint(point);
        reloadInMemoryDefinition(definition);
        syncAllPlayers(level);
    }

    public void removePoint(ServerLevel level, int slot) {
        definition = definition.withoutSlot(slot);
        reloadInMemoryDefinition(definition);
        syncAllPlayers(level);
    }

    public void onPlayerJoin(ServerPlayer player) {
        syncAllStateToPlayer(player, findContainingSlot(player));
    }

    public void onPlayerLogout(ServerPlayer player) {
        focusByPlayer.remove(player.getUUID());
        focusBoostByPlayer.remove(player.getUUID());
    }

    public void serverTick(ServerLevel level) {
        boolean enabled = ServerConfig.CAPTURE_ENABLED.get();
        if (enabled != captureEnabled) {
            captureEnabled = enabled;
            nextOccupancyCheckGameTime = 0L;
            nextTicketBleedGameTime = 0L;
            syncAllPlayers(level);
        }
        if (!enabled) {
            return;
        }
        if (points.isEmpty()) {
            return;
        }

        long now = level.getGameTime();
        processCaptureStepCompletions(level, now);

        if (now < nextOccupancyCheckGameTime) {
            return;
        }

        int interval = Math.max(1, ServerConfig.CAPTURE_OCCUPANCY_UPDATE_INTERVAL_TICKS.get());
        nextOccupancyCheckGameTime = now + interval;

        List<ServerPlayer> players = level.getServer().getPlayerList().getPlayers();
        processOccupancy(level, now, players);
        processTicketBleed(level, now);
        updateFocusSlots(level);
        syncFocusedBoostStates(level);
    }

    private void processTicketBleed(ServerLevel level, long now) {
        if (now < nextTicketBleedGameTime) {
            return;
        }
        nextTicketBleedGameTime = now + 40L;

        int ownedBlue = 0;
        int ownedRed = 0;
        for (PointRuntime point : points.values()) {
            if (point.state != PointState.OWNED) {
                continue;
            }
            if (point.owner == TeamSide.BLUE) {
                ownedBlue++;
            } else if (point.owner == TeamSide.RED) {
                ownedRed++;
            }
        }

        TeamSide losingSide = TeamSide.NONE;
        if (ownedBlue > ownedRed) {
            losingSide = TeamSide.RED;
        } else if (ownedRed > ownedBlue) {
            losingSide = TeamSide.BLUE;
        }

        if (losingSide != TeamSide.NONE) {
            TeamSide depletedSide = losingSide;
            ScoreboardTickets.subtractOne(level.getServer(), depletedSide)
                    .ifPresent(tickets -> handleTicketDepletion(level, depletedSide, tickets));
            return;
        }

        refreshDepletionState(level);
    }

    private void refreshDepletionState(ServerLevel level) {
        int minTickets = ServerConfig.MIN_TICKETS.get();
        ScoreboardTickets.getTickets(level.getServer(), TeamSide.BLUE)
                .ifPresent(tickets -> blueTicketsDepletedHandled = tickets <= minTickets && blueTicketsDepletedHandled);
        ScoreboardTickets.getTickets(level.getServer(), TeamSide.RED)
                .ifPresent(tickets -> redTicketsDepletedHandled = tickets <= minTickets && redTicketsDepletedHandled);
    }

    private void handleTicketDepletion(ServerLevel level, TeamSide side, int tickets) {
        int minTickets = ServerConfig.MIN_TICKETS.get();
        if (tickets > minTickets) {
            setDepletionHandled(side, false);
            return;
        }
        if (depletionHandled(side)) {
            return;
        }

        setDepletionHandled(side, true);
        executeConfiguredCommands(level, depletedCommands(side));
        executeConfiguredCommands(level, winnerCommands(opposingSide(side)));
    }

    private List<? extends String> depletedCommands(TeamSide side) {
        return side == TeamSide.BLUE
                ? ServerConfig.ON_TICKETS_DEPLETED_BLUE.get()
                : ServerConfig.ON_TICKETS_DEPLETED_RED.get();
    }

    private List<? extends String> winnerCommands(TeamSide winner) {
        return winner == TeamSide.BLUE
                ? ServerConfig.ON_BLUE_WIN.get()
                : ServerConfig.ON_RED_WIN.get();
    }

    private TeamSide opposingSide(TeamSide side) {
        if (side == TeamSide.BLUE) {
            return TeamSide.RED;
        }
        if (side == TeamSide.RED) {
            return TeamSide.BLUE;
        }
        return TeamSide.NONE;
    }

    private boolean depletionHandled(TeamSide side) {
        return side == TeamSide.BLUE ? blueTicketsDepletedHandled : redTicketsDepletedHandled;
    }

    private void setDepletionHandled(TeamSide side, boolean handled) {
        if (side == TeamSide.BLUE) {
            blueTicketsDepletedHandled = handled;
        } else if (side == TeamSide.RED) {
            redTicketsDepletedHandled = handled;
        }
    }

    private void executeConfiguredCommands(ServerLevel level, List<? extends String> commands) {
        if (commands.isEmpty()) {
            return;
        }

        MinecraftServer server = level.getServer();
        CommandSourceStack source = server.createCommandSourceStack()
                .withSuppressedOutput()
                .withPermission(4);
        for (String rawCommand : commands) {
            String command = rawCommand.trim();
            if (command.isEmpty()) {
                continue;
            }
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            try {
                server.getCommands().performPrefixedCommand(source, command);
            } catch (Exception e) {
                LOGGER.warn("Failed to execute ticket depletion command '{}'", command, e);
            }
        }
    }

    private void reloadInMemoryDefinition(CapturePointsDefinition newDefinition) {
        this.definition = newDefinition;
        points.clear();
        for (CapturePointsDefinition.PointDefinition point : newDefinition.points()) {
            points.put(point.slot(), PointRuntime.neutral(point));
        }
    }

    private void processCaptureStepCompletions(ServerLevel level, long now) {
        for (PointRuntime point : points.values()) {
            if (point.state != PointState.CAPTURING) {
                continue;
            }

            while (point.nextStepGameTime > 0L && now >= point.nextStepGameTime) {
                long previousStepTime = point.nextStepGameTime;
                point.progress = nextStepProgress(point.progress, point.captureTeam);
                point.pendingTeam = TeamSide.NONE;

                if (point.progress >= 1.0F - EPSILON) {
                    point.progress = 1.0F;
                    point.state = PointState.OWNED;
                    point.owner = TeamSide.BLUE;
                    point.captureTeam = TeamSide.NONE;
                    point.ratePerTick = 0.0F;
                    point.nextStepGameTime = 0L;
                    broadcastState(level, point, now);
                    break;
                }

                if (point.progress <= 0.0F + EPSILON) {
                    point.progress = 0.0F;
                    point.state = PointState.OWNED;
                    point.owner = TeamSide.RED;
                    point.captureTeam = TeamSide.NONE;
                    point.ratePerTick = 0.0F;
                    point.nextStepGameTime = 0L;
                    broadcastState(level, point, now);
                    break;
                }

                point.owner = ownerFromProgress(point.progress);
                point.ratePerTick = rateFor(point.captureTeam, point.progress, point.captureOccupantCount);
                point.nextStepGameTime = previousStepTime + captureStepDurationTicks(point.ratePerTick);
                broadcastState(level, point, now);
            }
        }
    }

    private void processOccupancy(ServerLevel level, long now, List<ServerPlayer> players) {
        for (PointRuntime point : points.values()) {
            int blueCount = 0;
            int redCount = 0;
            for (ServerPlayer player : players) {
                if (player.serverLevel().dimension() != point.dimension || !point.aabb.contains(player.position())) {
                    continue;
                }
                TeamSide side = resolvePlayerTeam(player);
                if (side == TeamSide.BLUE) {
                    blueCount++;
                } else if (side == TeamSide.RED) {
                    redCount++;
                }
            }

            float progressNow = point.progress;
            if (blueCount > 0 && redCount > 0) {
                boolean changed = applySnapshot(point, PointState.CONTESTED, ownerFromProgress(progressNow), progressNow,
                        TeamSide.NONE, 0.0F, now);
                point.pendingTeam = TeamSide.NONE;
                point.captureOccupantCount = 0;
                if (changed) {
                    broadcastState(level, point, now);
                }
                continue;
            }

            if (blueCount == 0 && redCount == 0) {
                PointState restingState = restingState(progressNow);
                boolean changed = applySnapshot(point, restingState, ownerFromProgress(progressNow), progressNow,
                        TeamSide.NONE, 0.0F, now);
                point.pendingTeam = TeamSide.NONE;
                point.captureOccupantCount = 0;
                if (changed) {
                    broadcastState(level, point, now);
                }
                continue;
            }

            TeamSide dominant = blueCount > 0 ? TeamSide.BLUE : TeamSide.RED;
            int dominantOccupantCount = dominant == TeamSide.BLUE ? blueCount : redCount;
            if ((dominant == TeamSide.BLUE && progressNow >= 1.0F - EPSILON)
                    || (dominant == TeamSide.RED && progressNow <= 0.0F + EPSILON)) {
                TeamSide owner = dominant;
                float edgeProgress = dominant == TeamSide.BLUE ? 1.0F : 0.0F;
                boolean changed = applySnapshot(point, PointState.OWNED, owner, edgeProgress,
                        TeamSide.NONE, 0.0F, now);
                point.pendingTeam = TeamSide.NONE;
                point.captureOccupantCount = 0;
                if (changed) {
                    broadcastState(level, point, now);
                }
                continue;
            }

            if (point.state == PointState.CAPTURING) {
                if (point.captureTeam == dominant) {
                    point.captureOccupantCount = dominantOccupantCount;
                    float expectedRate = rateFor(dominant, progressNow, dominantOccupantCount);
                    if (Math.abs(point.ratePerTick - expectedRate) > EPSILON) {
                        boolean changed = applySnapshot(point, PointState.CAPTURING, ownerFromProgress(progressNow),
                                progressNow, dominant, expectedRate, now);
                        if (changed) {
                            broadcastState(level, point, now);
                        }
                    }
                    point.pendingTeam = TeamSide.NONE;
                    continue;
                }
                boolean changed = applySnapshot(point, PointState.CONTESTED, ownerFromProgress(progressNow), progressNow,
                        TeamSide.NONE, 0.0F, now);
                point.pendingTeam = TeamSide.NONE;
                point.captureOccupantCount = 0;
                if (changed) {
                    broadcastState(level, point, now);
                }
                continue;
            }

            if (point.pendingTeam != dominant) {
                point.pendingTeam = dominant;
                point.pendingStartGameTime = now;
                PointState restingState = restingState(progressNow);
                boolean changed = applySnapshot(point, restingState, ownerFromProgress(progressNow), progressNow,
                        TeamSide.NONE, 0.0F, now);
                if (changed) {
                    broadcastState(level, point, now);
                }
                continue;
            }

            long delayTicks = (long) Math.max(0, ServerConfig.CAPTURE_START_DELAY_SECONDS.get()) * 20L;
            if (now - point.pendingStartGameTime < delayTicks) {
                continue;
            }

            float rate = rateFor(dominant, progressNow, dominantOccupantCount);
            boolean changed = applySnapshot(point, PointState.CAPTURING, ownerFromProgress(progressNow), progressNow,
                    dominant, rate, now);
            point.pendingTeam = TeamSide.NONE;
            point.captureOccupantCount = dominantOccupantCount;
            if (changed) {
                broadcastState(level, point, now);
            }
        }
    }

    private void updateFocusSlots(ServerLevel level) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            int nextSlot = findContainingSlot(player);
            int previousSlot = focusByPlayer.getOrDefault(player.getUUID(), -1);
            if (previousSlot == nextSlot) {
                continue;
            }
            syncFocusToPlayer(player, nextSlot);
        }
    }

    private int findContainingSlot(ServerPlayer player) {
        for (PointRuntime point : points.values()) {
            if (player.serverLevel().dimension() == point.dimension && point.aabb.contains(player.position())) {
                return point.slot;
            }
        }
        return -1;
    }

    private TeamSide resolvePlayerTeam(ServerPlayer player) {
        Team team = player.getTeam();
        if (team == null) {
            return TeamSide.NONE;
        }
        String teamName = team.getName();
        if (teamName.equals(ServerConfig.CAPTURE_BLUE_TEAM_NAME.get())) {
            return TeamSide.BLUE;
        }
        if (teamName.equals(ServerConfig.CAPTURE_RED_TEAM_NAME.get())) {
            return TeamSide.RED;
        }
        return TeamSide.NONE;
    }

    private PointState restingState(float progress) {
        TeamSide owner = ownerFromProgress(progress);
        return owner == TeamSide.NONE ? PointState.IDLE : PointState.OWNED;
    }

    private TeamSide ownerFromProgress(float progress) {
        if (progress >= 1.0F - EPSILON) {
            return TeamSide.BLUE;
        }
        if (progress <= 0.0F + EPSILON) {
            return TeamSide.RED;
        }
        return TeamSide.NONE;
    }

    private boolean applySnapshot(PointRuntime point,
                                  PointState nextState,
                                  TeamSide nextOwner,
                                  float nextProgress,
                                  TeamSide nextCaptureTeam,
                                  float nextRatePerTick,
                                  long now) {
        float clamped = snapProgress(nextProgress);

        boolean changed = point.state != nextState
                || point.owner != nextOwner
                || Math.abs(point.progress - clamped) > EPSILON
                || point.captureTeam != nextCaptureTeam
                || Math.abs(point.ratePerTick - nextRatePerTick) > EPSILON;

        if (!changed) {
            return false;
        }

        point.state = nextState;
        point.owner = nextOwner;
        point.progress = clamped;
        point.captureTeam = nextCaptureTeam;
        point.ratePerTick = nextRatePerTick;
        point.nextStepGameTime = nextState == PointState.CAPTURING
                ? now + captureStepDurationTicks(nextRatePerTick)
                : 0L;
        return true;
    }

    private float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private float snapProgress(float value) {
        return Math.round(clamp(value) / CAPTURE_STEP_PROGRESS) * CAPTURE_STEP_PROGRESS;
    }

    private float baseRatePerTick() {
        float captureSeconds = Math.max(1, ServerConfig.CAPTURE_SECONDS.get());
        return 0.5F / (captureSeconds * 20.0F);
    }

    private float rateFor(TeamSide dominant, float progress, int dominantOccupantCount) {
        boolean decapPhase = (dominant == TeamSide.BLUE && progress < 0.5F - EPSILON)
                || (dominant == TeamSide.RED && progress > 0.5F + EPSILON);
        float baseRate = decapPhase ? DECAP_RATE_PER_TICK : baseRatePerTick();
        if (dominantOccupantCount >= 2) {
            baseRate *= ServerConfig.CAPTURE_MULTI_OCCUPANT_RATE_MULTIPLIER.get().floatValue();
        }
        return dominant == TeamSide.BLUE ? baseRate : -baseRate;
    }

    private long captureStepDurationTicks(float ratePerTick) {
        float magnitude = Math.abs(ratePerTick);
        if (magnitude <= EPSILON) {
            return Long.MAX_VALUE;
        }
        return Math.max(1L, Math.round(CAPTURE_STEP_PROGRESS / magnitude));
    }

    private float nextStepProgress(float currentProgress, TeamSide captureTeam) {
        if (captureTeam == TeamSide.BLUE) {
            return snapProgress(currentProgress + CAPTURE_STEP_PROGRESS);
        }
        if (captureTeam == TeamSide.RED) {
            return snapProgress(currentProgress - CAPTURE_STEP_PROGRESS);
        }
        return snapProgress(currentProgress);
    }

    private void syncAllPlayers(ServerLevel level) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            int slot = findContainingSlot(player);
            syncAllStateToPlayer(player, slot);
        }
    }

    private void syncAllStateToPlayer(ServerPlayer player, int slot) {
        boolean enabled = ServerConfig.CAPTURE_ENABLED.get();
        syncCaptureFeatureStateToPlayer(player, enabled);
        if (!enabled) {
            syncFocusToPlayer(player, -1);
            return;
        }
        syncAllPointsToPlayer(player);
        syncFocusToPlayer(player, slot);
    }

    private void syncCaptureFeatureStateToPlayer(ServerPlayer player, boolean enabled) {
        CaptureNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CaptureFeatureStateS2CPacket(enabled));
    }

    private void syncAllPointsToPlayer(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        for (PointRuntime point : points.values()) {
            CaptureNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), buildPacket(point, now));
        }
    }

    private void broadcastState(ServerLevel level, PointRuntime point, long now) {
        CaptureNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), buildPacket(point, now));
    }

    private CapturePointEventS2CPacket buildPacket(PointRuntime point, long now) {
        float rate = point.state == PointState.CAPTURING ? point.ratePerTick : 0.0F;
        TeamSide captureTeam = point.state == PointState.CAPTURING ? point.captureTeam : TeamSide.NONE;
        return new CapturePointEventS2CPacket((byte) point.slot, now, point.state, point.owner, point.progress, captureTeam, rate);
    }

    private void syncFocusToPlayer(ServerPlayer player, int slot) {
        focusByPlayer.put(player.getUUID(), slot);
        CaptureNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PlayerPointFocusS2CPacket((byte) slot));
        syncFocusBoostToPlayer(player, boostedForSlot(slot), true);
    }

    private void syncFocusedBoostStates(ServerLevel level) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            int slot = focusByPlayer.getOrDefault(player.getUUID(), -1);
            syncFocusBoostToPlayer(player, boostedForSlot(slot), false);
        }
    }

    private void syncFocusBoostToPlayer(ServerPlayer player, boolean boosted, boolean force) {
        Boolean previous = focusBoostByPlayer.get(player.getUUID());
        if (!force && previous != null && previous == boosted) {
            return;
        }
        focusBoostByPlayer.put(player.getUUID(), boosted);
        CaptureNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PlayerPointHudStateS2CPacket(boosted));
    }

    private boolean boostedForSlot(int slot) {
        if (slot < 0) {
            return false;
        }
        PointRuntime point = points.get(slot);
        return point != null && isBoosted(point);
    }

    private boolean isBoosted(PointRuntime point) {
        return point.state == PointState.CAPTURING
                && point.captureOccupantCount >= 2
                && ServerConfig.CAPTURE_MULTI_OCCUPANT_RATE_MULTIPLIER.get().floatValue() > 1.0F;
    }

    private static final class PointRuntime {
        private final int slot;
        private final String id;
        private final ResourceKey<Level> dimension;
        private final AABB aabb;

        private PointState state = PointState.IDLE;
        private TeamSide owner = TeamSide.NONE;
        private float progress = 0.5F;
        private TeamSide captureTeam = TeamSide.NONE;
        private float ratePerTick = 0.0F;
        private TeamSide pendingTeam = TeamSide.NONE;
        private long pendingStartGameTime = 0L;
        private long nextStepGameTime = 0L;
        private int captureOccupantCount = 0;

        private PointRuntime(int slot, String id, ResourceKey<Level> dimension, AABB aabb) {
            this.slot = slot;
            this.id = id;
            this.dimension = dimension;
            this.aabb = aabb;
        }

        static PointRuntime neutral(CapturePointsDefinition.PointDefinition definition) {
            return new PointRuntime(definition.slot(), definition.id(), definition.dimensionKey(), definition.toAabb());
        }
    }
}
