package com.dousiyo.dpvptweaks.client.pvpstats.screen;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.pvpstats.model.AggregateStats;
import com.dousiyo.dpvptweaks.pvpstats.model.MatchRecord;
import com.dousiyo.dpvptweaks.pvpstats.model.StatsGuiPayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PvpStatsScreen extends Screen {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(DpvpTweaks.MODID, "textures/gui/pvp_stats_gui.png");
    private static final int TEX_W = 384;
    private static final int TEX_H = 256;
    private static final int GUI_W = 220;
    private static final int GUI_H = 190;

    private static final UV MAIN_BG = new UV(0, 0, 220, 190);
    private static final UV TAB_NORMAL = new UV(224, 0, 64, 22);
    private static final UV TAB_SELECTED = new UV(224, 24, 64, 22);
    private static final UV CLOSE_NORMAL = new UV(292, 0, 20, 20);
    private static final UV CLOSE_HOVER = new UV(292, 24, 20, 20);
    private static final UV LIST_ROW = new UV(224, 52, 104, 20);
    private static final UV SCROLL_TRACK = new UV(332, 52, 10, 56);
    private static final UV SCROLL_THUMB = new UV(344, 52, 10, 18);
    private static final UV ARROW_UP = new UV(358, 52, 20, 20);
    private static final UV ARROW_DOWN = new UV(358, 76, 20, 20);
    private static final UV ICON_WIN = new UV(224, 116, 32, 32);
    private static final UV ICON_LOSS = new UV(256, 116, 32, 32);
    private static final UV ICON_KILL = new UV(224, 148, 32, 32);
    private static final UV ICON_DEATH = new UV(256, 148, 32, 32);
    private static final UV ICON_MODE = new UV(292, 116, 32, 32);

    private static final Rect CLOSE_RECT = new Rect(192, 8, 20, 20);
    private static final Rect TAB_OVERALL_RECT = new Rect(10, 28, 64, 22);
    private static final Rect TAB_MODE_RECT = new Rect(78, 28, 64, 22);
    private static final Rect TAB_HISTORY_RECT = new Rect(146, 28, 64, 22);

    private static final int LIST_X = 110;
    private static final int LIST_Y = 58;
    private static final int LIST_W = 96;
    private static final int LIST_H = 20;
    private static final int VISIBLE_ROWS = 5;
    private static final int SCROLL_UP_X = 188;
    private static final int SCROLL_UP_Y = 58;
    private static final int SCROLL_TRACK_X = 196;
    private static final int SCROLL_TRACK_Y = 82;
    private static final int SCROLL_DOWN_X = 188;
    private static final int SCROLL_DOWN_Y = 156;

    private static final SimpleDateFormat HISTORY_DATE_FORMAT = new SimpleDateFormat("MM/dd HH:mm", Locale.ROOT);

    private final StatsGuiPayload payload;
    private int leftPos;
    private int topPos;
    private Tab currentTab = Tab.OVERALL;
    private int selectedModeIndex = 0;
    private int modeScroll = 0;
    private int historyScroll = 0;

    public PvpStatsScreen(StatsGuiPayload payload) {
        super(Component.translatable("gui.dpvptweaks.pvp_stats.title"));
        this.payload = payload;
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - GUI_W) / 2;
        this.topPos = (this.height - GUI_H) / 2;

        if (this.payload.modeStats().isEmpty()) {
            this.selectedModeIndex = -1;
        } else {
            this.selectedModeIndex = Mth.clamp(this.selectedModeIndex, 0, this.payload.modeStats().size() - 1);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        blit(guiGraphics, MAIN_BG, this.leftPos, this.topPos);

        renderHeader(guiGraphics, mouseX, mouseY);
        renderTabs(guiGraphics);

        switch (this.currentTab) {
            case OVERALL -> renderOverallTab(guiGraphics);
            case MODE -> renderModeTab(guiGraphics);
            case HISTORY -> renderHistoryTab(guiGraphics);
        }

        renderTooltips(guiGraphics, mouseX, mouseY);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphics gg, int mouseX, int mouseY) {
        gg.drawString(this.font, this.title, this.leftPos + 10, this.topPos + 8, 0xF2F8FA, false);
        gg.drawString(this.font, safeTargetName(), this.leftPos + 10, this.topPos + 19, 0xBED0D8, false);

        boolean hoverClose = isHovering(CLOSE_RECT, mouseX, mouseY);
        blit(gg, hoverClose ? CLOSE_HOVER : CLOSE_NORMAL, this.leftPos + CLOSE_RECT.x, this.topPos + CLOSE_RECT.y);
    }

    private void renderTabs(GuiGraphics gg) {
        renderTab(gg, TAB_OVERALL_RECT, Component.translatable("gui.dpvptweaks.pvp_stats.tab.overall"), this.currentTab == Tab.OVERALL);
        renderTab(gg, TAB_MODE_RECT, Component.translatable("gui.dpvptweaks.pvp_stats.tab.mode"), this.currentTab == Tab.MODE);
        renderTab(gg, TAB_HISTORY_RECT, Component.translatable("gui.dpvptweaks.pvp_stats.tab.history"), this.currentTab == Tab.HISTORY);
    }

    private void renderTab(GuiGraphics gg, Rect rect, Component text, boolean selected) {
        blit(gg, selected ? TAB_SELECTED : TAB_NORMAL, this.leftPos + rect.x, this.topPos + rect.y);
        int tx = this.leftPos + rect.x + (rect.w - this.font.width(text)) / 2;
        int ty = this.topPos + rect.y + 7;
        gg.drawString(this.font, text, tx, ty, selected ? 0xFFFFFF : 0xD6E1E6, false);
    }

    private void renderOverallTab(GuiGraphics gg) {
        AggregateStats global = this.payload.global();
        boolean showDraws = global.draws() > 0L;

        drawStatLine(gg, ICON_WIN, Component.translatable("gui.dpvptweaks.pvp_stats.stat.wins"), String.valueOf(global.wins()), 16, 60);
        drawStatLine(gg, ICON_LOSS, Component.translatable("gui.dpvptweaks.pvp_stats.stat.losses"), String.valueOf(global.losses()), 16, 88);
        drawStatLine(gg, ICON_KILL, Component.translatable("gui.dpvptweaks.pvp_stats.stat.kills"), String.valueOf(global.kills()), 16, 116);
        drawStatLine(gg, ICON_DEATH, Component.translatable("gui.dpvptweaks.pvp_stats.stat.deaths"), String.valueOf(global.deaths()), 16, 144);

        int x = this.leftPos + 112;
        int y = this.topPos + 62;
        int color = 0xF2F8FA;
        int sub = 0xBED0D8;

        gg.drawString(this.font, Component.translatable("gui.dpvptweaks.pvp_stats.section.summary"), x, y, color, false);
        y += 18;
        drawValuePair(gg, x, y, Component.translatable("gui.dpvptweaks.pvp_stats.stat.matches"), String.valueOf(global.matches()), sub, color);
        y += 14;
        if (showDraws) {
            drawValuePair(gg, x, y, Component.translatable("gui.dpvptweaks.pvp_stats.stat.draws"), String.valueOf(global.draws()), sub, color);
            y += 14;
        }
        drawValuePair(gg, x, y, Component.translatable("gui.dpvptweaks.pvp_stats.stat.kd"), formatKd(global), sub, color);
        y += 14;
        drawValuePair(gg, x, y, Component.translatable("gui.dpvptweaks.pvp_stats.stat.win_rate"), formatWinRate(global), sub, color);
        y += 22;
        gg.drawString(this.font, Component.translatable("gui.dpvptweaks.pvp_stats.target"), x, y, sub, false);
        y += 12;
        gg.drawString(this.font, trimToWidth(safeTargetName(), 90), x, y, color, false);

        if (!this.payload.hasAnyData()) {
            gg.drawString(this.font, Component.translatable("gui.dpvptweaks.pvp_stats.no_data"), x, this.topPos + 162, 0xCFAAAA, false);
        }
    }

    private void renderModeTab(GuiGraphics gg) {
        List<Map.Entry<String, AggregateStats>> entries = new ArrayList<>(this.payload.modeStats().entrySet());
        int total = entries.size();
        int visible = Math.min(VISIBLE_ROWS, total);
        this.modeScroll = Mth.clamp(this.modeScroll, 0, Math.max(0, total - VISIBLE_ROWS));

        for (int i = 0; i < visible; i++) {
            int idx = this.modeScroll + i;
            Map.Entry<String, AggregateStats> entry = entries.get(idx);
            int rowX = this.leftPos + LIST_X;
            int rowY = this.topPos + LIST_Y + i * LIST_H;
            gg.blit(TEXTURE, rowX, rowY, LIST_W, LIST_H, LIST_ROW.u, LIST_ROW.v, LIST_ROW.w, LIST_ROW.h, TEX_W, TEX_H);

            if (idx == this.selectedModeIndex) {
                gg.fill(rowX + 1, rowY + 1, rowX + LIST_W - 1, rowY + LIST_H - 1, 0x88495C66);
            }

            blitScaled(gg, ICON_MODE, rowX + 2, rowY + 2, 16, 16);
            gg.drawString(this.font, trimToWidth(toModeDisplayName(entry.getKey()), 68), rowX + 22, rowY + 6, 0xF2F8FA, false);
        }

        renderScrollBar(gg, total, this.modeScroll);

        int baseX = this.leftPos + 16;
        int baseY = this.topPos + 62;
        gg.drawString(this.font, Component.translatable("gui.dpvptweaks.pvp_stats.section.mode"), baseX, baseY, 0xF2F8FA, false);
        baseY += 14;

        if (entries.isEmpty() || this.selectedModeIndex < 0 || this.selectedModeIndex >= entries.size()) {
            gg.drawString(this.font, Component.translatable("gui.dpvptweaks.pvp_stats.mode_empty"), baseX, baseY, 0xBED0D8, false);
            return;
        }

        Map.Entry<String, AggregateStats> selected = entries.get(this.selectedModeIndex);
        AggregateStats stats = selected.getValue();
        boolean showDraws = stats.draws() > 0L;
        gg.drawString(this.font, trimToWidth(toModeDisplayName(selected.getKey()), 84), baseX, baseY, 0xDDE8ED, false);
        baseY += 12;

        drawValuePair(gg, baseX, baseY, Component.translatable("gui.dpvptweaks.pvp_stats.stat.wins"), String.valueOf(stats.wins()), 46, 0xBED0D8, 0xF2F8FA);
        baseY += 11;
        drawValuePair(gg, baseX, baseY, Component.translatable("gui.dpvptweaks.pvp_stats.stat.losses"), String.valueOf(stats.losses()), 46, 0xBED0D8, 0xF2F8FA);
        baseY += 11;
        if (showDraws) {
            drawValuePair(gg, baseX, baseY, Component.translatable("gui.dpvptweaks.pvp_stats.stat.draws"), String.valueOf(stats.draws()), 46, 0xBED0D8, 0xF2F8FA);
            baseY += 11;
        }
        drawValuePair(gg, baseX, baseY, Component.translatable("gui.dpvptweaks.pvp_stats.stat.kills"), String.valueOf(stats.kills()), 46, 0xBED0D8, 0xF2F8FA);
        baseY += 11;
        drawValuePair(gg, baseX, baseY, Component.translatable("gui.dpvptweaks.pvp_stats.stat.deaths"), String.valueOf(stats.deaths()), 46, 0xBED0D8, 0xF2F8FA);
        baseY += 11;
        drawValuePair(gg, baseX, baseY, Component.translatable("gui.dpvptweaks.pvp_stats.stat.matches"), String.valueOf(stats.matches()), 46, 0xBED0D8, 0xF2F8FA);
        baseY += 11;
        drawValuePair(gg, baseX, baseY, Component.translatable("gui.dpvptweaks.pvp_stats.stat.kd"), formatKd(stats), 46, 0xBED0D8, 0xF2F8FA);
        baseY += 11;
        drawValuePair(gg, baseX, baseY, Component.translatable("gui.dpvptweaks.pvp_stats.stat.win_rate"), formatWinRate(stats), 46, 0xBED0D8, 0xF2F8FA);
    }

    private void renderHistoryTab(GuiGraphics gg) {
        List<MatchRecord> records = this.payload.recentMatches();
        int total = records.size();
        int visible = Math.min(VISIBLE_ROWS, total);
        this.historyScroll = Mth.clamp(this.historyScroll, 0, Math.max(0, total - VISIBLE_ROWS));

        int leftX = this.leftPos + 16;
        int leftY = this.topPos + 62;
        gg.drawString(this.font, Component.translatable("gui.dpvptweaks.pvp_stats.section.history"), leftX, leftY, 0xF2F8FA, false);
        leftY += 16;
        drawValuePair(gg, leftX, leftY, Component.translatable("gui.dpvptweaks.pvp_stats.stat.total_entries"), String.valueOf(total), 52, 0xBED0D8, 0xF2F8FA);

        if (records.isEmpty()) {
            gg.drawString(this.font, Component.translatable("gui.dpvptweaks.pvp_stats.history_empty"), leftX, leftY + 20, 0xBED0D8, false);
        }

        for (int i = 0; i < visible; i++) {
            int idx = this.historyScroll + i;
            MatchRecord record = records.get(idx);
            int rowX = this.leftPos + LIST_X;
            int rowY = this.topPos + LIST_Y + i * LIST_H;
            gg.blit(TEXTURE, rowX, rowY, LIST_W, LIST_H, LIST_ROW.u, LIST_ROW.v, LIST_ROW.w, LIST_ROW.h, TEX_W, TEX_H);

            String normalizedResult = record.result().toUpperCase(Locale.ROOT);
            String resultShort = switch (normalizedResult) {
                case "WIN" -> "W";
                case "LOSS" -> "L";
                case "DRAW" -> "D";
                default -> "?";
            };
            int resultColor = switch (normalizedResult) {
                case "WIN" -> 0xBCE473;
                case "LOSS" -> 0xF0A2A2;
                case "DRAW" -> 0xE6D78C;
                default -> 0xC6D0D6;
            };

            gg.drawString(this.font, resultShort, rowX + 4, rowY + 6, resultColor, false);
            gg.drawString(this.font, trimToWidth(toModeDisplayName(record.modeId()), 46), rowX + 14, rowY + 6, 0xF2F8FA, false);
            gg.drawString(this.font, record.kills() + "/" + record.deaths(), rowX + 68, rowY + 6, 0xD6E1E6, false);
        }

        renderScrollBar(gg, total, this.historyScroll);
    }

    private void renderScrollBar(GuiGraphics gg, int total, int scroll) {
        blit(gg, ARROW_UP, this.leftPos + SCROLL_UP_X, this.topPos + SCROLL_UP_Y);
        blit(gg, ARROW_DOWN, this.leftPos + SCROLL_DOWN_X, this.topPos + SCROLL_DOWN_Y);
        blit(gg, SCROLL_TRACK, this.leftPos + SCROLL_TRACK_X, this.topPos + SCROLL_TRACK_Y);

        int maxScroll = Math.max(0, total - VISIBLE_ROWS);
        int thumbY = this.topPos + SCROLL_TRACK_Y;
        if (maxScroll > 0) {
            thumbY += (int) ((38.0D * scroll) / maxScroll);
        }
        blit(gg, SCROLL_THUMB, this.leftPos + SCROLL_TRACK_X, thumbY);
    }

    private void drawStatLine(GuiGraphics gg, UV icon, Component label, String value, int relX, int relY) {
        int x = this.leftPos + relX;
        int y = this.topPos + relY;
        blitScaled(gg, icon, x, y, 18, 18);
        gg.drawString(this.font, label, x + 22, y + 5, 0xC1D4DC, false);
        gg.drawString(this.font, value, x + 58, y + 5, 0xFFFFFF, false);
    }

    private void drawValuePair(GuiGraphics gg, int x, int y, Component key, String value, int keyColor, int valueColor) {
        drawValuePair(gg, x, y, key, value, 52, keyColor, valueColor);
    }

    private void drawValuePair(GuiGraphics gg, int x, int y, Component key, String value, int valueOffset, int keyColor, int valueColor) {
        gg.drawString(this.font, key, x, y, keyColor, false);
        gg.drawString(this.font, value, x + valueOffset, y, valueColor, false);
    }

    private void renderTooltips(GuiGraphics gg, int mouseX, int mouseY) {
        if (isHovering(TAB_OVERALL_RECT, mouseX, mouseY)) {
            gg.renderTooltip(this.font, Component.translatable("gui.dpvptweaks.pvp_stats.tooltip.overall"), mouseX, mouseY);
            return;
        }
        if (isHovering(TAB_MODE_RECT, mouseX, mouseY)) {
            gg.renderTooltip(this.font, Component.translatable("gui.dpvptweaks.pvp_stats.tooltip.mode"), mouseX, mouseY);
            return;
        }
        if (isHovering(TAB_HISTORY_RECT, mouseX, mouseY)) {
            gg.renderTooltip(this.font, Component.translatable("gui.dpvptweaks.pvp_stats.tooltip.history"), mouseX, mouseY);
            return;
        }
        if (isHovering(CLOSE_RECT, mouseX, mouseY)) {
            gg.renderTooltip(this.font, Component.translatable("gui.dpvptweaks.pvp_stats.tooltip.close"), mouseX, mouseY);
            return;
        }

        if (this.currentTab == Tab.HISTORY) {
            List<MatchRecord> records = this.payload.recentMatches();
            int visible = Math.min(VISIBLE_ROWS, records.size());
            for (int i = 0; i < visible; i++) {
                int idx = this.historyScroll + i;
                Rect row = new Rect(LIST_X, LIST_Y + i * LIST_H, LIST_W, LIST_H);
                if (idx < records.size() && isHovering(row, mouseX, mouseY)) {
                    MatchRecord record = records.get(idx);
                    List<Component> tooltip = List.of(
                            Component.literal(toModeDisplayName(record.modeId())),
                            Component.literal(HISTORY_DATE_FORMAT.format(new Date(record.timestamp()))),
                            Component.literal(toResultDisplayName(record.result()) + "  " + record.kills() + " / " + record.deaths())
                    );
                    gg.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                    return;
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (isHovering(CLOSE_RECT, mouseX, mouseY)) {
            this.onClose();
            return true;
        }
        if (isHovering(TAB_OVERALL_RECT, mouseX, mouseY)) {
            this.currentTab = Tab.OVERALL;
            return true;
        }
        if (isHovering(TAB_MODE_RECT, mouseX, mouseY)) {
            this.currentTab = Tab.MODE;
            return true;
        }
        if (isHovering(TAB_HISTORY_RECT, mouseX, mouseY)) {
            this.currentTab = Tab.HISTORY;
            return true;
        }

        if (this.currentTab == Tab.MODE) {
            List<Map.Entry<String, AggregateStats>> entries = new ArrayList<>(this.payload.modeStats().entrySet());
            int visible = Math.min(VISIBLE_ROWS, entries.size());
            for (int i = 0; i < visible; i++) {
                int idx = this.modeScroll + i;
                Rect row = new Rect(LIST_X, LIST_Y + i * LIST_H, LIST_W, LIST_H);
                if (idx < entries.size() && isHovering(row, mouseX, mouseY)) {
                    this.selectedModeIndex = idx;
                    return true;
                }
            }

            if (isHovering(new Rect(SCROLL_UP_X, SCROLL_UP_Y, 20, 20), mouseX, mouseY)) {
                this.modeScroll = Math.max(0, this.modeScroll - 1);
                return true;
            }
            if (isHovering(new Rect(SCROLL_DOWN_X, SCROLL_DOWN_Y, 20, 20), mouseX, mouseY)) {
                this.modeScroll = Math.min(Math.max(0, entries.size() - VISIBLE_ROWS), this.modeScroll + 1);
                return true;
            }
        }

        if (this.currentTab == Tab.HISTORY) {
            int total = this.payload.recentMatches().size();
            if (isHovering(new Rect(SCROLL_UP_X, SCROLL_UP_Y, 20, 20), mouseX, mouseY)) {
                this.historyScroll = Math.max(0, this.historyScroll - 1);
                return true;
            }
            if (isHovering(new Rect(SCROLL_DOWN_X, SCROLL_DOWN_Y, 20, 20), mouseX, mouseY)) {
                this.historyScroll = Math.min(Math.max(0, total - VISIBLE_ROWS), this.historyScroll + 1);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.currentTab == Tab.MODE) {
            int max = Math.max(0, this.payload.modeStats().size() - VISIBLE_ROWS);
            this.modeScroll = Mth.clamp(this.modeScroll - (int) Math.signum(delta), 0, max);
            return true;
        }

        if (this.currentTab == Tab.HISTORY) {
            int max = Math.max(0, this.payload.recentMatches().size() - VISIBLE_ROWS);
            this.historyScroll = Mth.clamp(this.historyScroll - (int) Math.signum(delta), 0, max);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean isHovering(Rect rect, double mouseX, double mouseY) {
        int x = this.leftPos + rect.x;
        int y = this.topPos + rect.y;
        return mouseX >= x && mouseX < x + rect.w && mouseY >= y && mouseY < y + rect.h;
    }

    private void blit(GuiGraphics gg, UV uv, int x, int y) {
        gg.blit(TEXTURE, x, y, uv.u, uv.v, uv.w, uv.h, TEX_W, TEX_H);
    }

    private void blitScaled(GuiGraphics gg, UV uv, int x, int y, int drawW, int drawH) {
        gg.blit(TEXTURE, x, y, drawW, drawH, uv.u, uv.v, uv.w, uv.h, TEX_W, TEX_H);
    }

    private String safeTargetName() {
        return this.payload.targetName().isBlank() ? "-" : this.payload.targetName();
    }

    private String formatKd(AggregateStats stats) {
        if (stats.deaths() <= 0L) {
            return stats.kills() <= 0L ? "--" : String.format(Locale.ROOT, "%.2f", (double) stats.kills());
        }
        return String.format(Locale.ROOT, "%.2f", (double) stats.kills() / (double) stats.deaths());
    }

    private String formatWinRate(AggregateStats stats) {
        if (stats.matches() <= 0L) {
            return "--";
        }
        return String.format(Locale.ROOT, "%.1f%%", (double) stats.wins() * 100.0D / (double) stats.matches());
    }

    private String trimToWidth(String text, int maxWidth) {
        Font font = this.font;
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, maxWidth - font.width("...")) + "...";
    }

    private String toModeDisplayName(String modeId) {
        String translationKey = "gui." + DpvpTweaks.MODID + ".pvp_stats.mode." + modeId.replace('/', '.');
        if (I18n.exists(translationKey)) {
            return I18n.get(translationKey);
        }
        return modeId;
    }

    private String toResultDisplayName(String result) {
        String normalized = result == null ? "" : result.toLowerCase(Locale.ROOT);
        String translationKey = "gui." + DpvpTweaks.MODID + ".pvp_stats.result." + normalized;
        if (I18n.exists(translationKey)) {
            return I18n.get(translationKey);
        }
        return result == null || result.isBlank() ? "UNKNOWN" : result;
    }

    private enum Tab {
        OVERALL,
        MODE,
        HISTORY
    }

    private record UV(int u, int v, int w, int h) {
    }

    private record Rect(int x, int y, int w, int h) {
    }
}
