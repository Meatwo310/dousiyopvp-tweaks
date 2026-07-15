package com.dousiyo.dpvptweaks.temporarybuilding;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

/** Marker item whose placement is allowed through vanilla BlockItem logic in Adventure mode. */
public final class TemporaryBuildingBlockItem extends BlockItem {
    public TemporaryBuildingBlockItem(Block block, Properties properties) {
        super(block, properties);
    }
}
