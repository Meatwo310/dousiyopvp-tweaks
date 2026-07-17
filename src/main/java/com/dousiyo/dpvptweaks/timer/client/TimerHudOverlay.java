package com.dousiyo.dpvptweaks.timer.client;

import com.dousiyo.dpvptweaks.timer.config.TimerClientConfig;
import com.dousiyo.dpvptweaks.timer.core.TimerMode;
import com.dousiyo.dpvptweaks.timer.core.TimerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class TimerHudOverlay {
    private static final int HUD_WIDTH = 184;
    private static final int HUD_HEIGHT = 23;
    private static final int HUD_Y = 5;
    private static final int PADDING = 7;
    private static final int PROGRESS_HEIGHT = 3;
    private static final float TITLE_SCALE = 0.78F;

    private TimerHudOverlay() {}

    public static void render(GuiGraphics gui, int screenW, int screenH) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.font == null || !ClientTimerState.isVisible()
                || mc.options.keyPlayerList.isDown()) {
            return;
        }

        if (ClientTimerState.getMode() == TimerMode.COUNTDOWN
                && ClientTimerState.getState() == TimerState.FINISHED
                && !ClientTimerState.isFinishAnimating(mc)) {
            return;
        }

        float hudScale = TimerClientConfig.HUD_SCALE.get().floatValue();
        int x = computeX(screenW, hudScale);
        gui.pose().pushPose();
        gui.pose().translate(x, 0.0F, 0.0F);
        gui.pose().scale(hudScale, hudScale, 1.0F);
        try {
            if (ClientTimerState.isFinishAnimating(mc)) {
                renderFinishAnimation(gui, mc);
            } else {
                renderTimer(gui, mc);
            }
        } finally {
            gui.pose().popPose();
        }
    }

    private static void renderTimer(GuiGraphics gui, Minecraft mc) {
        int ticks = ClientTimerState.getDisplayTicks(mc);
        boolean countdown = ClientTimerState.getMode() == TimerMode.COUNTDOWN;
        int accentColor = countdown
                ? resolveCountdownAlertColor(ClientTimerState.getDurationTicks(), ticks)
                : 0xFF49C6D4;

        drawPanel(gui, HUD_Y, accentColor, 255);
        drawStateIndicator(gui, HUD_Y, accentColor);

        String timeText = formatTicks(ticks);
        int timeWidth = mc.font.width(timeText);
        int timeX = HUD_WIDTH - PADDING - timeWidth;
        gui.drawString(mc.font, timeText, timeX, HUD_Y + 6, 0xFFF4F7F8, false);

        Component title = ClientTimerState.getTitle();
        String titleText = title == null || title.getString().isBlank()
                ? ClientTimerState.getTimerId()
                : title.getString();
        int titleStart = PADDING + 9;
        int titleRoom = Math.max(0, timeX - titleStart - 8);
        titleText = trimToWidth(mc, titleText, Math.round(titleRoom / TITLE_SCALE));

        gui.pose().pushPose();
        gui.pose().translate(titleStart, HUD_Y + 7, 0.0F);
        gui.pose().scale(TITLE_SCALE, TITLE_SCALE, 1.0F);
        gui.drawString(mc.font, titleText, 0, 0, 0xFFD3DADD, false);
        gui.pose().popPose();

        drawProgress(gui, HUD_Y, ticks, countdown, accentColor);
    }

    private static void drawPanel(GuiGraphics gui, int y, int accentColor, int alpha) {
        int shadowAlpha = Math.round(alpha * 0.38F);
        int panelAlpha = Math.round(alpha * 0.82F);
        int borderAlpha = Math.round(alpha * 0.32F);
        gui.fill(1, y + 1, HUD_WIDTH + 1, y + HUD_HEIGHT + 1, (shadowAlpha << 24));
        gui.fill(0, y, HUD_WIDTH, y + HUD_HEIGHT, (panelAlpha << 24) | 0x090D10);
        gui.fill(0, y, HUD_WIDTH, y + 1, (borderAlpha << 24) | 0xB8C6CC);
        gui.fill(0, y, 2, y + HUD_HEIGHT, withAlpha(accentColor, alpha));
    }

    private static void drawStateIndicator(GuiGraphics gui, int y, int accentColor) {
        int left = PADDING;
        int top = y + 9;
        if (ClientTimerState.getState() == TimerState.PAUSED) {
            gui.fill(left, top - 2, left + 2, top + 4, accentColor);
            gui.fill(left + 4, top - 2, left + 6, top + 4, accentColor);
        } else {
            gui.fill(left, top, left + 5, top + 5, accentColor);
            gui.fill(left + 1, top - 1, left + 4, top + 6, accentColor);
        }
    }

    private static void drawProgress(GuiGraphics gui, int y, int ticks,
                                     boolean countdown, int accentColor) {
        int barY = y + HUD_HEIGHT - PROGRESS_HEIGHT;
        gui.fill(2, barY, HUD_WIDTH, y + HUD_HEIGHT, 0xCC151C20);

        int fillWidth;
        if (countdown && ClientTimerState.getDurationTicks() > 0) {
            int duration = ClientTimerState.getDurationTicks();
            int clampedTicks = Math.max(0, Math.min(duration, ticks));
            fillWidth = Math.round((HUD_WIDTH - 2) * (clampedTicks / (float) duration));
        } else {
            fillWidth = HUD_WIDTH - 2;
        }
        if (fillWidth > 0) {
            gui.fill(2, barY, 2 + fillWidth, y + HUD_HEIGHT, accentColor);
            gui.fill(2, barY, 2 + fillWidth, barY + 1, withAlpha(0xFFFFFFFF, 76));
        }
    }

    private static void renderFinishAnimation(GuiGraphics gui, Minecraft mc) {
        float progress = ClientTimerState.getFinishAnimProgress(mc);
        float alpha = 1.0F - progress;
        int y = HUD_Y - Math.round(progress * 5.0F);
        int alphaByte = clamp(Math.round(alpha * 255.0F));
        int accentColor = 0xFF49C6D4;
        drawPanel(gui, y, accentColor, alphaByte);

        Component message = ClientTimerState.getFinishMessage();
        if (message == null || message.getString().isBlank()) {
            message = Component.literal("FINISHED");
        }
        String text = trimToWidth(mc, message.getString(), HUD_WIDTH - PADDING * 2);
        int color = (alphaByte << 24) | 0xE8F1F3;
        gui.drawCenteredString(mc.font, text, HUD_WIDTH / 2, y + 7, color);
        gui.fill(2, y + HUD_HEIGHT - PROGRESS_HEIGHT, HUD_WIDTH, y + HUD_HEIGHT,
                withAlpha(accentColor, alphaByte));
    }

    private static int computeX(int screenW, float hudScale) {
        int displayWidth = Math.round(HUD_WIDTH * hudScale);
        if (TimerClientConfig.HUD_POSITION.get() == TimerClientConfig.HudPosition.TOP_RIGHT) {
            return screenW - displayWidth - 10;
        }
        return (screenW - displayWidth) / 2;
    }

    private static int resolveCountdownAlertColor(int durationTicks, int remainingTicks) {
        if (remainingTicks <= 10 * 20) {
            return 0xFFFF5D57;
        }
        int amberThreshold = durationTicks >= 10 * 60 * 20 ? 60 * 20 : 30 * 20;
        if (remainingTicks <= amberThreshold) {
            return 0xFFFFB84A;
        }
        return 0xFF49C6D4;
    }

    private static String trimToWidth(Minecraft mc, String value, int maxWidth) {
        if (mc.font.width(value) <= maxWidth) {
            return value;
        }
        String suffix = "...";
        return mc.font.plainSubstrByWidth(value, Math.max(0, maxWidth - mc.font.width(suffix))) + suffix;
    }

    private static int withAlpha(int color, int alpha) {
        return (clamp(alpha) << 24) | (color & 0x00FFFFFF);
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
