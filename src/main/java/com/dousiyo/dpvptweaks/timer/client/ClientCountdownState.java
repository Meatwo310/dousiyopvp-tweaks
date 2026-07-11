package com.dousiyo.dpvptweaks.timer.client;

import com.dousiyo.dpvptweaks.timer.network.CountdownHudS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public final class ClientCountdownState {
    private static final int FINISH_HOLD_TICKS = 10;
    private static final int FINISH_FADE_TICKS = 12;
    private static final int FINISH_ANIMATION_TICKS = FINISH_HOLD_TICKS + FINISH_FADE_TICKS;

    private static boolean visible;
    private static boolean finished;
    private static int baseTicks;
    private static int durationTicks;
    private static long syncedClientGameTime;
    private static long finishAnimStartGameTime = -1L;
    private static long runningAnimStartGameTime = -1L;
    private static int lastCueSecond = Integer.MIN_VALUE;
    private static boolean finishSoundPlayed;

    private ClientCountdownState() {}

    public static void apply(CountdownHudS2CPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level != null ? mc.level.getGameTime() : 0L;
        switch (msg.getState()) {
            case RUNNING -> {
                visible = true;
                finished = false;
                baseTicks = Math.max(0, msg.getCurrentTicks());
                durationTicks = Math.max(0, msg.getDurationTicks());
                syncedClientGameTime = now;
                runningAnimStartGameTime = now;
                finishAnimStartGameTime = -1L;
                lastCueSecond = Integer.MIN_VALUE;
                finishSoundPlayed = false;
            }
            case FINISHED -> {
                visible = true;
                finished = true;
                baseTicks = 0;
                durationTicks = Math.max(0, msg.getDurationTicks());
                syncedClientGameTime = now;
                finishAnimStartGameTime = now;
                lastCueSecond = 0;
            }
            case HIDDEN -> reset();
        }
    }

    public static void tick(Minecraft mc) {
        if (!visible || mc.level == null) {
            return;
        }

        if (!finished) {
            int seconds = getDisplaySeconds(mc);
            if (seconds != lastCueSecond) {
                if (seconds == 10) {
                    playUiSound(mc, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.8F, 0.65F);
                } else if (seconds <= 5 && seconds >= 1) {
                    float pitch = 0.9F + (5 - seconds) * 0.12F;
                    playUiSound(mc, SoundEvents.NOTE_BLOCK_HAT.value(), 0.9F, pitch);
                }
                lastCueSecond = seconds;
            }
            return;
        }

        if (!finishSoundPlayed) {
            playUiSound(mc, SoundEvents.PLAYER_LEVELUP, 0.75F, 1.35F);
            finishSoundPlayed = true;
        }

        if (finishAnimStartGameTime >= 0L && mc.level.getGameTime() - finishAnimStartGameTime >= FINISH_ANIMATION_TICKS) {
            reset();
        }
    }

    public static boolean isVisible() {
        return visible;
    }

    public static boolean isFinished() {
        return finished;
    }

    public static int getDisplayTicks(Minecraft mc) {
        if (!visible || finished || mc.level == null) {
            return baseTicks;
        }
        long delta = Math.max(0L, mc.level.getGameTime() - syncedClientGameTime);
        return (int) Math.max(0L, baseTicks - delta);
    }

    public static int getDisplaySeconds(Minecraft mc) {
        int ticks = getDisplayTicks(mc);
        return Math.max(0, (ticks + 19) / 20);
    }

    public static int getDurationTicks() {
        return durationTicks;
    }

    public static float getRunningAnimProgress(Minecraft mc) {
        if (runningAnimStartGameTime < 0L || mc.level == null) {
            return 1.0F;
        }
        long elapsed = Math.max(0L, mc.level.getGameTime() - runningAnimStartGameTime);
        return Math.min(1.0F, elapsed / 8.0F);
    }

    public static float getFinishAnimProgress(Minecraft mc) {
        if (finishAnimStartGameTime < 0L || mc.level == null) {
            return 1.0F;
        }
        long elapsed = Math.max(0L, mc.level.getGameTime() - finishAnimStartGameTime);
        if (elapsed <= FINISH_HOLD_TICKS) {
            return 0.0F;
        }
        return Math.min(1.0F, (elapsed - FINISH_HOLD_TICKS) / (float) FINISH_FADE_TICKS);
    }

    private static void reset() {
        visible = false;
        finished = false;
        baseTicks = 0;
        durationTicks = 0;
        syncedClientGameTime = 0L;
        finishAnimStartGameTime = -1L;
        runningAnimStartGameTime = -1L;
        lastCueSecond = Integer.MIN_VALUE;
        finishSoundPlayed = false;
    }

    private static void playUiSound(Minecraft mc, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        if (mc.getSoundManager() == null) {
            return;
        }
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }
}
