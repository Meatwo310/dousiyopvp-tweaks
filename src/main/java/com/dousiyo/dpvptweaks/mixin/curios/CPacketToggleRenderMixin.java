package com.dousiyo.dpvptweaks.mixin.curios;

import com.dousiyo.dpvptweaks.config.ServerConfig;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.common.network.client.CPacketToggleRender;

import java.util.function.Supplier;

@Mixin(value = CPacketToggleRender.class, remap = false)
public class CPacketToggleRenderMixin {
    @SuppressWarnings("target")
    @Inject(
            method = "lambda$handle$3(Ljava/util/function/Supplier;Ltop/theillusivec4/curios/common/network/client/CPacketToggleRender;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void ignorePacket(Supplier<NetworkEvent.Context> ctx, CPacketToggleRender msg, CallbackInfo ci) {
        if (ServerConfig.CURIOS_IGNORE_RENDER_CHANGE.get()) {
            ci.cancel();
        }
    }
}
