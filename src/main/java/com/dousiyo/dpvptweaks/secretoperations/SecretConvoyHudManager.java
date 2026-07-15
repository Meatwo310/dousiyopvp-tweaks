package com.dousiyo.dpvptweaks.secretoperations;

import com.dousiyo.dpvptweaks.network.SecretConvoyHudStatePacket;
import com.dousiyo.dpvptweaks.network.SecretOperationsNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

/**
 * Server-side bridge between the convoy game logic and its escort-only HUD.
 * The supplied route must be ordered from the cargo start to its destination.
 */
public final class SecretConvoyHudManager {
    private SecretConvoyHudManager() {}

    public static void syncEscort(ServerPlayer player, Vec3 cargoPosition, List<Vec3> route,
            int nearbyEscorts, boolean enemyBlocking, long remainingTicks) {
        RouteProgress routeProgress = calculateRouteProgress(cargoPosition, route);
        send(player, new SecretConvoyHudStatePacket(true, (float) routeProgress.progress(),
                Math.max(0, nearbyEscorts), enemyBlocking, Math.max(0L, remainingTicks),
                routeProgress.remainingDistance()));
    }

    public static void syncEscort(ServerPlayer player, double progress, int nearbyEscorts,
            boolean enemyBlocking, long remainingTicks, double remainingRouteDistance) {
        send(player, new SecretConvoyHudStatePacket(true, (float)Math.max(0, Math.min(1, progress)),
                Math.max(0, nearbyEscorts), enemyBlocking, Math.max(0, remainingTicks),
                Math.max(0, remainingRouteDistance)));
    }

    /** Hides the convoy HUD. Call this for defenders and when the convoy round ends. */
    public static void clear(ServerPlayer player) {
        send(player, new SecretConvoyHudStatePacket(false, 0.0F, 0, false, 0L, 0.0D));
    }

    public static void clearAll(Iterable<ServerPlayer> players) { players.forEach(SecretConvoyHudManager::clear); }

    private static void send(ServerPlayer player, SecretConvoyHudStatePacket packet) {
        SecretOperationsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /**
     * Projects the cargo onto the closest route segment. Remaining distance follows
     * the polyline, so bends in the route are included instead of using a straight line.
     */
    public static RouteProgress calculateRouteProgress(Vec3 cargoPosition, List<Vec3> route) {
        if (cargoPosition == null || route == null || route.size() < 2)
            return new RouteProgress(0.0D, 0.0D, 0.0D);

        double totalDistance = 0.0D;
        for (int i = 0; i < route.size() - 1; i++)
            totalDistance += route.get(i).distanceTo(route.get(i + 1));
        if (totalDistance <= 1.0E-6D) return new RouteProgress(1.0D, 0.0D, 0.0D);

        double closestDistanceSqr = Double.POSITIVE_INFINITY;
        double distanceBeforeSegment = 0.0D;
        double travelledDistance = 0.0D;
        for (int i = 0; i < route.size() - 1; i++) {
            Vec3 start = route.get(i);
            Vec3 delta = route.get(i + 1).subtract(start);
            double segmentLengthSqr = delta.lengthSqr();
            double segmentLength = Math.sqrt(segmentLengthSqr);
            if (segmentLengthSqr <= 1.0E-12D) continue;

            double t = cargoPosition.subtract(start).dot(delta) / segmentLengthSqr;
            t = Math.max(0.0D, Math.min(1.0D, t));
            Vec3 projected = start.add(delta.scale(t));
            double distanceSqr = cargoPosition.distanceToSqr(projected);
            if (distanceSqr < closestDistanceSqr) {
                closestDistanceSqr = distanceSqr;
                travelledDistance = distanceBeforeSegment + segmentLength * t;
            }
            distanceBeforeSegment += segmentLength;
        }

        travelledDistance = Math.max(0.0D, Math.min(totalDistance, travelledDistance));
        return new RouteProgress(travelledDistance / totalDistance,
                totalDistance - travelledDistance, totalDistance);
    }

    public record RouteProgress(double progress, double remainingDistance, double totalDistance) {}
}
