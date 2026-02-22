package com.dousiyo.dpvptweaks.mixin.tacz;

import com.tacz.guns.client.event.RenderHeadShotAABB;
import net.minecraftforge.client.event.RenderLivingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderHeadShotAABB.class)
public class RenderHeadShotAABBMixin {
    @Inject(method = "onRenderEntity", at = @At("HEAD"), cancellable = true, remap = false)
    private static void noHeadShotAABB(RenderLivingEvent.Post<?, ?> event, CallbackInfo ci) {
        ci.cancel();
    }
}
