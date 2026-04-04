package com.dousiyo.dpvptweaks.client.effect;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class OverlayPostRenderEvents {
    private OverlayPostRenderEvents() {
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        StaticImageOverlay.render(event.getGuiGraphics(), screenWidth, screenHeight);
        DvdOverlay.render(event.getGuiGraphics(), screenWidth, screenHeight);
    }
}
