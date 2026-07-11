package com.dousiyo.dpvptweaks.timer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class CountdownHudOverlay {
    private static final int PANEL_WIDTH = 264;
    private static final int PANEL_HEIGHT = 98;
    private static final int BAR_WIDTH = 208;
    private static final int BAR_HEIGHT = 8;

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
        int y = (screenH - PANEL_HEIGHT) / 2 - 8;

        if (ClientCountdownState.isFinished()) {
            renderFinish(gui, mc, x, y);
            return;
        }

        renderRunning(gui, mc, x, y);
    }

    private static void renderRunning(GuiGraphics gui, Minecraft mc, int x, int y) {
        float intro = ClientCountdownState.getRunningAnimProgress(mc);
        int rise = (int) ((1.0F - intro) * 18.0F);
        int drawY = y - rise;

        int panelAlpha = 170 + (int) (intro * 55.0F);
        int panelColor = (clamp(panelAlpha) << 24) | 0x08111A;
        int borderColor = 0xFF69D36E;
        int innerColor = 0xFF162534;
        drawPanel(gui, x, drawY, panelColor, borderColor, innerColor);

        long now = mc.level != null ? mc.level.getGameTime() : 0L;
        int ticks = ClientCountdownState.getDisplayTicks(mc);
        int seconds = ClientCountdownState.getDisplaySeconds(mc);
        int durationTicks = Math.max(1, ClientCountdownState.getDurationTicks());
        float ratio = Math.max(0.0F, Math.min(1.0F, ticks / (float) durationTicks));
        int barFill = Math.max(0, Math.min(BAR_WIDTH, Math.round(BAR_WIDTH * ratio)));
        int accentColor = resolveAccentColor(seconds);
        float pulse = 1.0F + (seconds <= 5 ? (float) Math.sin(now * 0.42F) * 0.06F : 0.0F);

        int barX = x + (PANEL_WIDTH - BAR_WIDTH) / 2;
        int barY = drawY + PANEL_HEIGHT - 16;
        gui.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, 0xFF101B27);
        gui.fill(barX + 1, barY + 1, barX + BAR_WIDTH - 1, barY + BAR_HEIGHT - 1, 0xFF1C2D40);
        if (barFill > 0) {
            gui.fill(barX + 1, barY + 1, barX + Math.min(BAR_WIDTH - 1, barFill), barY + BAR_HEIGHT - 1, accentColor);
        }

        String topLabel = "開始まで";
        String bottomLabel = seconds > 5 ? "まもなく開始" : "開始準備";
        Component number = Component.literal(seconds > 5 ? String.format("%02d", seconds) : Integer.toString(seconds));

        int topWidth = mc.font.width(topLabel);
        gui.drawString(mc.font, topLabel, x + (PANEL_WIDTH - topWidth) / 2, drawY + 12, 0xFF91A7BA, false);

        float numberScale = seconds > 5 ? 3.05F : 4.2F * pulse;
        int numberWidth = (int) Math.ceil(mc.font.width(number) * numberScale);
        int numberX = x + (PANEL_WIDTH - numberWidth) / 2;
        gui.pose().pushPose();
        gui.pose().translate(numberX, drawY + 26, 0.0F);
        gui.pose().scale(numberScale, numberScale, 1.0F);
        gui.drawString(mc.font, number, 0, 0, accentColor, true);
        gui.pose().popPose();

        int bottomWidth = mc.font.width(bottomLabel);
        gui.drawString(mc.font, bottomLabel, x + (PANEL_WIDTH - bottomWidth) / 2, drawY + 74, 0xFF6F8296, false);

        drawSideMarkers(gui, x, drawY, accentColor);
    }

    private static void renderFinish(GuiGraphics gui, Minecraft mc, int x, int y) {
        float progress = ClientCountdownState.getFinishAnimProgress(mc);
        int alpha = clamp(255 - (int) (progress * 255.0F));
        if (alpha <= 0) {
            return;
        }
        int fillColor = (alpha << 24) | 0x0A1A10;
        int borderColor = (alpha << 24) | 0x7AE27C;
        int innerColor = (alpha << 24) | 0x13271A;
        drawPanel(gui, x, y, fillColor, borderColor, innerColor);

        int stripeAlpha = clamp((int) (alpha * 0.35F));
        gui.fill(x + 16, y + 18, x + PANEL_WIDTH - 16, y + 20, (stripeAlpha << 24) | 0x63D96D);
        gui.fill(x + 16, y + PANEL_HEIGHT - 20, x + PANEL_WIDTH - 16, y + PANEL_HEIGHT - 18, (stripeAlpha << 24) | 0x63D96D);

        Component title = Component.literal("開始");
        Component subtitle = Component.literal("スタート！");

        float titleScale = 3.0F;
        int titleWidth = (int) Math.ceil(mc.font.width(title) * titleScale);
        int titleX = x + (PANEL_WIDTH - titleWidth) / 2;
        gui.pose().pushPose();
        gui.pose().translate(titleX, y + 24, 0.0F);
        gui.pose().scale(titleScale, titleScale, 1.0F);
        gui.drawString(mc.font, title, 0, 0, (alpha << 24) | 0xF4FFF4, true);
        gui.pose().popPose();

        int subtitleX = x + (PANEL_WIDTH - mc.font.width(subtitle)) / 2;
        gui.drawString(mc.font, subtitle, subtitleX, y + 72, (alpha << 24) | 0xBFEEC5, false);
    }

    private static void drawPanel(GuiGraphics gui, int x, int y, int fillColor, int borderColor, int innerColor) {
        gui.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, fillColor);
        gui.fill(x + 4, y + 4, x + PANEL_WIDTH - 4, y + PANEL_HEIGHT - 4, innerColor);

        gui.fill(x, y, x + PANEL_WIDTH, y + 2, borderColor);
        gui.fill(x, y + PANEL_HEIGHT - 2, x + PANEL_WIDTH, y + PANEL_HEIGHT, borderColor);
        gui.fill(x, y, x + 2, y + PANEL_HEIGHT, borderColor);
        gui.fill(x + PANEL_WIDTH - 2, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, borderColor);

        gui.fill(x + 12, y + 10, x + 54, y + 12, borderColor);
        gui.fill(x + PANEL_WIDTH - 54, y + 10, x + PANEL_WIDTH - 12, y + 12, borderColor);
        gui.fill(x + 12, y + PANEL_HEIGHT - 12, x + 54, y + PANEL_HEIGHT - 10, borderColor);
        gui.fill(x + PANEL_WIDTH - 54, y + PANEL_HEIGHT - 12, x + PANEL_WIDTH - 12, y + PANEL_HEIGHT - 10, borderColor);
    }

    private static void drawSideMarkers(GuiGraphics gui, int x, int y, int accentColor) {
        int left = x + 18;
        int right = x + PANEL_WIDTH - 18;
        int midY = y + 49;

        gui.fill(left, midY - 12, left + 2, midY + 12, accentColor);
        gui.fill(left, midY - 12, left + 12, midY - 10, accentColor);
        gui.fill(left, midY + 10, left + 12, midY + 12, accentColor);

        gui.fill(right - 2, midY - 12, right, midY + 12, accentColor);
        gui.fill(right - 12, midY - 12, right, midY - 10, accentColor);
        gui.fill(right - 12, midY + 10, right, midY + 12, accentColor);
    }

    private static int resolveAccentColor(int seconds) {
        if (seconds <= 2) {
            return 0xFFFF6B57;
        }
        if (seconds <= 5) {
            return 0xFFFFC857;
        }
        return 0xFF69D36E;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
