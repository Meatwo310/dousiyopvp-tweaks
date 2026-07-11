package com.dousiyo.dpvptweaks.inteldraft;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public record IntelDraftDefinition(long sessionId, int remainingRerolls, List<ChoiceDefinition> choices) {
    public IntelDraftDefinition {
        remainingRerolls = Math.max(0, remainingRerolls);
        choices = List.copyOf(choices);
    }

    public static IntelDraftDefinition empty() {
        return new IntelDraftDefinition(0L, 0, List.of());
    }

    public record ChoiceDefinition(TechDefinition tech, GunDefinition gun) {
    }

    public record TechDefinition(int id, String name, String description, ItemStack iconStack) {
        public TechDefinition {
            iconStack = iconStack == null ? ItemStack.EMPTY : iconStack.copy();
        }
    }

    public record GunDefinition(int id, String name, ItemStack gunStack) {
        public GunDefinition {
            gunStack = gunStack == null ? ItemStack.EMPTY : gunStack.copy();
        }
    }
}
