package com.dousiyo.dpvptweaks.client.secretoperations;

import com.dousiyo.dpvptweaks.network.SecretOperationsMatchStatePacket;
import com.dousiyo.dpvptweaks.secretoperations.SecretShowdownPhase;

public final class ClientShowdownState {
    private static volatile boolean participating;
    private static volatile SecretShowdownPhase phase = SecretShowdownPhase.IDLE;
    private static volatile int redScore, blueScore, personalPoints, pendingDrafts;
    private static volatile long remainingTicks, receivedAtMillis;
    private static volatile boolean waiting, dropProtected;
    private ClientShowdownState() {}

    public static void update(SecretOperationsMatchStatePacket packet) {
        participating = packet.participating(); phase = packet.phase(); redScore = packet.redScore(); blueScore = packet.blueScore();
        remainingTicks = packet.remainingTicks(); personalPoints = packet.personalPoints(); pendingDrafts = packet.pendingDrafts(); waiting = packet.waiting();
        dropProtected = packet.dropProtected(); receivedAtMillis = System.currentTimeMillis();
    }
    public static void clear() { update(new SecretOperationsMatchStatePacket(false, SecretShowdownPhase.IDLE, 0, 0, 0, 0, 0, false, false)); }
    public static boolean participating() { return participating; }
    public static SecretShowdownPhase phase() { return phase; }
    public static int redScore() { return redScore; }
    public static int blueScore() { return blueScore; }
    public static int personalPoints() { return personalPoints; }
    public static int pendingDrafts() { return pendingDrafts; }
    public static boolean waiting() { return waiting; }
    public static boolean dropProtected() { return dropProtected; }
    public static long displayedRemainingTicks() {
        if (phase != SecretShowdownPhase.PREPARING && phase != SecretShowdownPhase.ACTIVE) return 0L;
        return Math.max(0L, remainingTicks - (System.currentTimeMillis() - receivedAtMillis) / 50L);
    }
}
