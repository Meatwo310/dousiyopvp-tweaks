package com.dousiyo.dpvptweaks.timer.core;

import com.dousiyo.dpvptweaks.timer.network.ModNetwork;
import com.dousiyo.dpvptweaks.timer.network.TimerHudUpdateS2CPacket;
import com.dousiyo.dpvptweaks.timer.data.TimerSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

public class TimerManager {
    private static final int RUNNING_SYNC_INTERVAL_TICKS = 20 * 20;
    private static final int RUNNING_PROCESS_INTERVAL_TICKS = 20;
    private static final Map<MinecraftServer, TimerManager> INSTANCES = new WeakHashMap<>();

    private final TimerSavedData savedData;
    private final Map<String, TimerDefinition> definitions;
    private final Map<UUID, Map<String, TimerInstance>> instancesByPlayer = new HashMap<>();
    private final Map<UUID, String> activeHudTimerIds = new HashMap<>();

    private TimerManager(TimerSavedData savedData) {
        this.savedData = savedData;
        this.definitions = savedData.getDefinitions();
    }

    public static TimerManager get(ServerLevel level) {
        return INSTANCES.computeIfAbsent(level.getServer(), server -> new TimerManager(TimerSavedData.get(level)));
    }

    public TimerDefinition define(String id, TimerMode mode, int durationTicks, @Nullable Component title) {
        TimerDefinition definition = new TimerDefinition(id, mode, durationTicks, title);
        definitions.put(id, definition);
        markDirty();
        return definition;
    }

    public void removeDefinition(String id, ServerLevel level) {
        definitions.remove(id);
        instancesByPlayer.values().forEach(map -> map.remove(id));

        for (Map.Entry<UUID, String> entry : new ArrayList<>(activeHudTimerIds.entrySet())) {
            if (id.equals(entry.getValue())) {
                activeHudTimerIds.remove(entry.getKey());
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
                if (player != null) {
                    syncHideToPlayer(player);
                }
            }
        }
        markDirty();
    }

    public Optional<TimerDefinition> getDefinition(String id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public Map<String, TimerDefinition> getDefinitionsView() {
        return Map.copyOf(definitions);
    }

    public TimerInstance getOrCreateInstance(UUID playerUuid, String timerId) {
        TimerDefinition definition = requireDefinition(timerId);
        return instancesByPlayer
                .computeIfAbsent(playerUuid, ignored -> new HashMap<>())
                .computeIfAbsent(timerId, ignored -> new TimerInstance(timerId, playerUuid, definition));
    }

    public Optional<TimerInstance> getInstance(UUID playerUuid, String timerId) {
        return Optional.ofNullable(instancesByPlayer.getOrDefault(playerUuid, Map.of()).get(timerId));
    }

    public void removeInstance(UUID playerUuid, String timerId) {
        Map<String, TimerInstance> map = instancesByPlayer.get(playerUuid);
        if (map != null) {
            map.remove(timerId);
            if (map.isEmpty()) {
                instancesByPlayer.remove(playerUuid);
            }
        }
    }

    public void removeAllInstances(UUID playerUuid) {
        instancesByPlayer.remove(playerUuid);
        activeHudTimerIds.remove(playerUuid);
    }

    public void setActiveHudTimer(UUID playerUuid, @Nullable String timerId) {
        if (timerId == null) {
            activeHudTimerIds.remove(playerUuid);
            return;
        }
        activeHudTimerIds.put(playerUuid, timerId);
    }

    @Nullable
    public String getActiveHudTimer(UUID playerUuid) {
        return activeHudTimerIds.get(playerUuid);
    }

    public void show(UUID playerUuid, String timerId, ServerLevel level) {
        getOrCreateInstance(playerUuid, timerId).setVisible(true);
        activeHudTimerIds.put(playerUuid, timerId);
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerUuid);
        if (player != null) {
            syncHudToPlayer(player);
        }
    }

    public void hide(UUID playerUuid, ServerLevel level) {
        String activeId = activeHudTimerIds.remove(playerUuid);
        if (activeId != null) {
            getInstance(playerUuid, activeId).ifPresent(instance -> instance.setVisible(false));
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerUuid);
        if (player != null) {
            syncHideToPlayer(player);
        }
    }

    public void start(UUID playerUuid, String timerId, ServerLevel level) {
        TimerDefinition definition = requireDefinition(timerId);
        TimerInstance instance = getOrCreateInstance(playerUuid, timerId);
        if (definition.getMode() == TimerMode.COUNTDOWN && instance.getCurrentTicks() <= 0) {
            instance.setCurrentTicks(definition.getDurationTicks());
            instance.setFinishFired(false);
        }
        instance.setState(TimerState.RUNNING);
        instance.setLastServerGameTime(level.getGameTime());
        instance.setLastClientSyncGameTime(level.getGameTime());
        syncIfActive(level, playerUuid, timerId);
    }

