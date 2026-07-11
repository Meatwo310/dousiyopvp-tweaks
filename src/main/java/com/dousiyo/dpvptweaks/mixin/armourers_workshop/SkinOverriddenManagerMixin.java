package com.dousiyo.dpvptweaks.mixin.armourers_workshop;

import moe.plushie.armourers_workshop.core.client.other.SkinOverriddenManager;
import moe.plushie.armourers_workshop.core.utils.OpenEquipmentSlot;
import moe.plushie.armourers_workshop.core.utils.OpenItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SkinOverriddenManager.class, remap = false)
public class SkinOverriddenManagerMixin<T> {
    @Inject(method = "willRender", at = @At("HEAD"), cancellable = true, require = 0)
    private void forceRender(T source, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = {"overrideEquipment", "shouldOverrideEquipment"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void forceRenderEquipment(OpenEquipmentSlot slot, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = {"overrideAnyModel", "shouldOverrideAnyModel"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void forceRenderModel(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "overrideHandModel", at = @At("HEAD"), cancellable = true, require = 0)
    private void forceRenderHandModel(OpenItemDisplayContext context, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
