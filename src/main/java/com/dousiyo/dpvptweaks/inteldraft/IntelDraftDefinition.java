package com.dousiyo.dpvptweaks.inteldraft;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/** Immutable client payload and server-side draft definition types. */
public record IntelDraftDefinition(long sessionId, int remainingRerolls, long expiresAtMillis,
                                   boolean closeAllowed, List<String> acquiredTechNames, List<ChoiceDefinition> choices) {
    public IntelDraftDefinition {
        remainingRerolls = Math.max(0, remainingRerolls);
        acquiredTechNames = List.copyOf(acquiredTechNames);
        choices = List.copyOf(choices);
    }

    public IntelDraftDefinition(long sessionId, int remainingRerolls, long expiresAtMillis,
                                List<String> acquiredTechNames, List<ChoiceDefinition> choices) {
        this(sessionId, remainingRerolls, expiresAtMillis, false, acquiredTechNames, choices);
    }

    public static IntelDraftDefinition empty() {
        return new IntelDraftDefinition(0L, 0, 0L, false, List.of(), List.of());
    }

    public record ChoiceDefinition(TechDefinition tech, GunDefinition gun,
                                   AttachmentDefinition attachment) {
    }

    public record TechDefinition(ResourceLocation id, String name, String description,
                                 ItemStack iconStack, EffectDefinition effect,
                                 ResourceLocation onSelectFunction) {
        public TechDefinition {
            iconStack = iconStack == null ? ItemStack.EMPTY : iconStack.copy();
            effect = effect == null ? EffectDefinition.NONE : effect;
        }

        public boolean isSupplyOnly() {
            return id == null;
        }
    }

    public record EffectDefinition(String type, Map<String, Double> values) {
        public static final EffectDefinition NONE = new EffectDefinition("none", Map.of());

        public EffectDefinition {
            type = type == null ? "none" : type.trim().toLowerCase(java.util.Locale.ROOT);
            values = values == null ? Map.of() : Map.copyOf(values);
        }

        public double value(String key, double fallback) {
            return values.getOrDefault(key, fallback);
        }
    }

    public record GunDefinition(ResourceLocation id, String name, ItemStack gunStack) {
        public GunDefinition {
            gunStack = gunStack == null ? ItemStack.EMPTY : gunStack.copy();
        }
    }

    public record AttachmentDefinition(ResourceLocation id, String name, ItemStack attachmentStack) {
        public AttachmentDefinition {
            attachmentStack = attachmentStack == null ? ItemStack.EMPTY : attachmentStack.copy();
        }
    }

    public record AmmoDefinition(ResourceLocation id, int onSelect, int onRespawn, int onElimination) {
        public AmmoDefinition {
            onSelect = Math.max(0, onSelect);
            onRespawn = Math.max(0, onRespawn);
            onElimination = Math.max(0, onElimination);
        }
    }

    public record Pool(int sessionSeconds, int rerollCount, List<TechDefinition> techs,
                       List<GunDefinition> guns, List<AttachmentDefinition> attachments,
                       List<AmmoDefinition> ammo) {
        public Pool {
            sessionSeconds = Math.max(5, sessionSeconds);
            rerollCount = Math.max(0, rerollCount);
            techs = List.copyOf(techs);
            guns = List.copyOf(guns);
            attachments = List.copyOf(attachments);
            ammo = List.copyOf(ammo);
        }

        public static Pool empty() {
            return new Pool(30, 1, List.of(), List.of(), List.of(), List.of());
        }
    }
}
