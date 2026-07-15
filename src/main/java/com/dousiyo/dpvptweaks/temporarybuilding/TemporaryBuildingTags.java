package com.dousiyo.dpvptweaks.temporarybuilding;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class TemporaryBuildingTags {
    public static final TagKey<Block> TEMPORARY_BLOCKS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(DpvpTweaks.MODID, "temporary_blocks"));

    private TemporaryBuildingTags() {}
}
