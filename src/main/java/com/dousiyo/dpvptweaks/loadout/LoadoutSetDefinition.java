package com.dousiyo.dpvptweaks.loadout;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** A datapack-defined ordered view over command-saved loadouts. */
public record LoadoutSetDefinition(ResourceLocation id, String displayName, List<Entry> loadouts) {
    public LoadoutSetDefinition {
        displayName = displayName == null || displayName.isBlank() ? id.getPath() : displayName.trim();
        loadouts = loadouts == null ? List.of() : List.copyOf(loadouts);
    }

    public record Entry(String id, String displayName, String description, ResourceLocation afterApply, RandomDefinition random) {
        public Entry {
            id = id == null ? "" : id.trim();
            displayName = displayName == null || displayName.isBlank() ? id : displayName.trim();
            description = description == null ? "" : description;
        }

        public boolean isRandom() {
            return random != null;
        }
    }

    public record RandomDefinition(String profile, String template, int weaponCount) {}
}
