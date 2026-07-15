package com.dousiyo.dpvptweaks.secretoperations;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.entity.ModEntities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(DpvpTweaks.MODID)
@PrefixGameTestTemplate(false)
public final class SecretConvoyGameTests {
    private SecretConvoyGameTests() {}

    @GameTest(template = "inteldraftgametests.empty")
    public static void bentRouteUsesPolylineDistance(GameTestHelper helper) {
        var result = SecretConvoyHudManager.calculateRouteProgress(new Vec3(10, 0, 5),
                List.of(new Vec3(0, 0, 0), new Vec3(10, 0, 0), new Vec3(10, 0, 10)));
        assertNear(helper, result.totalDistance(), 20.0D, "total route distance");
        assertNear(helper, result.remainingDistance(), 5.0D, "remaining route distance");
        assertNear(helper, result.progress(), 0.75D, "route progress");

        SecretOperationsConfig.Convoy settings = new SecretOperationsConfig.Convoy();
        settings.speedPerEscort = 1.0D; settings.maxSpeedEscorts = 8;
        assertNear(helper, SecretConvoyManager.escortSpeed(0, settings), 0.0D, "zero escort speed");
        assertNear(helper, SecretConvoyManager.escortSpeed(3, settings), 3.0D, "three escort speed");
        assertNear(helper, SecretConvoyManager.escortSpeed(9, settings), 8.0D, "capped escort speed");
        helper.succeed();
    }

    @GameTest(template = "inteldraftgametests.empty")
    public static void truckIsIndestructibleAndNotPersistent(GameTestHelper helper) {
        var truck = ModEntities.SECRET_CONVOY_TRUCK.get().create(helper.getLevel());
        if (truck == null) helper.fail("Convoy truck could not be created");
        if (truck.isPushable()) helper.fail("Convoy truck must not be pushable");
        if (truck.shouldBeSaved()) helper.fail("Convoy truck must not survive a server restart");
        if (truck.hurt(helper.getLevel().damageSources().generic(), 100.0F)) helper.fail("Convoy truck accepted damage");
        helper.succeed();
    }

    private static void assertNear(GameTestHelper helper, double actual, double expected, String label) {
        if (Math.abs(actual - expected) > 1.0E-6D) helper.fail(label + ": expected=" + expected + ", actual=" + actual);
    }
}
