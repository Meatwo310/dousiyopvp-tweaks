package com.dousiyo.dpvptweaks.arsenal;

import java.util.List;

public record ArsenalWeaponSet(int schemaVersion, String id, String displayName, List<ArsenalWeaponStage> stages) {
    public static final int STAGE_COUNT = 30;

    public ArsenalWeaponSet {
        id = id == null ? "" : id;
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        stages = stages == null ? List.of() : List.copyOf(stages);
    }
}
