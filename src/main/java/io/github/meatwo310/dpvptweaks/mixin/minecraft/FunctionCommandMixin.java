package io.github.meatwo310.dpvptweaks.mixin.minecraft;

import net.minecraft.server.commands.FunctionCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(FunctionCommand.class)
public class FunctionCommandMixin {
    @ModifyConstant(
            method = "runFunction(" +
                    "Lnet/minecraft/commands/CommandSourceStack;" +
                    "Ljava/util/Collection;" +
                    ")I",
            constant = @Constant(intValue = 1),
            slice = @Slice(from = @At(
                    value = "CONSTANT",
                    args = "intValue=1",
                    ordinal = 0
            ))
    )
    private static int notifyOperators(int original) {
        return 0;
    }
}
