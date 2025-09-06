package io.github.meatwo310.dpvptweaks.mixin.armourers_workshop;

import moe.plushie.armourers_workshop.core.client.other.SkinOverriddenManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SkinOverriddenManager.class, remap = false)
public class SkinOverriddenManagerMixin<T> {
    @Inject(method = "willRender", at = @At("HEAD"), cancellable = true)
    private void forceRender(T source, CallbackInfo ci) {
        ci.cancel();
    }
}
