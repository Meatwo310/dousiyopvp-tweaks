package com.dousiyo.dpvptweaks.client.secretoperations;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SecretShowdownHudModEvents {
    private SecretShowdownHudModEvents() {}

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("secret_showdown_score", (gui, graphics, partialTick, width, height) ->
                SecretShowdownHudEvents.render(graphics, width, height));
        event.registerAboveAll("secret_convoy_escort", (gui, graphics, partialTick, width, height) ->
                SecretConvoyHudEvents.render(graphics, width, height));
    }
}
