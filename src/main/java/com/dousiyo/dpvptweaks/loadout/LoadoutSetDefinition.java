package com.dousiyo.dpvptweaks.loadout;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record LoadoutSetDefinition(ResourceLocation id, String title, String layout, List<ResourceLocation> loadouts) {
    public LoadoutSetDefinition {
        title = title == null || title.isBlank() ? id.toString() : title;
        layout = layout == null || layout.isBlank() ? "normal" : layout.trim();
        loadouts = loadouts == null ? List.of() : List.copyOf(loadouts);
    }

    public boolean isMiniLayout() {
        return "mini".equalsIgnoreCase(layout);
    }
}
