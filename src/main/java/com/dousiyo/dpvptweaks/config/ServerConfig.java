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

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> OPEN_LOADOUT_ON_RESPAWN_TEAMS = BUILDER
            .comment("Players in these scoreboard teams will open the normal loadout GUI after death when they respawn.")
            .defineList("openLoadoutOnRespawnTeams", List.of(), o -> o instanceof String);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> OPEN_MINI_LOADOUT_ON_RESPAWN_TEAMS = BUILDER
            .comment("Players in these scoreboard teams will open the mini loadout GUI after death when they respawn.")
            .defineList("openMiniLoadoutOnRespawnTeams", List.of("tbg.mini.blue", "tbg.mini.red"), o -> o instanceof String);

    public static final ForgeConfigSpec.BooleanValue CAPTURE_ENABLED = BUILDER
            .comment("Enable capture points and ticket bleed feature")
            .define("captureEnabled", true);
    public static final ForgeConfigSpec.ConfigValue<String> CAPTURE_BLUE_TEAM_NAME = BUILDER
            .comment("Scoreboard team name treated as BLUE")
            .define("captureBlueTeamName", "blue");

    public static final ForgeConfigSpec.ConfigValue<String> CAPTURE_RED_TEAM_NAME = BUILDER
            .comment("Scoreboard team name treated as RED")
            .define("captureRedTeamName", "red");

    public static final ForgeConfigSpec.IntValue CAPTURE_SECONDS = BUILDER
            .comment("Seconds required to move from neutral (0.5) to owned edge (0.0/1.0)")
            .defineInRange("captureSeconds", 10, 1, 600);

    public static final ForgeConfigSpec.IntValue CAPTURE_START_DELAY_SECONDS = BUILDER
            .comment("Delay before capture starts after enemies are removed")
            .defineInRange("captureStartDelaySeconds", 1, 0, 30);

    public static final ForgeConfigSpec.IntValue CAPTURE_OCCUPANCY_UPDATE_INTERVAL_TICKS = BUILDER
            .comment("AABB occupancy check interval in ticks")
            .defineInRange("captureOccupancyUpdateIntervalTicks", 5, 1, 40);

    public static final ForgeConfigSpec.DoubleValue CAPTURE_MULTI_OCCUPANT_RATE_MULTIPLIER = BUILDER
            .comment("Capture rate multiplier applied when two or more players from the dominant team are inside a point")
            .defineInRange("captureMultiOccupantRateMultiplier", 2.0, 1.0, 16.0);

    public static final ForgeConfigSpec.ConfigValue<String> TICKET_OBJECTIVE_NAME = BUILDER
            .comment("Scoreboard objective name used for ticket bleed")
            .define("ticketObjectiveName", "tickets");

    public static final ForgeConfigSpec.ConfigValue<String> BLUE_TICKET_HOLDER = BUILDER
            .comment("Score holder used for BLUE tickets. Pseudo-player names are recommended.")
            .define("blueTicketHolder", "blue");

    public static final ForgeConfigSpec.ConfigValue<String> RED_TICKET_HOLDER = BUILDER
            .comment("Score holder used for RED tickets. Pseudo-player names are recommended.")
            .define("redTicketHolder", "red");

    public static final ForgeConfigSpec.IntValue MIN_TICKETS = BUILDER
            .comment("Lower bound applied when subtracting tickets")
            .defineInRange("minTickets", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ON_TICKETS_DEPLETED_BLUE = BUILDER
            .comment("Legacy: server commands executed once when BLUE tickets reach minTickets")
            .defineList("onTicketsDepletedBlue", List.of(), o -> o instanceof String);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ON_TICKETS_DEPLETED_RED = BUILDER
            .comment("Legacy: server commands executed once when RED tickets reach minTickets")
            .defineList("onTicketsDepletedRed", List.of(), o -> o instanceof String);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ON_BLUE_WIN = BUILDER
            .comment("Server commands executed once when BLUE wins because RED tickets reach minTickets")
            .defineList("onBlueWin", List.of(), o -> o instanceof String);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ON_RED_WIN = BUILDER
            .comment("Server commands executed once when RED wins because BLUE tickets reach minTickets")
            .defineList("onRedWin", List.of(), o -> o instanceof String);

    public static final ForgeConfigSpec.BooleanValue FUNCTION_PALETTE_ENABLED;
    public static final ForgeConfigSpec.IntValue FUNCTION_PALETTE_REQUIRED_PERMISSION_LEVEL;
    public static final ForgeConfigSpec.BooleanValue FUNCTION_PALETTE_ALLOW_TAGS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FUNCTION_PALETTE_ALLOWED_NAMESPACES;
    public static final ForgeConfigSpec.BooleanValue FUNCTION_PALETTE_SHOW_RUN_RESULT;

    static {
        BUILDER.push("function_palette");

        FUNCTION_PALETTE_ENABLED = BUILDER
                .comment("Enable the function palette GUI and its network handlers.")
                .define("enabled", true);

        FUNCTION_PALETTE_REQUIRED_PERMISSION_LEVEL = BUILDER
                .comment("Permission level required to open and run functions from the palette.")
                .defineInRange("required_permission_level", 2, 0, 4);

        FUNCTION_PALETTE_ALLOW_TAGS = BUILDER
                .comment("Reserved for future support of #function tags from the palette.")
                .define("allow_tags", false);

        FUNCTION_PALETTE_ALLOWED_NAMESPACES = BUILDER
                .comment("Allowlist of namespaces that can be executed from the palette. Use * to allow all.")
                .defineList("allowed_namespaces", List.of("*"), o -> o instanceof String);

        FUNCTION_PALETTE_SHOW_RUN_RESULT = BUILDER
                .comment("Show a short confirmation message after a function is executed from the palette.")
                .define("show_run_result", true);

        BUILDER.pop();
    }

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