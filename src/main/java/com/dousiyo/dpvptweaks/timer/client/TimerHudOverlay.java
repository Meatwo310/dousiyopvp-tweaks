package com.dousiyo.dpvptweaks.timer.client;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.timer.config.TimerClientConfig;
import com.dousiyo.dpvptweaks.timer.core.TimerMode;
import com.dousiyo.dpvptweaks.timer.core.TimerState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

public final class TimerHudOverlay {
    private static final ResourceLocation FRAME_TEXTURE =
            new ResourceLocation(DpvpTweaks.MODID, "textures/gui/timer/timer_frame.png");

    private static final int TEXTURE_WIDTH = 512;
    private static final int TEXTURE_HEIGHT = 112;
    private static final int FRAME_WIDTH = 256;
    private static final int FRAME_HEIGHT = 56;
    private static final int HUD_Y = 6;
    private static final int BAR_X = 18;
    private static final int BAR_Y = 43;
    private static final int BAR_WIDTH = 220;
    private static final float TITLE_SCALE = 0.72F;
    private static final float TIME_SCALE = 1.28F;

    private TimerHudOverlay() {}

    public static void render(GuiGraphics gui, int screenW, int screenH) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.font == null || !ClientTimerState.isVisible()
                || mc.options.keyPlayerList.isDown()) {
            return;
        }

        int x = computeX(screenW);
        if (ClientTimerState.isFinishAnimating(mc)) {
            renderFinishAnimation(gui, mc, x);
            return;
        }
        if (ClientTimerState.getMode() == TimerMode.COUNTDOWN
                && ClientTimerState.getState() == TimerState.FINISHED) {
            return;
        }

        int ticks = ClientTimerState.getDisplayTicks(mc);
        boolean countdown = ClientTimerState.getMode() == TimerMode.COUNTDOWN;
        int accentColor = countdown ? resolveCountdownAlertColor(ClientTimerState.getDurationTicks(), ticks) : 0xFF29D9EA;

        drawFrame(gui, x, HUD_Y, 1.0F);
        drawStatusRail(gui, x, HUD_Y, ticks, countdown, accentColor);

        Component title = ClientTimerState.getTitle();
        String titleText = title == null || title.getString().isBlank()
                ? ClientTimerState.getTimerId()
                : title.getString();
        titleText = trimToWidth(mc, titleText, 112);

        String modeText = countdown ? "COUNTDOWN" : "STOPWATCH";
        gui.drawString(mc.font, modeText, x + 18, HUD_Y + 12, 0xFF708493, false);

        gui.pose().pushPose();
        gui.pose().translate(x + 18, HUD_Y + 25, 0.0F);
        gui.pose().scale(TITLE_SCALE, TITLE_SCALE, 1.0F);
        gui.drawString(mc.font, titleText, 0, 0, 0xFFE7F7FA, false);
        gui.pose().popPose();

        String timeText = formatTicks(ticks);
        int scaledTimeWidth = (int) Math.ceil(mc.font.width(timeText) * TIME_SCALE);
        gui.pose().pushPose();
        gui.pose().translate(x + FRAME_WIDTH - 18 - scaledTimeWidth, HUD_Y + 20, 0.0F);
        gui.pose().scale(TIME_SCALE, TIME_SCALE, 1.0F);
        gui.drawString(mc.font, timeText, 0, 0, accentColor, true);
        gui.pose().popPose();

        gui.fill(x + 142, HUD_Y + 17, x + 143, HUD_Y + 37, 0x5536D5E5);
    }

    private static void drawStatusRail(GuiGraphics gui, int x, int y, int ticks,
                                       boolean countdown, int accentColor) {
        gui.fill(x + BAR_X, y + BAR_Y, x + BAR_X + BAR_WIDTH, y + BAR_Y + 2, 0xCC16232B);
        if (!countdown || ClientTimerState.getDurationTicks() <= 0) {
            gui.fill(x + BAR_X, y + BAR_Y, x + BAR_X + BAR_WIDTH, y + BAR_Y + 1, accentColor);
            return;
        }

        int duration = ClientTimerState.getDurationTicks();
        int clampedTicks = Math.max(0, Math.min(duration, ticks));
        int fillWidth = Math.round(BAR_WIDTH * (clampedTicks / (float) duration));
        if (fillWidth > 0) {
            gui.fill(x + BAR_X, y + BAR_Y, x + BAR_X + fillWidth, y + BAR_Y + 2, accentColor);
        }
        int marker = x + BAR_X + fillWidth;
        gui.fill(marker - 1, y + BAR_Y - 1, marker + 1, y + BAR_Y + 3, 0xFFF2FCFF);
    }

    private static void renderFinishAnimation(GuiGraphics gui, Minecraft mc, int x) {
        float progress = ClientTimerState.getFinishAnimProgress(mc);
        float alpha = 1.0F - progress;
        int y = HUD_Y - Math.round(progress * 12.0F);
        drawFrame(gui, x, y, alpha);

        Component message = ClientTimerState.getFinishMessage();
        if (message == null || message.getString().isBlank()) {
            message = Component.literal("FINISHED");
        }
        String text = trimToWidth(mc, message.getString(), 190);
        float scale = 1.05F + progress * 0.08F;
        int width = (int) Math.ceil(mc.font.width(text) * scale);
        int color = (clamp(Math.round(alpha * 255.0F)) << 24) | 0xE9FBFF;

        gui.pose().pushPose();
        gui.pose().translate(x + (FRAME_WIDTH - width) / 2.0F, y + 22, 0.0F);
        gui.pose().scale(scale, scale, 1.0F);
        gui.drawString(mc.font, text, 0, 0, color, true);
        gui.pose().popPose();
    }

    private static void drawFrame(GuiGraphics gui, int x, int y, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        gui.blit(FRAME_TEXTURE, x, y, FRAME_WIDTH, FRAME_HEIGHT,
                0.0F, 0.0F, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static int computeX(int screenW) {
        if (TimerClientConfig.HUD_POSITION.get() == TimerClientConfig.HudPosition.TOP_RIGHT) {
            return screenW - FRAME_WIDTH - 10;
        }
        return (screenW - FRAME_WIDTH) / 2;
    }

    private static int resolveCountdownAlertColor(int durationTicks, int remainingTicks) {
        if (remainingTicks <= 10 * 20) {
            return 0xFFFF594D;
        }
        int amberThreshold = durationTicks >= 10 * 60 * 20 ? 60 * 20 : 30 * 20;
        if (remainingTicks <= amberThreshold) {
            return 0xFFFFB82E;
        }
        return 0xFF2DDBE8;
    }

    private static String trimToWidth(Minecraft mc, String value, int maxWidth) {
        if (mc.font.width(value) <= maxWidth) {
            return value;
        }
        String suffix = "...";
        return mc.font.plainSubstrByWidth(value, Math.max(0, maxWidth - mc.font.width(suffix))) + suffix;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static String formatTicks(int ticks) {
        long totalSeconds = Math.max(0L, ticks / 20L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }
}
