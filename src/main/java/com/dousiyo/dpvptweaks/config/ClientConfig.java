package com.dousiyo.dpvptweaks.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.concurrent.ThreadLocalRandom;

public class ClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public enum LoadoutThemeMode {
        RANDOM(""),
        DEFAULT("default"),
        BLACK_CYAN("black_cyan"),
        GUNMETAL_RED("gunmetal_red"),
        OLIVE_AMBER("olive_amber"),
        STEEL_BLUE("steel_blue"),
        SUBDUED_OD("subdued_od");

        private final String folder;

        LoadoutThemeMode(String folder) {
            this.folder = folder;
        }

        public String resolveFolder() {
            if (this != RANDOM) {
                return folder;
            }
            LoadoutThemeMode[] fixed = {
                    DEFAULT, BLACK_CYAN, GUNMETAL_RED, OLIVE_AMBER, STEEL_BLUE, SUBDUED_OD
            };
            return fixed[ThreadLocalRandom.current().nextInt(fixed.length)].folder;
        }
    }

    public static final ForgeConfigSpec.EnumValue<LoadoutThemeMode> LOADOUT_THEME_MODE = BUILDER
            .comment("""
                    Theme mode for loadout GUI textures.
                    RANDOM: choose one of the 6 themes each time the GUI opens.
                    Others: fix to that theme color set.""")
            .defineEnum("loadoutThemeMode", LoadoutThemeMode.RANDOM);

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}

