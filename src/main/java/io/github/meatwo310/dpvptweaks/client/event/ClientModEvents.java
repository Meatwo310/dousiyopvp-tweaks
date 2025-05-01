package io.github.meatwo310.dpvptweaks.client.event;

import io.github.meatwo310.dpvptweaks.DpvpTweaks;
import io.github.meatwo310.dpvptweaks.entity.ModEntities;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.THROWN_VALINE1G.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_VALINE2G.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_VALINE3G.get(), ThrownItemRenderer::new);
    }
}
