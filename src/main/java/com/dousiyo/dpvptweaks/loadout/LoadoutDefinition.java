package com.dousiyo.dpvptweaks.loadout;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record LoadoutDefinition(
        String id,
        String name,
        String weapons,
        List<ItemStack> gunStacks,
        String description,
        List<String> teams,
        ResourceLocation applyFunction
) {
    public LoadoutDefinition(String id, String name, String weapons, List<ItemStack> gunStacks, String description) {
        this(id, name, weapons, gunStacks, description, List.of(), null);
    }

    public LoadoutDefinition {
        gunStacks = List.copyOf(gunStacks);
        teams = teams == null ? List.of() : List.copyOf(teams);
    }
}
