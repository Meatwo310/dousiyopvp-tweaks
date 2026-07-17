package com.dousiyo.dpvptweaks.timer.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class TimerClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public enum HudPosition {
        TOP_CENTER,
        TOP_RIGHT
    }

    public static final ForgeConfigSpec.EnumValue<HudPosition> HUD_POSITION = BUILDER
            .comment("Timer HUD position")
            .defineEnum("hudPosition", HudPosition.TOP_CENTER);

    public static final ForgeConfigSpec.DoubleValue HUD_SCALE = BUILDER
            .comment("Timer HUD scale (0.65 - 1.25)")
            .defineInRange("hudScale", 1.0D, 0.65D, 1.25D);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private TimerClientConfig() {}
}
