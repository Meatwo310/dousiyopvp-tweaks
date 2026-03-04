package com.dousiyo.dpvptweaks.config;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue DISABLE_STARTER_KIT = BUILDER
            .comment("Disable the starter kit's welcome items.")
            .define("disableStarterKit", true);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MUTED_PLAYERS = BUILDER
            .comment("List of muted players. See: /dpvptweaks mute <player>")
            .defineList("mutedPlayers", List.of(), o -> o instanceof String);

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

    public static final ForgeConfigSpec.EnumValue<WalletsRenderMode> WALLETS_RENDER_MODE = BUILDER
            .comment("""
                    Adjust how LC's wallets are rendered on clients.
                    DEFAULT: Unchanged behavior. Players can choose to show or hide their wallets.
                    ALWAYS: Always render wallets. Default.
                    NEVER: Never render any wallets.""")
            .defineEnum("walletsRenderMode", WalletsRenderMode.ALWAYS);

    public static final ForgeConfigSpec.BooleanValue CURIOS_IGNORE_RENDER_CHANGE = BUILDER
            .comment("Ignore any packet from clients that request to show or hide their ANY curios.")
            .define("curiosIgnoreRenderChange", false);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CLEAR_INVENTORY_ON_DEATH_TEAMS = BUILDER
            .comment("Players in these scoreboard teams will have inventory cleared on death.")
            .defineList("clearInventoryOnDeathTeams", List.of(), o -> o instanceof String);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SET_SPECTATOR_ON_DEATH_TEAMS = BUILDER
            .comment("Players in these scoreboard teams will be set to spectator on death.")
            .defineList("setSpectatorOnDeathTeams", List.of(), o -> o instanceof String);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static Set<String> mutedPlayersSet = Set.of();

    @SubscribeEvent
    static void onModConfig(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        mutedPlayersSet = Set.copyOf(MUTED_PLAYERS.get());
    }
}
