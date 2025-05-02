package io.github.meatwo310.dpvptweaks.mixin.modernui;

import icyllis.modernui.mc.forge.UIManagerForge;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.annotation.Nonnull;

@Mixin(value = UIManagerForge.class, remap = false)
public class UIManagerForgeMixin {
    /**
     * @author Meatwo310
     * @reason Blocks zoom feature
     */
    @Overwrite
    @SubscribeEvent
    void onChangeFov(@Nonnull ViewportEvent.ComputeFov event) {
    }
}
