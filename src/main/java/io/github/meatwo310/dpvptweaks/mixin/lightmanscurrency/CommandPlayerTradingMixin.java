package io.github.meatwo310.dpvptweaks.mixin.lightmanscurrency;

import com.mojang.brigadier.context.CommandContext;
import io.github.lightman314.lightmanscurrency.common.commands.CommandPlayerTrading;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CommandPlayerTrading.class, remap = false)
public class CommandPlayerTradingMixin {
    @Inject(
            method = "requestPlayerTrade",
            at = @At(value = "INVOKE",
                    target = "Lio/github/lightman314/lightmanscurrency/common/playertrading/PlayerTradeManager;" +
                            "CreateNewTrade(" +
                            "Lnet/minecraft/server/level/ServerPlayer;" +
                            "Lnet/minecraft/server/level/ServerPlayer;" +
                            ")I"),
            cancellable = true
    )
    private static void onCreateNewTrade(CommandContext<CommandSourceStack> ctx, CallbackInfoReturnable<Integer> cir) {
        ctx.getSource().sendFailure(Component.literal("ﾇﾍﾞｰｯ!"));
        cir.setReturnValue(0);
    }
}
