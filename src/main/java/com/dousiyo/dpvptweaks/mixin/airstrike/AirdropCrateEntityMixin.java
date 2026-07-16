package com.dousiyo.dpvptweaks.mixin.airstrike;

import com.dousiyo.airstrike.entity.AirdropCrateEntity;
import com.dousiyo.dpvptweaks.secretoperations.SecretShowdownSupplyManager;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AirdropCrateEntity.class)
public abstract class AirdropCrateEntityMixin {
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void dpvptweaks$protectShowdownSupply(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        AirdropCrateEntity crate = (AirdropCrateEntity) (Object) this;
        if (SecretShowdownSupplyManager.isMarked(crate) && !SecretShowdownSupplyManager.isUnlocked(crate))
            cir.setReturnValue(false);
    }
}
