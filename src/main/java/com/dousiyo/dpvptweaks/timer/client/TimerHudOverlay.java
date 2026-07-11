package com.dousiyo.dpvptweaks.timer.client;

import com.dousiyo.dpvptweaks.timer.config.TimerClientConfig;
import com.dousiyo.dpvptweaks.timer.core.TimerMode;
import com.dousiyo.dpvptweaks.timer.core.TimerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class TimerHudOverlay {
    private static final int HUD_Y = 8;
    private static final int PANEL_PADDING_X = 6;
    private static final int PANEL_PADDING_TOP = 4;
    private static final int PANEL_BAR_WIDTH = 182;
    private static final float TITLE_SCALE = 0.82F;
    private static final float TIME_SCALE = 1.0F;

    private TimerHudOverlay() {}

    public static void render(GuiGraphics gui, int screenW, int screenH) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.font == null || !ClientTimerState.isVisible() || mc.options.keyPlayerList.isDown()) {
            return;
        }

        if (ClientTimerState.isFinishAnimating(mc)) {
            renderFinishAnimation(gui, mc, screenW);
            return;
        }

        if (ClientTimerState.getMode() == TimerMode.COUNTDOWN && ClientTimerState.getState() == TimerState.FINISHED) {
            return;
        }

        int ticks = ClientTimerState.getDisplayTicks(mc);
        Component title = ClientTimerState.getTitle();
        if (title == null || title.getString().isBlank()) {
            title = Component.literal(ClientTimerState.getTimerId());
        }

        String titleText = title.getString();
        String timeText = formatTicks(ticks);
        int titleWidth = mc.font.width(titleText);
        int timeWidth = mc.font.width(timeText);
        int gap = 8;
        int scaledTitleWidth = (int) Math.ceil(titleWidth * TITLE_SCALE);
        int scaledTimeWidth = (int) Math.ceil(timeWidth * TIME_SCALE);
        int lineWidth = scaledTitleWidth + gap + scaledTimeWidth;

        boolean drawCountdownBar = ClientTimerState.getMode() == TimerMode.COUNTDOWN
                && ClientTimerState.getDurationTicks() > 0;
        int barHeight = drawCountdownBar ? 8 : 0;
        int textHeight = (int) Math.ceil(mc.font.lineHeight * TIME_SCALE);
        int contentHeight = barHeight + textHeight + 6;

        int width = Math.max(PANEL_BAR_WIDTH, lineWidth);
        int x = computeX(screenW, width);
        int y = HUD_Y;

        drawPanel(gui, x, y, width, contentHeight, 0x90000000);

        if (drawCountdownBar) {
            int duration = ClientTimerState.getDurationTicks();
            int clampedTicks = Math.max(0, Math.min(duration, ticks));
            float remainingRatio = duration <= 0 ? 0.0F : (clampedTicks / (float) duration);
            int alertColor = resolveCountdownAlertColor(duration, clampedTicks);

            int barX = x + (width - PANEL_BAR_WIDTH) / 2;
            int barY = y + 1;
            int fillWidth = Math.max(0, Math.min(PANEL_BAR_WIDTH, Math.round(PANEL_BAR_WIDTH * remainingRatio)));

            gui.fill(barX, barY, barX + PANEL_BAR_WIDTH, barY + 6, 0xAA2A2A2A);
            if (fillWidth > 0) {
                gui.fill(barX, barY, barX + fillWidth, barY + 6, alertColor);
            }
        }

        int timeColor = 0xFFD25A;
        if (drawCountdownBar) {
            int duration = ClientTimerState.getDurationTicks();
            int clampedTicks = Math.max(0, Math.min(duration, ticks));
            timeColor = resolveCountdownAlertColor(duration, clampedTicks);
        }

        int textY = y + barHeight + 3;
        int textX = x + (width - lineWidth) / 2;

        gui.pose().pushPose();
        gui.pose().translate(textX, textY + 1, 0.0F);
        gui.pose().scale(TITLE_SCALE, TITLE_SCALE, 1.0F);
        gui.drawString(mc.font, titleText, 0, 0, 0xFFFFFF, true);
        gui.pose().popPose();

        gui.pose().pushPose();
        gui.pose().translate(textX + scaledTitleWidth + gap, textY + 1, 0.0F);
        gui.pose().scale(TIME_SCALE, TIME_SCALE, 1.0F);
        gui.drawString(mc.font, timeText, 0, 0, timeColor, true);
        gui.pose().popPose();
    }

    private static void renderFinishAnimation(GuiGraphics gui, Minecraft mc, int screenW) {
        float progress = ClientTimerState.getFinishAnimProgress(mc);
        int rise = 22;
        int y = HUD_Y - (int) (progress * rise);
        int alpha = 255 - (int) (progress * 255.0F);
        alpha = Math.max(0, Math.min(255, alpha));

        Component message = ClientTimerState.getFinishMessage();
        if (message == null || message.getString().isBlank()) {
            message = Component.literal("FINISHED");
        }

        float messageScale = 1.0F;
        int messageWidth = (int) Math.ceil(mc.font.width(message) * messageScale);
        int messageHeight = (int) Math.ceil(mc.font.lineHeight * messageScale);
        int width = Math.max(PANEL_BAR_WIDTH, messageWidth + 10);
        int contentHeight = messageHeight + 8;
        int x = computeX(screenW, width);

        int panelColor = ((int) (alpha * 0.56F) << 24);
        drawPanel(gui, x, y, width, contentHeight, panelColor);

        int textColor = (alpha << 24) | 0xFFFFFF;
        gui.pose().pushPose();
        gui.pose().translate(x + (width - messageWidth) / 2.0F, y + 4, 0.0F);
        gui.pose().scale(messageScale, messageScale, 1.0F);
        gui.drawString(mc.font, message, 0, 0, textColor, true);
        gui.pose().popPose();
    }

    private static int computeX(int screenW, int width) {
        if (TimerClientConfig.HUD_POSITION.get() == TimerClientConfig.HudPosition.TOP_RIGHT) {
            return screenW - width - 12;
        }
        return (screenW - width) / 2;
    }

    private static void drawPanel(GuiGraphics gui, int x, int y, int width, int contentHeight, int color) {
        gui.fill(
                x - PANEL_PADDING_X,
                y - PANEL_PADDING_TOP,
                x + width + PANEL_PADDING_X,
                y + contentHeight + 4,
                color
        );
    }

    private static int resolveCountdownAlertColor(int durationTicks, int remainingTicks) {
        int redThresholdTicks = 10 * 20;
        int yellowThresholdTicks = durationTicks >= 10 * 60 * 20 ? 60 * 20 : 30 * 20;

        if (remainingTicks <= redThresholdTicks) {
            return 0xFFFF5A5A;
        }
        if (remainingTicks <= yellowThresholdTicks) {
            return 0xFFFFC857;
        }
        return 0xFF4FD66D;
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
