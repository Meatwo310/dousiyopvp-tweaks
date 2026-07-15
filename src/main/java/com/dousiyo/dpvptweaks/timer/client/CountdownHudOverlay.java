package com.dousiyo.dpvptweaks.timer.client;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class CountdownHudOverlay {
    private static final ResourceLocation FRAME_TEXTURE =
            new ResourceLocation(DpvpTweaks.MODID, "textures/gui/timer/countdown_frame.png");

    private static final int TEXTURE_WIDTH = 640;
    private static final int TEXTURE_HEIGHT = 208;
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 104;
    private static final int BAR_X = 72;
    private static final int BAR_Y = 91;
    private static final int BAR_WIDTH = 176;

    private CountdownHudOverlay() {}

    public static void render(GuiGraphics gui, int screenW, int screenH) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.font == null || !ClientCountdownState.isVisible()) {
            return;
        }

        ClientCountdownState.tick(mc);
        if (!ClientCountdownState.isVisible()) {
            return;
        }

        int x = (screenW - PANEL_WIDTH) / 2;
        int y = (screenH - PANEL_HEIGHT) / 2 - 12;
        if (ClientCountdownState.isFinished()) {
            renderFinish(gui, mc, x, y);
        } else {
            renderRunning(gui, mc, x, y);
        }
    }

    private static void renderRunning(GuiGraphics gui, Minecraft mc, int x, int y) {
        float intro = easeOutCubic(ClientCountdownState.getRunningAnimProgress(mc));
        float frameScale = 0.94F + intro * 0.06F;

        gui.pose().pushPose();
        gui.pose().translate(x + PANEL_WIDTH / 2.0F, y + PANEL_HEIGHT / 2.0F, 0.0F);
        gui.pose().scale(frameScale, frameScale, 1.0F);
        gui.pose().translate(-(x + PANEL_WIDTH / 2.0F), -(y + PANEL_HEIGHT / 2.0F), 0.0F);

        gui.fill(x + 20, y + 29, x + PANEL_WIDTH - 20, y + 85, 0xB0060D12);
        drawFrame(gui, x, y, intro);

        int ticks = ClientCountdownState.getDisplayTicks(mc);
        int seconds = ClientCountdownState.getDisplaySeconds(mc);
        int durationTicks = Math.max(1, ClientCountdownState.getDurationTicks());
        int accentColor = resolveAccentColor(seconds);
        int barFill = Math.round(BAR_WIDTH * Math.max(0.0F, Math.min(1.0F, ticks / (float) durationTicks)));

        gui.fill(x + BAR_X, y + BAR_Y, x + BAR_X + BAR_WIDTH, y + BAR_Y + 2, 0xDD101A20);
        if (barFill > 0) {
            gui.fill(x + BAR_X, y + BAR_Y, x + BAR_X + barFill, y + BAR_Y + 2, accentColor);
        }

        String header = seconds <= 5 ? "FINAL COUNT" : "COUNTDOWN";
        int headerX = x + (PANEL_WIDTH - mc.font.width(header)) / 2;
        gui.drawString(mc.font, header, headerX, y + 18, 0xFF91A7B4, false);

        long now = mc.level != null ? mc.level.getGameTime() : 0L;
        float pulse = seconds <= 5 ? (float) Math.sin(now * 0.62F) * 0.10F : 0.0F;
        float numberScale = (seconds <= 5 ? 4.7F : 4.15F) + pulse;
        Component number = Component.literal(seconds > 9 ? String.format("%02d", seconds) : Integer.toString(seconds));
        int numberWidth = (int) Math.ceil(mc.font.width(number) * numberScale);

        gui.pose().pushPose();
        gui.pose().translate(x + (PANEL_WIDTH - numberWidth) / 2.0F, y + 36, 0.0F);
        gui.pose().scale(numberScale, numberScale, 1.0F);
        gui.drawString(mc.font, number, 0, 0, accentColor, true);
        gui.pose().popPose();

        String status = seconds <= 3 ? "GET READY" : "STANDBY";
        int statusX = x + (PANEL_WIDTH - mc.font.width(status)) / 2;
        gui.drawString(mc.font, status, statusX, y + 77, 0xFF607785, false);

        gui.fill(x + 42, y + 50, x + 59, y + 52, accentColor);
        gui.fill(x + PANEL_WIDTH - 59, y + 50, x + PANEL_WIDTH - 42, y + 52, accentColor);
        gui.pose().popPose();
    }

    private static void renderFinish(GuiGraphics gui, Minecraft mc, int x, int y) {
        float progress = ClientCountdownState.getFinishAnimProgress(mc);
        float alpha = 1.0F - progress;
        float scale = 1.0F + progress * 0.10F;

        gui.pose().pushPose();
        gui.pose().translate(x + PANEL_WIDTH / 2.0F, y + PANEL_HEIGHT / 2.0F, 0.0F);
        gui.pose().scale(scale, scale, 1.0F);
        gui.pose().translate(-(x + PANEL_WIDTH / 2.0F), -(y + PANEL_HEIGHT / 2.0F), 0.0F);
        gui.fill(x + 20, y + 29, x + PANEL_WIDTH - 20, y + 85,
                (clamp(Math.round(alpha * 184.0F)) << 24) | 0x061116);
        drawFrame(gui, x, y, alpha);

        Component title = Component.literal("GO!");
        float titleScale = 4.8F;
        int titleWidth = (int) Math.ceil(mc.font.width(title) * titleScale);
        int color = (clamp(Math.round(alpha * 255.0F)) << 24) | 0xE9FDFF;
        gui.pose().pushPose();
        gui.pose().translate(x + (PANEL_WIDTH - titleWidth) / 2.0F, y + 37, 0.0F);
        gui.pose().scale(titleScale, titleScale, 1.0F);
        gui.drawString(mc.font, title, 0, 0, color, true);
        gui.pose().popPose();

        String subtitle = "START";
        int subtitleX = x + (PANEL_WIDTH - mc.font.width(subtitle)) / 2;
        gui.drawString(mc.font, subtitle, subtitleX, y + 79,
                (clamp(Math.round(alpha * 255.0F)) << 24) | 0x42E4ED, false);
        gui.pose().popPose();
    }

    private static void drawFrame(GuiGraphics gui, int x, int y, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.max(0.0F, Math.min(1.0F, alpha)));
        gui.blit(FRAME_TEXTURE, x, y, PANEL_WIDTH, PANEL_HEIGHT,
                0.0F, 0.0F, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static int resolveAccentColor(int seconds) {
        if (seconds <= 2) {
            return 0xFFFF594D;
        }
        if (seconds <= 5) {
            return 0xFFFFB82E;
        }
        return 0xFF2DDBE8;
    }

    private static float easeOutCubic(float value) {
        float inverse = 1.0F - Math.max(0.0F, Math.min(1.0F, value));
        return 1.0F - inverse * inverse * inverse;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
