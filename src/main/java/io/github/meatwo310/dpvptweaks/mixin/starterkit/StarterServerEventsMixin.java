package io.github.meatwo310.dpvptweaks.mixin.starterkit;

import com.natamus.starterkit_common_forge.events.StarterServerEvents;
import io.github.meatwo310.dpvptweaks.config.ServerConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StarterServerEvents.class, remap = false)
public class StarterServerEventsMixin {
    @Inject(at = @At("HEAD"), cancellable = true, method = "onSpawn(" +
            "Lnet/minecraft/world/level/Level;" +
            "Lnet/minecraft/world/entity/Entity;" +
            ")V")
    private static void onSpawn(Level level, Entity entity, CallbackInfo ci) {
        if (ServerConfig.DISABLE_STARTER_KIT.get()) {
            ci.cancel();
        }
    }
}
