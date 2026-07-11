package com.dousiyo.dpvptweaks.timer.core;

import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TimerDefinition {
    private final String id;
    private TimerMode mode;
    private int durationTicks;
    @Nullable
    private Component defaultTitle;
    @Nullable
    private Component finishMessage;
    private final List<String> onFinishCommands = new ArrayList<>();

    public TimerDefinition(String id, TimerMode mode, int durationTicks, @Nullable Component defaultTitle) {
        this.id = id;
        this.mode = mode;
        this.durationTicks = Math.max(0, durationTicks);
        this.defaultTitle = defaultTitle;
        this.finishMessage = Component.literal("終了");
    }

    public String getId() {
        return id;
    }

    public TimerMode getMode() {
        return mode;
    }

    public void setMode(TimerMode mode) {
        this.mode = mode;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public void setDurationTicks(int durationTicks) {
        this.durationTicks = Math.max(0, durationTicks);
    }

    @Nullable
    public Component getDefaultTitle() {
        return defaultTitle;
    }

    public void setDefaultTitle(@Nullable Component defaultTitle) {
        this.defaultTitle = defaultTitle;
    }

    @Nullable
    public Component getFinishMessage() {
        return finishMessage;
    }

    public void setFinishMessage(@Nullable Component finishMessage) {
        this.finishMessage = finishMessage;
    }

    public List<String> getOnFinishCommands() {
        return Collections.unmodifiableList(onFinishCommands);
    }

    public void setOnFinishCommands(List<String> commands) {
        onFinishCommands.clear();
        onFinishCommands.addAll(commands);
    }

    public void addOnFinishCommand(String command) {
        onFinishCommands.add(command);
    }

    public void clearOnFinishCommands() {
        onFinishCommands.clear();
    }
}
