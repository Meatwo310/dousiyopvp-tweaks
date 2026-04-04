package com.dousiyo.dpvptweaks.effect;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

import java.util.function.Consumer;

public abstract class OverlayImageEffect extends MobEffect {
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(DpvpTweaks.MODID, "textures/gui/dvd_logo.png");
    public static final int TEXTURE_WIDTH = 128;
    public static final int TEXTURE_HEIGHT = 128;
    public static final int HUD_DRAW_WIDTH = 48;
    public static final int HUD_DRAW_HEIGHT = 48;
    private static final int ICON_DRAW_SIZE = 18;

    protected OverlayImageEffect() {
        super(MobEffectCategory.NEUTRAL, 0xF4F4F4);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    @Override
    public void initializeClient(Consumer<IClientMobEffectExtensions> consumer) {
        consumer.accept(new IClientMobEffectExtensions() {
            @Override
            public boolean renderInventoryIcon(MobEffectInstance instance, EffectRenderingInventoryScreen<?> screen,
                                               GuiGraphics guiGraphics, int x, int y, int blitOffset) {
                renderIcon(guiGraphics, x, y, 1.0F);
                return true;
            }

            @Override
            public boolean renderGuiIcon(MobEffectInstance instance, Gui gui, GuiGraphics guiGraphics,
                                         int x, int y, float z, float alpha) {
                renderIcon(guiGraphics, x + 3, y + 3, alpha);
                return true;
            }

            private static void renderIcon(GuiGraphics guiGraphics, int x, int y, float alpha) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
                guiGraphics.blit(TEXTURE, x, y, ICON_DRAW_SIZE, ICON_DRAW_SIZE,
                        0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.disableBlend();
            }
        });
    }
}
