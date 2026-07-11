package com.dousiyo.dpvptweaks.pvpstats.badge;

import java.util.List;

/** Display-only badge catalog. Award and progress systems are intentionally absent. */
public final class BadgeSystem {
    public static final List<BadgeDefinition> DEFINITIONS = List.of(
            badge("first_blood"),
            badge("hunter"),
            badge("executioner"),
            badge("legendary_slayer"),
            badge("victor"),
            badge("veteran")
    );

    private BadgeSystem() {
    }

    private static BadgeDefinition badge(String id) {
        return new BadgeDefinition(id, "gui.dpvptweaks.combat_record.badge." + id);
    }
}
