package io.github.meatwo310.dpvptweaks.config;

import io.github.meatwo310.dpvptweaks.DpvpTweaks;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue DISABLE_STARTER_KIT = BUILDER
            .comment("Disable the starter kit's welcome items.")
            .define("disableStarterKit", true);

    private static final String COMMENT_DAMAGE = "The damage dealt by the %s";
    private static final String COMMENT_COOLDOWN = "The cooldown of the %s in ticks";

    public static final ForgeConfigSpec.DoubleValue VALINE1G_DAMAGE = BUILDER
            .comment(String.format(COMMENT_DAMAGE, "valine1g"))
            .defineInRange("valine1g_damage", 2.0, 0.0, Float.MAX_VALUE);
    public static final ForgeConfigSpec.IntValue VALINE1G_COOLDOWN = BUILDER
            .comment(String.format(COMMENT_COOLDOWN, "valine1g"))
            .defineInRange("valine1g_cooldown", 0, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue VALINE2G_DAMAGE = BUILDER
            .comment(String.format(COMMENT_DAMAGE, "valine2g"))
            .defineInRange("valine2g_damage", 5.0, 0.0, Float.MAX_VALUE);
    public static final ForgeConfigSpec.IntValue VALINE2G_COOLDOWN = BUILDER
            .comment(String.format(COMMENT_COOLDOWN, "valine2g"))
            .defineInRange("valine2g_cooldown", 9, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue VALINE3G_DAMAGE = BUILDER
            .comment(String.format(COMMENT_DAMAGE, "valine3g"))
            .defineInRange("valine3g_damage", 12.0, 0.0, Float.MAX_VALUE);
    public static final ForgeConfigSpec.IntValue VALINE3G_COOLDOWN = BUILDER
            .comment(String.format(COMMENT_COOLDOWN, "valine3g"))
            .defineInRange("valine3g_cooldown", 20, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}
