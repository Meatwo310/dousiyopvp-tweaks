package com.dousiyo.dpvptweaks.client.capture;

import com.dousiyo.dpvptweaks.capture.core.PointState;
import com.dousiyo.dpvptweaks.capture.core.TeamSide;
import com.dousiyo.dpvptweaks.network.CapturePointEventS2CPacket;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientCapturePointsState {
    private static final float CAPTURE_STEP_PROGRESS = 0.1F;
    private static final Map<Integer, Snapshot> POINTS = new ConcurrentHashMap<>();
    private static volatile int focusedSlot = -1;
    private static volatile boolean focusedBoosted;
    private static volatile boolean captureFeatureEnabled = true;

    private ClientCapturePointsState() {}

    public static void applyPointEvent(CapturePointEventS2CPacket msg) {
        int slot = msg.getSlotIndex();
        if (slot < 0 || slot > 4) {
            return;
        }
        POINTS.put(slot, new Snapshot(msg.getServerGameTime(), msg.getState(), msg.getOwner(),
                msg.getProgress0(), msg.getCaptureTeam(), msg.getRatePerTick()));
    }

    public static void applyCaptureFeatureState(boolean enabled) {
        captureFeatureEnabled = enabled;
        if (!enabled) {
            POINTS.clear();
            focusedSlot = -1;
            focusedBoosted = false;
        }
    }

    public static void applyFocusSlot(int slotIndex) {
        focusedSlot = slotIndex;
        if (slotIndex < 0) {
            focusedBoosted = false;
        }
    }

    public static void applyFocusedBoosted(boolean boosted) {
        focusedBoosted = boosted;
    }

    public static int getFocusedSlot() {
        return focusedSlot;
    }

    public static boolean isFocusedBoosted() {
        return focusedBoosted;
    }

    public static boolean isCaptureFeatureEnabled() {
        return captureFeatureEnabled;
    }

    public static List<Integer> getSortedSlots() {
        List<Integer> slots = new ArrayList<>(POINTS.keySet());
        slots.sort(Comparator.naturalOrder());
        return slots;
    }

    public static Snapshot getSnapshot(int slot) {
        return POINTS.get(slot);
    }

    public static float resolveProgress(Snapshot snapshot, Minecraft mc) {
        if (snapshot == null) {
            return 0.5F;
        }
        if (mc.level == null || snapshot.state != PointState.CAPTURING || snapshot.captureTeam == TeamSide.NONE) {
            return snapshot.progress0;
        }

        long dt = Math.max(0L, mc.level.getGameTime() - snapshot.serverGameTime);
        float predicted = snapshot.progress0 + snapshot.ratePerTick * dt;
        float target = nextDisplayTarget(snapshot);
        if (target >= snapshot.progress0) {
            return clamp(Math.min(predicted, target));
        }
        return clamp(Math.max(predicted, target));
    }

    private static float nextDisplayTarget(Snapshot snapshot) {
        if (snapshot.captureTeam == TeamSide.BLUE) {
            return clamp(snapshot.progress0 + CAPTURE_STEP_PROGRESS);
        }
        if (snapshot.captureTeam == TeamSide.RED) {
            return clamp(snapshot.progress0 - CAPTURE_STEP_PROGRESS);
        }
        return clamp(snapshot.progress0);
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    public record Snapshot(long serverGameTime,
                           PointState state,
                           TeamSide owner,
                           float progress0,
                           TeamSide captureTeam,
                           float ratePerTick) {
    }
}