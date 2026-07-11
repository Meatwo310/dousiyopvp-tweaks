package com.dousiyo.dpvptweaks.timer.core;

import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.UUID;

public class TimerInstance {
    private final String timerId;
    private final UUID ownerUuid;
    private TimerState state;
    private int currentTicks;
    private long lastServerGameTime;
    private long lastClientSyncGameTime;
    private boolean visible;
    @Nullable
    private Component titleOverride;
    private boolean finishFired;

    public TimerInstance(String timerId, UUID ownerUuid, TimerDefinition definition) {
        this.timerId = timerId;
        this.ownerUuid = ownerUuid;
        this.state = TimerState.IDLE;
        this.currentTicks = definition.getMode() == TimerMode.COUNTDOWN ? definition.getDurationTicks() : 0;
        this.lastServerGameTime = 0L;
        this.lastClientSyncGameTime = 0L;
        this.visible = false;
        this.finishFired = false;
    }

    public String getTimerId() {
        return timerId;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public TimerState getState() {
        return state;
    }

    public void setState(TimerState state) {
        this.state = state;
    }

    public int getCurrentTicks() {
        return currentTicks;
    }

    public void setCurrentTicks(int currentTicks) {
        this.currentTicks = Math.max(0, currentTicks);
    }

    public long getLastServerGameTime() {
        return lastServerGameTime;
    }

    public void setLastServerGameTime(long lastServerGameTime) {
        this.lastServerGameTime = lastServerGameTime;
    }

    public long getLastClientSyncGameTime() {
        return lastClientSyncGameTime;
    }

    public void setLastClientSyncGameTime(long lastClientSyncGameTime) {
        this.lastClientSyncGameTime = lastClientSyncGameTime;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Nullable
    public Component getTitleOverride() {
        return titleOverride;
    }

    public void setTitleOverride(@Nullable Component titleOverride) {
        this.titleOverride = titleOverride;
    }

    public boolean isFinishFired() {
        return finishFired;
    }

    public void setFinishFired(boolean finishFired) {
        this.finishFired = finishFired;
    }

    public void resetByDefinition(TimerDefinition definition) {
        this.state = TimerState.IDLE;
        this.currentTicks = definition.getMode() == TimerMode.COUNTDOWN ? definition.getDurationTicks() : 0;
        this.lastServerGameTime = 0L;
        this.lastClientSyncGameTime = 0L;
        this.finishFired = false;
    }
}
