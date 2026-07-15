package com.dousiyo.dpvptweaks.client.secretoperations;

import com.dousiyo.dpvptweaks.network.SecretConvoyHudStatePacket;

public final class ClientSecretConvoyHudState {
    private static volatile boolean visible;
    private static volatile float progress;
    private static volatile int nearbyEscorts;
    private static volatile boolean enemyBlocking;
    private static volatile long remainingTicks;
    private static volatile double remainingRouteDistance;
    private static volatile long receivedAtMillis;

    private ClientSecretConvoyHudState() {}

    public static void update(SecretConvoyHudStatePacket packet) {
        visible = packet.visible();
        progress = Math.max(0.0F, Math.min(1.0F, packet.progress()));
        nearbyEscorts = Math.max(0, packet.nearbyEscorts());
        enemyBlocking = packet.enemyBlocking();
        remainingTicks = Math.max(0L, packet.remainingTicks());
        remainingRouteDistance = Math.max(0.0D, packet.remainingRouteDistance());
        receivedAtMillis = System.currentTimeMillis();
    }

    public static void clear() {
        update(new SecretConvoyHudStatePacket(false, 0.0F, 0, false, 0L, 0.0D));
    }

    public static boolean visible() { return visible; }
    public static float progress() { return progress; }
    public static int nearbyEscorts() { return nearbyEscorts; }
    public static boolean enemyBlocking() { return enemyBlocking; }
    public static double remainingRouteDistance() { return remainingRouteDistance; }

    public static long displayedRemainingTicks() {
        if (!visible) return 0L;
        return Math.max(0L, remainingTicks - (System.currentTimeMillis() - receivedAtMillis) / 50L);
    }
}