    public void pause(UUID playerUuid, ServerLevel level) {
        TimerInstance instance = requireActiveInstance(playerUuid);
        if (instance.getState() != TimerState.RUNNING) {
            return;
        }
        TimerDefinition definition = requireDefinition(instance.getTimerId());
        updateRunningInstance(level, instance, definition, false, true);
        if (instance.getState() == TimerState.RUNNING) {
            instance.setState(TimerState.PAUSED);
        }
        syncActive(level, playerUuid);
    }

    public void resume(UUID playerUuid, ServerLevel level) {
        TimerInstance instance = requireActiveInstance(playerUuid);
        if (instance.getState() != TimerState.PAUSED) {
            return;
        }
        instance.setState(TimerState.RUNNING);
        instance.setLastServerGameTime(level.getGameTime());
        instance.setLastClientSyncGameTime(level.getGameTime());
        syncActive(level, playerUuid);
    }

    public void stop(UUID playerUuid, ServerLevel level) {
        TimerInstance instance = requireActiveInstance(playerUuid);
        instance.setState(TimerState.IDLE);
        instance.setLastServerGameTime(0L);
        syncActive(level, playerUuid);
    }

    public void reset(UUID playerUuid, String timerId, ServerLevel level) {
        TimerDefinition definition = requireDefinition(timerId);
        TimerInstance instance = getOrCreateInstance(playerUuid, timerId);
        instance.resetByDefinition(definition);
        instance.setLastClientSyncGameTime(level.getGameTime());
        syncIfActive(level, playerUuid, timerId);
    }

    public void setTime(UUID playerUuid, int seconds, ServerLevel level) {
        TimerInstance instance = requireActiveInstance(playerUuid);
        int ticks = Math.max(0, seconds * 20);
        instance.setCurrentTicks(ticks);
        if (ticks > 0 && instance.getState() == TimerState.FINISHED) {
            instance.setState(TimerState.IDLE);
            instance.setFinishFired(false);
        }
        instance.setLastClientSyncGameTime(level.getGameTime());
        syncActive(level, playerUuid);
    }

    public void setTitle(UUID playerUuid, Component title, ServerLevel level) {
        TimerInstance instance = requireActiveInstance(playerUuid);
        instance.setTitleOverride(title);
        syncActive(level, playerUuid);
    }

    public void setOnFinish(String timerId, List<String> commands) {
        TimerDefinition definition = requireDefinition(timerId);
        definition.setOnFinishCommands(commands);
        markDirty();
    }

    public void addOnFinish(String timerId, String command) {
        TimerDefinition definition = requireDefinition(timerId);
        definition.addOnFinishCommand(command);
        markDirty();
    }

    public void clearOnFinish(String timerId) {
        TimerDefinition definition = requireDefinition(timerId);
        definition.clearOnFinishCommands();
        markDirty();
    }

    public List<String> listOnFinish(String timerId) {
        return requireDefinition(timerId).getOnFinishCommands();
    }

    public void setFinishMessage(String timerId, Component message) {
        TimerDefinition definition = requireDefinition(timerId);
        definition.setFinishMessage(message);
        markDirty();
    }

    public void clearFinishMessage(String timerId) {
        TimerDefinition definition = requireDefinition(timerId);
        definition.setFinishMessage(Component.literal("終了"));
        markDirty();
    }

    public Component getFinishMessage(String timerId) {
        TimerDefinition definition = requireDefinition(timerId);
        Component message = definition.getFinishMessage();
        return message != null ? message : Component.literal("終了");
    }

