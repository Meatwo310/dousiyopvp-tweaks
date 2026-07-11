package com.dousiyo.dpvptweaks.pvpstats.mode;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.Locale;

public record PvpModeDefinition(
        String modeId,
        String displayName,
        String translationKey,
        String description,
        String descriptionTranslationKey,
        ResourceLocation icon,
        int sortOrder,
        Set<String> tags,
        boolean visible,
        boolean rankingEnabled,
        long rankingMinMatches,
        long rankingMinKills
) {
    public PvpModeDefinition {
        modeId = modeId == null ? "" : modeId.trim().toLowerCase(Locale.ROOT);
        displayName = displayName == null || displayName.isBlank() ? modeId : displayName.trim();
        translationKey = translationKey == null ? "" : translationKey.trim();
        description = description == null ? "" : description.trim();
        descriptionTranslationKey = descriptionTranslationKey == null ? "" : descriptionTranslationKey.trim();
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        rankingMinMatches = Math.max(0L, rankingMinMatches);
        rankingMinKills = Math.max(0L, rankingMinKills);
    }

    public boolean hasTag(String tag) {
        return tag != null && tags.contains(tag.toLowerCase(Locale.ROOT));
    }
}
