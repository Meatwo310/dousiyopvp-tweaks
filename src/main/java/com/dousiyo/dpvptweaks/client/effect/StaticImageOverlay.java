package com.dousiyo.dpvptweaks.client.effect;

import com.dousiyo.dpvptweaks.effect.ModEffects;
import com.dousiyo.dpvptweaks.effect.OverlayImageEffect;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class StaticImageOverlay {
    private StaticImageOverlay() {
    }

    public static void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null
                || !mc.player.hasEffect(ModEffects.STATIC_OVERLAY_EFFECT.get())) {
            return;
        }

        int x = Math.max(0, (screenWidth - OverlayImageEffect.HUD_DRAW_WIDTH) / 2);
        int y = Math.max(0, (screenHeight - OverlayImageEffect.HUD_DRAW_HEIGHT) / 2);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(OverlayImageEffect.TEXTURE, x, y,
                OverlayImageEffect.HUD_DRAW_WIDTH, OverlayImageEffect.HUD_DRAW_HEIGHT,
                0, 0,
                OverlayImageEffect.TEXTURE_WIDTH, OverlayImageEffect.TEXTURE_HEIGHT,
                OverlayImageEffect.TEXTURE_WIDTH, OverlayImageEffect.TEXTURE_HEIGHT);
        RenderSystem.disableBlend();
    }
}
