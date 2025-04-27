package io.github.meatwo310.dpvptweaks.config;

import io.github.meatwo310.dpvptweaks.DpvpTweaks;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue DISABLE_STARTER_KIT = BUILDER
            .comment("Disable the starter kit's welcome items.")
            .define("disableStarterKit", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}
