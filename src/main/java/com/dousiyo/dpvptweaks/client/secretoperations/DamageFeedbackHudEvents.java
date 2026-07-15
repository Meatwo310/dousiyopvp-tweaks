package com.dousiyo.dpvptweaks.client.secretoperations;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DamageFeedbackHudEvents {
    private DamageFeedbackHudEvents() {}

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        ClientDamageFeedback.render(event.getGuiGraphics(),
                event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight());
    }
}
