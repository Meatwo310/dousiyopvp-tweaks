package io.github.meatwo310.dpvptweaks.mixin.minecraft;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Set;

@Mixin(DamageType.class)
public class DamageTypeMixin {
    @Unique
    private static final Set<String> dpvptweaks$EXPLOSIONS = Set.of("explosion", "explosion.player");

    @ModifyVariable(
            method = "<init>(Ljava/lang/String;" +
                    "Lnet/minecraft/world/damagesource/DamageScaling;" +
                    "FLnet/minecraft/world/damagesource/DamageEffects;" +
                    "Lnet/minecraft/world/damagesource/DeathMessageType;" +
                    ")V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private static DamageScaling disableExplosionScaling(DamageScaling scaling, @Local(argsOnly = true) String pMsgId) {
        return dpvptweaks$EXPLOSIONS.contains(pMsgId) ? DamageScaling.NEVER : scaling;
    }
}
