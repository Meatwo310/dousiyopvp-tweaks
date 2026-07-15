package com.dousiyo.dpvptweaks.client.event;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.entity.ModEntities;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.dousiyo.dpvptweaks.client.secretoperations.SecretConvoyTruckRenderer;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class EntityRenderersRegisterer {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.THROWN_VALINE1G.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_VALINE2G.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_VALINE3G.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.SECRET_CONVOY_TRUCK.get(), SecretConvoyTruckRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SecretConvoyTruckRenderer.LAYER,
                com.dousiyo.dpvptweaks.client.secretoperations.SecretConvoyTruckModel::createLayer);
    }
}
