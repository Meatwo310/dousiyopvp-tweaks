package com.dousiyo.dpvptweaks.client.capture;

import com.dousiyo.dpvptweaks.capture.core.PointState;
import com.dousiyo.dpvptweaks.capture.core.TeamSide;
import com.dousiyo.dpvptweaks.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public final class OverviewHudOverlay {
    private static final int PANEL_WIDTH = 24;
    private static final int PANEL_HEIGHT = 34;
    private static final int PANEL_GAP = 6;

    private static final int COLOR_BORDER = 0xE0FFFFFF;
    private static final int COLOR_BORDER_BLUE = 0xFF3B89FF;
    private static final int COLOR_BORDER_RED = 0xFFD95A5A;
    private static final int COLOR_BG = 0x7A111111;
    private static final int COLOR_BLUE = 0xCC3B89FF;
    private static final int COLOR_RED = 0xCCD95A5A;
    private static final int COLOR_CONTESTED = 0xCCE6C35A;

    private OverviewHudOverlay() {}

    public static void render(GuiGraphics gui, int screenW, int screenH) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.font == null || !ClientConfig.CAPTURE_SHOW_OVERVIEW_HUD.get()
                || !ClientCapturePointsState.isCaptureFeatureEnabled()) {
            return;
        }

        List<Integer> slots = ClientCapturePointsState.getSortedSlots();
        if (slots.isEmpty()) {
            return;
        }

        int totalWidth = slots.size() * PANEL_WIDTH + (slots.size() - 1) * PANEL_GAP;
        int baseX = (screenW - totalWidth) / 2;
        int baseY = -22 + ClientConfig.CAPTURE_OVERVIEW_Y_OFFSET.get();
        for (int i = 0; i < slots.size(); i++) {
            int slot = slots.get(i);
            ClientCapturePointsState.Snapshot snapshot = ClientCapturePointsState.getSnapshot(slot);
            if (snapshot == null) {
                continue;
            }

            int x = baseX + i * (PANEL_WIDTH + PANEL_GAP);
            int y = baseY;
            float progress = ClientCapturePointsState.resolveProgress(snapshot, mc);

            int borderColor = COLOR_BORDER;
            if (snapshot.state() == PointState.OWNED
                    || (snapshot.state() == PointState.CONTESTED && snapshot.owner() != TeamSide.NONE)) {
                borderColor = borderColorFromTeam(snapshot.owner());
            }

            gui.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, COLOR_BG);
            gui.fill(x, y, x + PANEL_WIDTH, y + 1, borderColor);
            gui.fill(x, y + PANEL_HEIGHT - 1, x + PANEL_WIDTH, y + PANEL_HEIGHT, borderColor);
            gui.fill(x, y, x + 1, y + PANEL_HEIGHT, borderColor);
            gui.fill(x + PANEL_WIDTH - 1, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, borderColor);

            int fillTop = y + 2;
            int fillBottom = y + PANEL_HEIGHT - 2;
            int fillLeft = x + 2;
            int fillRight = x + PANEL_WIDTH - 2;
            int fillHeightMax = fillBottom - fillTop;

            if (snapshot.state() == PointState.CONTESTED) {
                gui.fill(fillLeft, fillTop, fillRight, fillBottom, COLOR_CONTESTED);
            } else if (snapshot.state() == PointState.OWNED) {
                gui.fill(fillLeft, fillTop, fillRight, fillBottom, teamColor(snapshot.owner()));
            } else {
                TeamSide dominant = dominantSide(progress);
                float ratio = directionalRatio(progress);
                int fillHeight = Math.max(0, Math.min(fillHeightMax, Math.round(fillHeightMax * ratio)));
                if (dominant != TeamSide.NONE && fillHeight > 0) {
                    gui.fill(fillLeft, fillBottom - fillHeight, fillRight, fillBottom, teamColor(dominant));
                }
            }

            char label = (char) ('A' + slot);
            String labelText = Character.toString(label);
            int labelX = x + (PANEL_WIDTH - mc.font.width(labelText)) / 2;
            gui.drawString(mc.font, labelText, labelX, y + 3, 0xFFFFFFFF, true);
        }
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

    private static int teamColor(TeamSide side) {
        if (side == TeamSide.BLUE) {
            return COLOR_BLUE;
        }
        if (side == TeamSide.RED) {
            return COLOR_RED;
        }
        return 0;
    }

    private static int borderColorFromTeam(TeamSide side) {
        if (side == TeamSide.BLUE) {
            return COLOR_BORDER_BLUE;
        }
        if (side == TeamSide.RED) {
            return COLOR_BORDER_RED;
        }
        return COLOR_BORDER;
    }
}