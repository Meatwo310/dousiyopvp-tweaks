package com.dousiyo.dpvptweaks.client.capture;

import com.dousiyo.dpvptweaks.capture.core.PointState;
import com.dousiyo.dpvptweaks.capture.core.TeamSide;
import com.dousiyo.dpvptweaks.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

public final class FocusHudOverlay {
    private static final int PANEL_WIDTH = 180;
    private static final int PANEL_HEIGHT = 46;
    private static final int BAR_WIDTH = 156;
    private static final int BAR_HEIGHT = 8;

    private FocusHudOverlay() {}

    public static void render(GuiGraphics gui, int screenW, int screenH) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.font == null || !ClientConfig.CAPTURE_SHOW_FOCUS_HUD.get()
                || !ClientCapturePointsState.isCaptureFeatureEnabled()) {
            return;
        }

        int slot = ClientCapturePointsState.getFocusedSlot();
        if (slot < 0) {
            return;
        }

        ClientCapturePointsState.Snapshot snapshot = ClientCapturePointsState.getSnapshot(slot);
        if (snapshot == null) {
            return;
        }

        float progress = ClientCapturePointsState.resolveProgress(snapshot, mc);
        TeamSide dominant = dominantSide(progress);
        boolean decapping = isDecapping(snapshot, progress);
        float ratio = directionalRatio(progress);

        int x = 8;
        int y = screenH - 124;

        gui.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xA0000000);

        String label = "\u62e0\u70b9 " + (char) ('A' + slot);
        String stateText = stateText(snapshot.state(), decapping);
        int titleColor = snapshot.state() == PointState.CONTESTED ? 0xFFE6C35A : 0xFFFFFFFF;

        gui.drawString(mc.font, label, x + 8, y + 5, titleColor, true);
        gui.drawString(mc.font, stateText, x + PANEL_WIDTH - 8 - mc.font.width(stateText), y + 5, titleColor, true);

        int barX = x + (PANEL_WIDTH - BAR_WIDTH) / 2;
        int barY = y + 18;
        gui.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, 0xAA252525);

        int fillWidth = Math.max(0, Math.min(BAR_WIDTH, Math.round(BAR_WIDTH * ratio)));
        if (dominant != TeamSide.NONE && fillWidth > 0) {
            int color = dominant == TeamSide.BLUE ? 0xFF3B89FF : 0xFFD95A5A;
            gui.fill(barX, barY, barX + fillWidth, barY + BAR_HEIGHT, color);
        }

        if (ClientCapturePointsState.isFocusedBoosted()) {
            String boost = "2x\u5236\u5727\u4e2d";
            gui.drawString(mc.font, boost, x + 8, y + 28, 0xFFE6C35A, false);
        }

        String pct = String.format(Locale.ROOT, "\u9032\u6357 %.0f%%", ratio * 100.0F);
        gui.drawString(mc.font, pct, x + PANEL_WIDTH - 8 - mc.font.width(pct), y + 28, 0xFFFFFFFF, false);
    }

    private static TeamSide dominantSide(float progress) {
        if (progress > 0.5001F) {
            return TeamSide.BLUE;
        }
        if (progress < 0.4999F) {
            return TeamSide.RED;
        }
        return TeamSide.NONE;
    }

    private static float directionalRatio(float progress) {
        float ratio = Math.abs(progress - 0.5F) * 2.0F;
        return Math.max(0.0F, Math.min(1.0F, ratio));
    }

    private static String stateText(PointState state, boolean decapping) {
        if (decapping) {
            return "\u7121\u52b9\u5316\u4e2d";
        }
        return switch (state) {
            case CONTESTED -> "\u62ee\u6297";
            case CAPTURING -> "\u5236\u5727\u4e2d";
            case OWNED -> "\u5236\u5727\u6e08";
            default -> "\u4e2d\u7acb";
        };
    }

    private static boolean isDecapping(ClientCapturePointsState.Snapshot snapshot, float progress) {
        if (snapshot.state() != PointState.CAPTURING) {
            return false;
        }
        return (snapshot.captureTeam() == TeamSide.BLUE && progress < 0.5F)
                || (snapshot.captureTeam() == TeamSide.RED && progress > 0.5F);
    }
}