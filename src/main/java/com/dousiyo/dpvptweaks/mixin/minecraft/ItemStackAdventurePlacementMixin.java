package com.dousiyo.dpvptweaks.mixin.minecraft;

import com.dousiyo.dpvptweaks.temporarybuilding.TemporaryBuildingBlockItem;
import net.minecraft.core.Registry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only bypasses Adventure's CanPlaceOn gate; BlockItem still performs the complete vanilla placement. */
@Mixin(ItemStack.class)
public abstract class ItemStackAdventurePlacementMixin {
    @Inject(method = "hasAdventureModePlaceTagForBlock", at = @At("HEAD"), cancellable = true)
    private void allowTemporaryBuildingItems(Registry<Block> registry, BlockInWorld block,
                                             CallbackInfoReturnable<Boolean> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.getItem() instanceof TemporaryBuildingBlockItem) cir.setReturnValue(true);
    }
}
