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

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private TimerClientConfig() {}
}
