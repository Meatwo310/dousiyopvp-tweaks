package com.dousiyo.dpvptweaks.temporarybuilding;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Exact temporary-block mining timings while retaining vanilla mining progress and animation. */
@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TemporaryBuildingToolEvents {
    private TemporaryBuildingToolEvents() {}

    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        boolean wood = event.getState().is(ModTemporaryBlocks.WOOD.get());
        boolean stone = event.getState().is(ModTemporaryBlocks.STONE.get());
        if (!wood && !stone) return;
        if (event.getEntity().getMainHandItem().is(Items.IRON_PICKAXE)) {
            event.setNewSpeed(wood ? 6.0F : 4.0F); // 0.5 s / 1.5 s with configured hardness.
        } else if (event.getEntity().getMainHandItem().is(Items.NETHERITE_PICKAXE)) {
            event.setNewSpeed(wood ? 30.0F : 12.0F); // 0.1 s / 0.5 s.
        }
    }
}
