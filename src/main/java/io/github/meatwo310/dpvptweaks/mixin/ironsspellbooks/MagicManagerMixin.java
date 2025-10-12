package io.github.meatwo310.dpvptweaks.mixin.ironsspellbooks;

import io.github.meatwo310.dpvptweaks.config.ManaRegenBehaviour;
import io.github.meatwo310.dpvptweaks.config.ServerConfig;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MagicManager.class)
public class MagicManagerMixin {
    @ModifyVariable(method = "regenPlayerMana", at = @At("STORE"), ordinal = 0, remap = false)
    private int modifyPlayerMaxMana(int playerMaxMana) {
        if (ServerConfig.IRONS_OVERRIDE_ATTRIBUTES.get()) {
            int maxMana = ServerConfig.IRONS_MAX_MANA.get();
            if (maxMana > 0) {
                return maxMana;
            }
        }
        return playerMaxMana;
    }

    @ModifyVariable(method = "regenPlayerMana", at = @At("STORE"), ordinal = 2, remap = false)
    private float modifyIncrement(float increment) {
        if (ServerConfig.IRONS_OVERRIDE_ATTRIBUTES.get()) {
            if (ServerConfig.IRONS_MANA_REGEN.get() == ManaRegenBehaviour.FIXED) {
                return ServerConfig.IRONS_MANA_REGEN_VALUE.get();
            }
        }
        return increment;
    }

    @ModifyVariable(method = "tick", at = @At("STORE"), ordinal = 0, remap = false)
    private boolean modifyDoManaRegen(boolean doManaRegen) {
        if (ServerConfig.IRONS_OVERRIDE_ATTRIBUTES.get()) {
            if (ServerConfig.IRONS_MANA_REGEN.get() == ManaRegenBehaviour.NEVER) {
                return false;
            }
        }
        return doManaRegen;
    }
}
