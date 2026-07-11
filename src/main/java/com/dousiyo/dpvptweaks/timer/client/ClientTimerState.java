package com.dousiyo.dpvptweaks.timer.client;

import com.dousiyo.dpvptweaks.timer.network.TimerHudUpdateS2CPacket;
import com.dousiyo.dpvptweaks.timer.core.TimerMode;
import com.dousiyo.dpvptweaks.timer.core.TimerState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ClientTimerState {
    private static final int FINISH_HOLD_TICKS = 40;
    private static final int FINISH_FADE_TICKS = 20;
    private static final int FINISH_ANIMATION_TICKS = FINISH_HOLD_TICKS + FINISH_FADE_TICKS;
    private static boolean visible;
    private static String timerId = "";
    private static TimerMode mode = TimerMode.COUNTDOWN;
    private static TimerState state = TimerState.IDLE;
    private static int baseTicks;
    private static int durationTicks;
    private static Component title = Component.empty();
    private static long syncedClientGameTime;
    private static long finishAnimStartGameTime = -1L;
    private static Component finishMessage = Component.empty();

    private ClientTimerState() {}

    public static void apply(TimerHudUpdateS2CPacket msg) {
        TimerState previousState = state;
        visible = msg.isVisible();
        timerId = msg.getTimerId();
        mode = msg.getMode();
        state = msg.getState();
        baseTicks = Math.max(0, msg.getCurrentTicks());
        durationTicks = Math.max(0, msg.getDurationTicks());
        title = msg.getTitle();
        finishMessage = msg.getFinishMessage();
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level != null ? mc.level.getGameTime() : 0L;
        syncedClientGameTime = now;

        if (visible && mode == TimerMode.COUNTDOWN && previousState != TimerState.FINISHED && state == TimerState.FINISHED) {
            finishAnimStartGameTime = now;
        } else if (!visible || state != TimerState.FINISHED) {
            finishAnimStartGameTime = -1L;
        }
    }

    public static boolean isVisible() {
        return visible;
    }

    public static Component getTitle() {
        return title;
    }

    public static String getTimerId() {
        return timerId;
    }

    public static TimerMode getMode() {
        return mode;
    }

    public static TimerState getState() {
        return state;
    }

    public static int getDurationTicks() {
        return durationTicks;
    }

    public static int getDisplayTicks(Minecraft mc) {
        if (!visible || mc.level == null || state != TimerState.RUNNING) {
            return baseTicks;
        }

        long delta = Math.max(0L, mc.level.getGameTime() - syncedClientGameTime);
        long wholeSeconds = delta / 20L;
        long steppedDeltaTicks = wholeSeconds * 20L;
        if (mode == TimerMode.COUNTDOWN) {
            return (int) Math.max(0L, baseTicks - steppedDeltaTicks);
        }
        long next = (long) baseTicks + steppedDeltaTicks;
        return (int) Math.min(Integer.MAX_VALUE, next);
    }

    public static boolean isFinishAnimating(Minecraft mc) {
        if (finishAnimStartGameTime < 0L || mc.level == null) {
            return false;
        }
        return mc.level.getGameTime() - finishAnimStartGameTime <= FINISH_ANIMATION_TICKS;
    }

    public static float getFinishAnimProgress(Minecraft mc) {
        if (finishAnimStartGameTime < 0L || mc.level == null) {
            return 1.0F;
        }
        long elapsed = Math.max(0L, mc.level.getGameTime() - finishAnimStartGameTime);
        if (elapsed <= FINISH_HOLD_TICKS) {
            return 0.0F;
        }
        long fadeElapsed = elapsed - FINISH_HOLD_TICKS;
        return Math.min(1.0F, fadeElapsed / (float) FINISH_FADE_TICKS);
    }

    public static Component getFinishMessage() {
        return finishMessage;
    }
}
