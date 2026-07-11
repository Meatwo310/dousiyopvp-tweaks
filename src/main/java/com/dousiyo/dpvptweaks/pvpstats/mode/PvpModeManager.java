package com.dousiyo.dpvptweaks.pvpstats.mode;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PvpModeManager {
    private static volatile Map<String, PvpModeDefinition> definitions = Map.of();

    private PvpModeManager() {
    }

    public static void replace(Map<String, PvpModeDefinition> loaded) {
        definitions = Map.copyOf(loaded == null ? Map.of() : loaded);
    }

    public static PvpModeDefinition get(String modeId) {
        return modeId == null ? null : definitions.get(modeId.toLowerCase());
    }

    public static List<PvpModeDefinition> sortedDefinitions() {
        return definitions.values().stream()
                .sorted(Comparator.comparingInt(PvpModeDefinition::sortOrder).thenComparing(PvpModeDefinition::modeId))
                .toList();
    }

    public static Map<String, PvpModeDefinition> snapshot() {
        return new LinkedHashMap<>(definitions);
    }
}
