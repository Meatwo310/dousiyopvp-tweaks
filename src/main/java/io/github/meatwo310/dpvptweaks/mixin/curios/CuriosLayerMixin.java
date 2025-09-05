package io.github.meatwo310.dpvptweaks.mixin.curios;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.lightman314.lightmanscurrency.client.renderer.entity.layers.WalletLayer;
import io.github.meatwo310.dpvptweaks.config.ServerConfig;
import io.github.meatwo310.dpvptweaks.config.WalletsRenderMode;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WalletLayer.class, remap = false)
public class CuriosLayerMixin<T extends LivingEntity, M extends EntityModel<T>, E> {
    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void skipRender(PoseStack matrixStack, MultiBufferSource renderTypeBuffer, int light, T livingEntity,
                            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                            float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (ServerConfig.WALLETS_RENDER_MODE.get() == WalletsRenderMode.NEVER) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(value = "INVOKE", target = "Lio/github/lightman314/lightmanscurrency/common/capability/wallet/IWalletHandler;visible()Z")
    )
    private boolean modifyVisible(boolean original) {
        return ServerConfig.WALLETS_RENDER_MODE.get() == WalletsRenderMode.ALWAYS || original;
    }
}
