package com.dousiyo.dpvptweaks.pvpstats.badge;

import java.util.List;

/** Catalog of achievements that can be awarded by server operators. */
public final class BadgeSystem {
    public static final List<BadgeDefinition> DEFINITIONS = List.of(
            badge("debugger"),
            badge("supporter")
    );

    private BadgeSystem() {
    }

    private static BadgeDefinition badge(String id) {
        return new BadgeDefinition(id, "gui.dpvptweaks.combat_record.badge." + id);
    }

    public static boolean contains(String id) {
        return DEFINITIONS.stream().anyMatch(definition -> definition.id().equals(id));
    }
}
