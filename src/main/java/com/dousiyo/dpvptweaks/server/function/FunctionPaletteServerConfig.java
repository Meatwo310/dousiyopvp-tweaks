package com.dousiyo.dpvptweaks.server.function;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class FunctionPaletteServerConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Enable the function palette GUI and its network handlers.")
            .define("enabled", true);

    public static final ForgeConfigSpec.IntValue REQUIRED_PERMISSION_LEVEL = BUILDER
            .comment("Permission level required to open and run functions from the palette.")
            .defineInRange("required_permission_level", 2, 0, 4);

    public static final ForgeConfigSpec.BooleanValue ALLOW_TAGS = BUILDER
            .comment("Reserved for future support of #function tags from the palette.")
            .define("allow_tags", false);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ALLOWED_NAMESPACES = BUILDER
            .comment("Allowlist of namespaces that can be executed from the palette. Use * to allow all.")
            .defineList("allowed_namespaces", List.of("*"), o -> o instanceof String);

    public static final ForgeConfigSpec.ConfigValue<String> DEFINITION_JSON = BUILDER
            .comment("JSON file name under .minecraft/dousiyo/ used for function palette dialog categories.")
            .define("definition_json", "function_palette_dialog.json");

    public static final ForgeConfigSpec.BooleanValue SHOW_RUN_RESULT = BUILDER
            .comment("Show a short confirmation message after a function is executed from the palette.")
            .define("show_run_result", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private FunctionPaletteServerConfig() {
    }
}