    public void serverTick(ServerLevel level) {
        if (instancesByPlayer.isEmpty()) {
            return;
        }

        for (Map.Entry<UUID, Map<String, TimerInstance>> playerEntry : instancesByPlayer.entrySet()) {
            UUID playerUuid = playerEntry.getKey();
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerUuid);
            if (player == null) {
                continue;
            }

            for (TimerInstance instance : playerEntry.getValue().values()) {
                if (instance.getState() != TimerState.RUNNING) {
                    continue;
                }

                TimerDefinition definition = definitions.get(instance.getTimerId());
                if (definition == null) {
                    continue;
                }

                long now = level.getGameTime();
                if (now - instance.getLastServerGameTime() < RUNNING_PROCESS_INTERVAL_TICKS) {
                    continue;
                }

                boolean finishedNow = updateRunningInstance(level, instance, definition, true, false);
                if (finishedNow && instance.getTimerId().equals(activeHudTimerIds.get(playerUuid))) {
                    syncHudToPlayer(player);
                    instance.setLastClientSyncGameTime(level.getGameTime());
                    continue;
                }

                if (instance.getTimerId().equals(activeHudTimerIds.get(playerUuid))
                        && shouldPeriodicSync(level.getGameTime(), instance.getLastClientSyncGameTime())) {
                    syncHudToPlayer(player);
                    instance.setLastClientSyncGameTime(level.getGameTime());
                }
            }
        }
    }

    public void syncHudOnLogin(ServerPlayer player) {
        String activeId = activeHudTimerIds.get(player.getUUID());
        if (activeId == null) {
            syncHideToPlayer(player);
            return;
        }

        TimerDefinition definition = definitions.get(activeId);
        if (definition == null) {
            activeHudTimerIds.remove(player.getUUID());
            syncHideToPlayer(player);
            return;
        }

        TimerInstance instance = getOrCreateInstance(player.getUUID(), activeId);
        if (instance.getState() == TimerState.RUNNING) {
            updateRunningInstance(player.serverLevel(), instance, definition, true, true);
        }
        instance.setLastClientSyncGameTime(player.serverLevel().getGameTime());
        syncHudToPlayer(player);
    }

    public void syncHudToPlayer(ServerPlayer player) {
        String activeId = activeHudTimerIds.get(player.getUUID());
        if (activeId == null) {
            syncHideToPlayer(player);
            return;
        }

        TimerDefinition definition = definitions.get(activeId);
        if (definition == null) {
            syncHideToPlayer(player);
            return;
        }

        TimerInstance instance = getOrCreateInstance(player.getUUID(), activeId);
        Component title = instance.getTitleOverride();
        if (title == null) {
            title = definition.getDefaultTitle();
        }
        if (title == null) {
            title = Component.literal(activeId);
        }

        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                TimerHudUpdateS2CPacket.show(
                        activeId,
                        definition.getMode(),
                        instance.getState(),
                        instance.getCurrentTicks(),
                        definition.getDurationTicks(),
                        title,
                        definition.getFinishMessage() != null ? definition.getFinishMessage() : Component.literal("終了")
                ));
    }

    public void syncHideToPlayer(ServerPlayer player) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), TimerHudUpdateS2CPacket.hide());
    }

    public void markDirty() {
        savedData.setDirty();
    }

    private TimerDefinition requireDefinition(String timerId) {
        TimerDefinition definition = definitions.get(timerId);
        if (definition == null) {
            throw new IllegalArgumentException("タイマー定義が見つかりません: " + timerId);
        }
        return definition;
    }

    private TimerInstance requireActiveInstance(UUID playerUuid) {
        String timerId = activeHudTimerIds.get(playerUuid);
        if (timerId == null) {
            throw new IllegalStateException("HUDに表示中のタイマーがありません");
        }
        return getOrCreateInstance(playerUuid, timerId);
    }

    private void syncIfActive(ServerLevel level, UUID playerUuid, String timerId) {
        if (!timerId.equals(activeHudTimerIds.get(playerUuid))) {
            return;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerUuid);
        if (player != null) {
            syncHudToPlayer(player);
        }
    }

    private void syncActive(ServerLevel level, UUID playerUuid) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerUuid);
        if (player != null) {
            syncHudToPlayer(player);
        }
    }

    private boolean updateRunningInstance(ServerLevel level, TimerInstance instance, TimerDefinition definition,
                                          boolean fireOnFinish, boolean allowSubSecondDelta) {
        long now = level.getGameTime();
        long last = instance.getLastServerGameTime();
        long delta = Math.max(0L, now - last);
        if (!allowSubSecondDelta && delta < RUNNING_PROCESS_INTERVAL_TICKS) {
            return false;
        }
        if (delta == 0L) {
            return false;
        }

        if (definition.getMode() == TimerMode.COUNTDOWN) {
            instance.setCurrentTicks((int) Math.max(0L, instance.getCurrentTicks() - delta));
        } else {
            long next = (long) instance.getCurrentTicks() + delta;
            instance.setCurrentTicks((int) Math.min(Integer.MAX_VALUE, next));
        }
        instance.setLastServerGameTime(now);

        if (definition.getMode() == TimerMode.COUNTDOWN && instance.getCurrentTicks() <= 0) {
            instance.setCurrentTicks(0);
            instance.setState(TimerState.FINISHED);
            if (!instance.isFinishFired()) {
                instance.setFinishFired(true);
                if (fireOnFinish) {
                    ServerPlayer player = level.getServer().getPlayerList().getPlayer(instance.getOwnerUuid());
                    if (player != null) {
                        TimerExecutor.execute(level, player, instance.getTimerId(), definition.getOnFinishCommands());
                    }
                }
                return true;
            }
        }
        return false;
    }

    private boolean shouldPeriodicSync(long nowGameTime, long lastSyncGameTime) {
        return nowGameTime - lastSyncGameTime >= RUNNING_SYNC_INTERVAL_TICKS;
    }
}
