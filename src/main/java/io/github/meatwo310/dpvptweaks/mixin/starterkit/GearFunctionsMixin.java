package io.github.meatwo310.dpvptweaks.mixin.starterkit;

import com.natamus.collective_common_forge.functions.GearFunctions;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GearFunctions.class, remap = false)
public class GearFunctionsMixin {
    @Redirect(
            method = "setPlayerGearFromGearString(Lnet/minecraft/world/entity/player/Player;Ljava/lang/String;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Inventory;clearContent()V",
                    remap = true
            ),
            remap = false
    )
    private static void onEmptiedInventory(Inventory inventory) {
        // Clear the inventory but keep the armor
        inventory.items.clear();
        inventory.offhand.clear();
    }
}
