package com.dousiyo.dpvptweaks.client.pvpstats.screen;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.client.pvpstats.PvpStatsClient;
import com.dousiyo.dpvptweaks.client.content.ContentClient;
import com.dousiyo.dpvptweaks.client.secretoperations.ClientDamageFeedback;
import com.dousiyo.dpvptweaks.config.ClientConfig;
import com.dousiyo.dpvptweaks.pvpstats.model.AggregateStats;
import com.dousiyo.dpvptweaks.pvpstats.model.MatchRecord;
import com.dousiyo.dpvptweaks.pvpstats.model.PlayerPrivacySettings;
import com.dousiyo.dpvptweaks.pvpstats.model.RankingEntry;
import com.dousiyo.dpvptweaks.pvpstats.model.StatsGuiPayload;
import com.dousiyo.dpvptweaks.pvpstats.mode.PvpModeDefinition;
import com.dousiyo.dpvptweaks.pvpstats.rank.RankState;
import com.dousiyo.dpvptweaks.pvpstats.rank.RankSystem;
import com.dousiyo.dpvptweaks.pvpstats.badge.BadgeDefinition;
import com.dousiyo.dpvptweaks.pvpstats.badge.BadgeSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PvpStatsScreen extends Screen {
    private static final String KEY_PREFIX = "gui." + DpvpTweaks.MODID + ".combat_record.";
    private static final int GUI_W = 320;
    private static final int GUI_H = 210;

    private static final ResourceLocation OVERVIEW_BG = texture("pages/overview_bg");
    private static final ResourceLocation HISTORY_BG = texture("pages/history_bg");
    private static final ResourceLocation RANKING_BG = texture("pages/ranking_bg");
    private static final ResourceLocation SETTINGS_BG = texture("pages/settings_bg");

    private static final Tex TAB_HOVER = tex("overlay/tab_hover", 54, 14);
    private static final Tex TAB_SELECTED = tex("overlay/tab_selected_glow", 54, 14);
    private static final Tex MODE_ROW_NORMAL = tex("overlay/mode_row_normal", 60, 14);
    private static final Tex MODE_ROW_HOVER = tex("overlay/mode_row_hover", 60, 14);
    private static final Tex MODE_ROW_SELECTED = tex("overlay/mode_row_selected", 60, 14);
    private static final Tex FILTER_ROW_NORMAL = tex("overlay/filter_row_normal", 60, 14);
    private static final Tex FILTER_ROW_HOVER = tex("overlay/filter_row_hover", 60, 14);
    private static final Tex FILTER_ROW_SELECTED = tex("overlay/filter_row_selected", 60, 14);
    private static final Tex SCROLL_TRACK = tex("overlay/scroll_track", 4, 84);
    private static final Tex SCROLL_THUMB = tex("overlay/scroll_thumb", 4, 20);
    private static final Tex SORT_BUTTON_NORMAL = tex("overlay/sort_button_normal", 44, 14);
    private static final Tex SORT_BUTTON_SELECTED = tex("overlay/sort_button_selected", 44, 14);
    private static final Tex CHOICE_ARROW_LEFT = tex("overlay/choice_arrow_left", 8, 8);
    private static final Tex CHOICE_ARROW_RIGHT = tex("overlay/choice_arrow_right", 8, 8);
    private static final Tex TOGGLE_OFF = tex("overlay/toggle_off", 24, 12);
    private static final Tex TOGGLE_ON = tex("overlay/toggle_on", 24, 12);
    private static final Tex TOGGLE_HOVER = tex("overlay/toggle_hover", 24, 12);
    private static final Tex RESULT_BADGE_WIN = tex("overlay/result_badge_win", 28, 10);
    private static final Tex RESULT_BADGE_LOSS = tex("overlay/result_badge_loss", 28, 10);
    private static final Tex RESULT_BADGE_DRAW = tex("overlay/result_badge_draw", 28, 10);
    private static final Tex CLOSE_HOVER = tex("overlay/close_hover", 16, 16);
    private static final Tex SEARCH_FOCUS = tex("overlay/search_focus", 92, 16);
    private static final Tex RANKING_ROW_HOVER = tex("overlay/ranking_row_hover", 228, 14);
    private static final Tex RANKING_ROW_SELECTED = tex("overlay/ranking_row_selected", 228, 14);
    private static final Tex HISTORY_ROW_HOVER = tex("overlay/history_row_hover", 228, 16);
    private static final Tex HISTORY_ROW_SELECTED = tex("overlay/history_row_selected", 228, 16);

    private static final Tex ICON_MODE_OVERALL = tex("icons/modes/overall", 8, 8);
    private static final Tex ICON_MODE_TDM = tex("icons/modes/team_deathmatch", 8, 8);
    private static final Tex ICON_MODE_FFA = tex("icons/modes/free_for_all", 8, 8);
    private static final Tex ICON_MODE_CTF = tex("icons/modes/capture_the_flag", 8, 8);
    private static final Tex ICON_MODE_DOMINATION = tex("icons/modes/domination", 8, 8);
    private static final Tex ICON_MODE_GUN_GAME = tex("icons/modes/gun_game", 8, 8);
    private static final Tex ICON_MODE_SNIPER = tex("icons/modes/sniper_only", 8, 8);
    private static final Tex ICON_MODE_PISTOL = tex("icons/modes/pistol_only", 8, 8);
    private static final Tex ICON_MODE_SHOTGUN = tex("icons/modes/shotgun_only", 8, 8);
    private static final Tex ICON_MODE_RANKED = tex("icons/modes/ranked", 8, 8);
    private static final Tex ICON_MODE_CASUAL = tex("icons/modes/casual", 8, 8);
    private static final Tex ICON_MODE_EVENT = tex("icons/modes/event", 8, 8);
    private static final Tex ICON_MODE_FAVORITE = tex("icons/modes/favorite", 8, 8);
    private static final Tex ICON_MODE_FALLBACK = tex("icons/modes/fallback_mode", 8, 8);
    private static final Tex ICON_KDR = tex("icons/stats/kdr", 16, 16);
    private static final Tex ICON_WIN_RATE = tex("icons/stats/win_rate", 16, 16);
    private static final Tex ICON_MATCHES = tex("icons/stats/matches", 16, 16);
    private static final Tex ICON_KILLS = tex("icons/stats/kills", 16, 16);
    private static final Tex ICON_DEATHS = tex("icons/stats/deaths", 16, 16);
    private static final Tex ICON_WINS = tex("icons/stats/wins", 16, 16);
    private static final Tex ICON_LOSSES = tex("icons/stats/losses", 16, 16);

    private static final Rect CLOSE_RECT = new Rect(298, 4, 16, 16);
    private static final Rect SEARCH_RECT = new Rect(202, 4, 92, 16);
    private static final Rect TAB_OVERVIEW = new Rect(82, 28, 54, 14);
    private static final Rect TAB_HISTORY = new Rect(138, 28, 54, 14);
    private static final Rect TAB_RANKING = new Rect(194, 28, 54, 14);
    private static final Rect TAB_SETTINGS = new Rect(250, 28, 54, 14);
    private static final Rect CONTENT_RECT = new Rect(5, 26, 70, 18);
    private static final Rect OVERVIEW_ALL_MODE = new Rect(6, 62, 66, 14);
    private static final Rect OVERVIEW_MODE_TRACK = new Rect(70, 108, 4, 70);
    private static final Rect HISTORY_TRACK = new Rect(312, 78, 4, 84);
    private static final Rect RANKING_TRACK = new Rect(70, 64, 4, 84);
    private static final Rect SORT_KILLS = new Rect(168, 64, 67, 14);
    private static final Rect SORT_WIN_RATE = new Rect(237, 64, 67, 14);
    private static final Rect RANK_SUBTAB_RANKING = new Rect(84, 48, 68, 14);
    private static final Rect RANK_SUBTAB_BADGES = new Rect(154, 48, 68, 14);
    private static final Rect SETTINGS_CATEGORY_PRIVACY = new Rect(6, 64, 60, 14);
    private static final Rect SETTINGS_CATEGORY_DISPLAY = new Rect(6, 80, 60, 14);
    private static final Rect SETTING_SHOW_RANK = new Rect(92, 70, 210, 18);
    private static final Rect SETTING_SHOW_STATS = new Rect(92, 94, 210, 18);
    private static final Rect SETTING_SHOW_MATCH_HISTORY = new Rect(92, 118, 210, 18);
    private static final Rect SETTING_JOIN_LEADERBOARDS = new Rect(92, 142, 210, 18);
    private static final Rect SETTING_LOADOUT_THEME = new Rect(92, 70, 210, 18);
    private static final Rect SETTING_DAMAGE_FEEDBACK_MODE = new Rect(92, 94, 210, 18);

    private static final int MODE_ROW_Y = 108;
    private static final int MODE_ROW_HEIGHT = 14;
    private static final int MODE_VISIBLE_ROWS = 5;
    private static final int HISTORY_ROW_Y = 78;
    private static final int HISTORY_ROW_HEIGHT = 16;
    private static final int HISTORY_VISIBLE_ROWS = 5;
    private static final int RANKING_ROW_Y = 94;
    private static final int RANKING_ROW_HEIGHT = 14;
    private static final int RANKING_VISIBLE_ROWS = 5;
    private static final SimpleDateFormat HISTORY_DATE_FORMAT = new SimpleDateFormat("MM/dd HH:mm", Locale.ROOT);

    private StatsGuiPayload payload;
    private int leftPos;
    private int topPos;
    private Page currentPage = Page.OVERVIEW;
    private String selectedModeId;
    private String selectedRankingModeId;
    private int modeScroll;
    private int historyScroll;
    private int rankingModeScroll;
    private int selectedHistoryIndex = -1;
    private HistoryFilter historyFilter = HistoryFilter.ALL;
    private RankingSort rankingSort = RankingSort.KILLS;
    private RankSubPage rankSubPage = RankSubPage.RANKING;
    private SettingsCategory settingsCategory = SettingsCategory.PRIVACY;
    private EditBox searchBox;
    private String searchText = "";

    public PvpStatsScreen(StatsGuiPayload payload) {
        super(Component.translatable(key("title")));
        this.payload = payload;
    }

    public void refreshPayload(StatsGuiPayload payload) {
        if (payload == null) {
            return;
        }
        this.payload = payload;
        this.selectedHistoryIndex = -1;
        this.modeScroll = Mth.clamp(this.modeScroll, 0, Math.max(0, filteredModes().size() - MODE_VISIBLE_ROWS));
        this.historyScroll = Mth.clamp(this.historyScroll, 0, Math.max(0, filteredHistoryRows().size() - HISTORY_VISIBLE_ROWS));
        this.rankingModeScroll = Mth.clamp(this.rankingModeScroll, 0, Math.max(0, rankingModes().size() - RANKING_VISIBLE_ROWS));
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - GUI_W) / 2;
        this.topPos = (this.height - GUI_H) / 2;
        this.searchBox = addRenderableWidget(new EditBox(
                this.font,
                this.leftPos + 216,
                this.topPos + 6,
                74,
                12,
                Component.translatable(key("search"))
        ));
        this.searchBox.setBordered(false);
        this.searchBox.setMaxLength(64);
        this.searchBox.setTextColor(0xD8F8FF);
        this.searchBox.setValue(this.searchText);
        this.searchBox.setResponder(this::onSearchChanged);
        updateSearchBoxState();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateSearchBoxState();
        this.renderBackground(guiGraphics);
        renderFrame(guiGraphics);
        renderHeader(guiGraphics, mouseX, mouseY);
        renderTabs(guiGraphics, mouseX, mouseY);

        switch (this.currentPage) {
            case OVERVIEW -> renderOverview(guiGraphics, mouseX, mouseY);
            case HISTORY -> renderHistory(guiGraphics, mouseX, mouseY);
            case RANKING -> renderRanking(guiGraphics, mouseX, mouseY);
            case SETTINGS -> renderSettings(guiGraphics, mouseX, mouseY);
        }

        renderFooter(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderFrame(GuiGraphics gg) {
        ResourceLocation background = switch (this.currentPage) {
            case OVERVIEW -> OVERVIEW_BG;
            case HISTORY -> HISTORY_BG;
            case RANKING -> RANKING_BG;
            case SETTINGS -> SETTINGS_BG;
        };
        gg.blit(background, this.leftPos, this.topPos, GUI_W, GUI_H, 0.0F, 0.0F, 1280, 840, 1280, 840);
    }

    private void renderHeader(GuiGraphics gg, int mouseX, int mouseY) {
        drawTrimmed(gg, this.title.getString(), this.leftPos + 28, this.topPos + 7, 124, 0xF2FBFF);

        if (this.currentPage != Page.SETTINGS) {
            if (this.searchBox != null && (this.searchBox.isFocused() || isHovering(SEARCH_RECT, mouseX, mouseY))) {
                blit(gg, SEARCH_FOCUS, this.leftPos + SEARCH_RECT.x, this.topPos + SEARCH_RECT.y);
            }
            if (this.searchBox != null && this.searchBox.getValue().isBlank() && !this.searchBox.isFocused()) {
                drawTrimmed(gg, text("search"), this.leftPos + 216, this.topPos + 7, 72, 0x7898A4);
            }
        }

        boolean hoverClose = isHovering(CLOSE_RECT, mouseX, mouseY);
        if (hoverClose) {
            blit(gg, CLOSE_HOVER, this.leftPos + CLOSE_RECT.x, this.topPos + CLOSE_RECT.y);
        }
    }

    private void renderTabs(GuiGraphics gg, int mouseX, int mouseY) {
        boolean contentHover = isHovering(CONTENT_RECT, mouseX, mouseY);
        int x = this.leftPos + CONTENT_RECT.x;
        int y = this.topPos + CONTENT_RECT.y;
        int right = x + CONTENT_RECT.w;
        int bottom = y + CONTENT_RECT.h;

        // Opaque frame, inset face, top shine and bottom shadow make this read as a button.
        gg.fill(x, y, right, bottom, contentHover ? 0xFFFFC247 : 0xFF8E7430);
        gg.fill(x + 1, y + 1, right - 1, bottom - 1, 0xFF0A0E0E);
        gg.fill(x + 2, y + 2, right - 2, bottom - 2, contentHover ? 0xFF3B331D : 0xFF24251B);
        gg.fill(x + 2, y + 2, right - 2, y + 3, contentHover ? 0xFFFFD268 : 0xFFC6A344);
        gg.fill(x + 2, bottom - 3, right - 2, bottom - 2, 0xFF090B09);
        gg.fill(x + 2, y + 3, x + 4, bottom - 3, contentHover ? 0xFFFFC247 : 0xFFD5A92E);

        String info = "お知らせ・ルール";
        gg.drawString(font, info, x + (CONTENT_RECT.w - font.width(info)) / 2 + 1,
                y + 5, contentHover ? 0xFFFFF1C4 : 0xFFE8D9A4, true);
        renderPageTab(gg, TAB_OVERVIEW, Page.OVERVIEW, mouseX, mouseY);
        renderPageTab(gg, TAB_HISTORY, Page.HISTORY, mouseX, mouseY);
        renderPageTab(gg, TAB_RANKING, Page.RANKING, mouseX, mouseY);
        renderPageTab(gg, TAB_SETTINGS, Page.SETTINGS, mouseX, mouseY);
    }

    private void renderPageTab(GuiGraphics gg, Rect rect, Page page, int mouseX, int mouseY) {
        boolean selected = this.currentPage == page;
        boolean hovering = isHovering(rect, mouseX, mouseY);
        if (selected || hovering) {
            blit(gg, selected ? TAB_SELECTED : TAB_HOVER, this.leftPos + rect.x, this.topPos + rect.y);
        }
        String label = text(page.key);
        int tx = this.leftPos + rect.x + (rect.w - this.font.width(label)) / 2;
        gg.drawString(this.font, label, tx, this.topPos + rect.y + 3, selected ? 0xFFFFFF : 0xBFD6DE, false);
    }

    private void renderOverview(GuiGraphics gg, int mouseX, int mouseY) {
        renderOverviewSidebar(gg, mouseX, mouseY);
        if (!this.payload.statsVisible()) {
            renderNotice(gg, 88, 82, 210, 38, Component.translatable(key("private.stats")));
            return;
        }
        renderOverviewContent(gg);
    }

    private void renderOverviewSidebar(GuiGraphics gg, int mouseX, int mouseY) {
        drawTrimmed(gg, text("filter"), this.leftPos + 6, this.topPos + 50, 64, 0x9FD9E9);
        renderModeRow(gg, OVERVIEW_ALL_MODE, Component.translatable(key("mode.overall")), ICON_MODE_OVERALL, this.selectedModeId == null, true, mouseX, mouseY);

        blit(gg, ICON_MODE_FAVORITE, this.leftPos + 8, this.topPos + 81);
        drawTrimmed(gg, text("favorites"), this.leftPos + 20, this.topPos + 82, 50, 0x66818A);
        drawTrimmed(gg, text("modes"), this.leftPos + 6, this.topPos + 96, 60, 0x9FD9E9);

        List<Map.Entry<String, AggregateStats>> entries = filteredModes();
        this.modeScroll = Mth.clamp(this.modeScroll, 0, Math.max(0, entries.size() - MODE_VISIBLE_ROWS));
        int visible = Math.min(MODE_VISIBLE_ROWS, entries.size());
        for (int i = 0; i < visible; i++) {
            int idx = this.modeScroll + i;
            Map.Entry<String, AggregateStats> entry = entries.get(idx);
            Rect row = new Rect(6, MODE_ROW_Y + i * MODE_ROW_HEIGHT, 60, MODE_ROW_HEIGHT);
            renderModeRow(
                    gg,
                    row,
                    Component.literal(toModeDisplayName(entry.getKey())),
                    modeIcon(entry.getKey()),
                    entry.getKey().equals(this.selectedModeId),
                    true,
                    mouseX,
                    mouseY
            );
        }
        renderScrollBar(gg, OVERVIEW_MODE_TRACK, entries.size(), MODE_VISIBLE_ROWS, this.modeScroll);
    }

    private void renderOverviewContent(GuiGraphics gg) {
        AggregateStats stats = selectedOverviewStats();
        String modeName = this.selectedModeId == null ? text("mode.overall") : toModeDisplayName(this.selectedModeId);

        int faceX = this.leftPos + 88;
        int faceY = this.topPos + 50;
        gg.fill(faceX - 1, faceY - 1, faceX + 17, faceY + 17, 0xFF29414C);
        PlayerFaceRenderer.draw(gg, playerSkin(), faceX, faceY, 16);
        drawTrimmed(gg, I18n.get(key("player"), safeTargetName()), this.leftPos + 108, this.topPos + 51, 78, 0xF2FBFF);
        drawTrimmed(gg, I18n.get(key("mode_label"), modeName), this.leftPos + 190, this.topPos + 51, 110, 0xBFD6DE);

        String rankMode = this.selectedModeId == null ? RankingEntry.OVERALL_MODE_ID : this.selectedModeId;
        RankState rank = this.payload.ranks().getOrDefault(rankMode, RankState.INITIAL);
        String tierName = I18n.get(key("rank.tier." + rank.tier().serializedName()));
        boolean rankPrivate = !this.payload.editableSettings() && !this.payload.privacySettings().showRank();
        String rankText = rankPrivate ? text("rank.private") : RankSystem.ENABLED
                ? I18n.get(key("rank.summary"), tierName, rank.rating())
                : I18n.get(key("rank.disabled_preview"), tierName, rank.rating());
        drawTrimmed(gg, rankText, this.leftPos + 108, this.topPos + 64, 190, RankSystem.ENABLED ? 0xFFE59A : 0x7898A4);

        renderMainCard(gg, new Rect(84, 82, 72, 44), ICON_KDR, key("stat.kdr"), formatKdr(stats), 0xE9FDFF);
        renderMainCard(gg, new Rect(162, 82, 72, 44), ICON_WIN_RATE, key("stat.win_rate"), formatWinRate(stats), 0xEBFFE9);
        renderMainCard(gg, new Rect(240, 82, 72, 44), ICON_MATCHES, key("stat.matches"), formatCount(stats.matches()), 0xFFF7C6);

        renderSmallCard(gg, new Rect(84, 136, 54, 44), ICON_KILLS, key("stat.kills"), formatCount(stats.kills()));
        renderSmallCard(gg, new Rect(142, 136, 54, 44), ICON_DEATHS, key("stat.deaths"), formatCount(stats.deaths()));
        renderSmallCard(gg, new Rect(200, 136, 54, 44), ICON_WINS, key("stat.wins"), formatCount(stats.wins()));
        renderSmallCard(gg, new Rect(258, 136, 54, 44), ICON_LOSSES, key("stat.losses"), formatCount(stats.losses()));

        if (!this.payload.hasAnyData()) {
            drawTrimmed(gg, text("no_data"), this.leftPos + 88, this.topPos + 182, 210, 0xCFAAAA);
        }
    }

    private void renderMainCard(GuiGraphics gg, Rect rect, Tex icon, String titleKey, String value, int valueColor) {
        int x = this.leftPos + rect.x;
        int y = this.topPos + rect.y;
        drawTrimmed(gg, text(titleKey), x + 6, y + 5, 58, 0xBCD3DA);
        blit(gg, icon, x + 8, y + 16);
        drawTrimmed(gg, value, x + 28, y + 18, 38, valueColor);
    }

    private void renderSmallCard(GuiGraphics gg, Rect rect, Tex icon, String titleKey, String value) {
        int x = this.leftPos + rect.x;
        int y = this.topPos + rect.y;
        blit(gg, icon, x + 19, y + 5);
        drawCenteredTrimmed(gg, text(titleKey), x + 5, y + 24, 44, 0xBCD3DA);
        drawCenteredTrimmed(gg, value, x + 5, y + 34, 44, 0xF2FBFF);
    }

    private void renderHistory(GuiGraphics gg, int mouseX, int mouseY) {
        renderHistoryFilters(gg, mouseX, mouseY);
        if (!this.payload.historyVisible()) {
            renderNotice(gg, 88, 82, 210, 38, Component.translatable(key("private.history")));
            return;
        }
        renderHistoryTable(gg, mouseX, mouseY);
    }

    private void renderHistoryFilters(GuiGraphics gg, int mouseX, int mouseY) {
        drawTrimmed(gg, text("filter"), this.leftPos + 6, this.topPos + 50, 64, 0x9FD9E9);
        HistoryFilter[] filters = HistoryFilter.values();
        for (int i = 0; i < filters.length; i++) {
            HistoryFilter filter = filters[i];
            Rect row = new Rect(6, 64 + i * MODE_ROW_HEIGHT, 60, MODE_ROW_HEIGHT);
            renderTextButton(gg, row, Component.translatable(filter.key), this.historyFilter == filter, true, mouseX, mouseY);
        }
    }

    private void renderHistoryTable(GuiGraphics gg, int mouseX, int mouseY) {
        List<HistoryRow> rows = filteredHistoryRows();
        this.historyScroll = Mth.clamp(this.historyScroll, 0, Math.max(0, rows.size() - HISTORY_VISIBLE_ROWS));
        if (this.selectedHistoryIndex < 0 && !rows.isEmpty()) {
            this.selectedHistoryIndex = rows.get(0).sourceIndex();
        }

        drawTrimmed(gg, text("history.title"), this.leftPos + 84, this.topPos + 50, 100, 0xF2FBFF);
        drawTrimmed(gg, text("history.header.time"), this.leftPos + 88, this.topPos + 68, 38, 0x85AAB5);
        drawTrimmed(gg, text("history.header.mode"), this.leftPos + 140, this.topPos + 68, 56, 0x85AAB5);
        drawTrimmed(gg, text("history.header.result"), this.leftPos + 213, this.topPos + 68, 34, 0x85AAB5);
        drawTrimmed(gg, text("history.header.kd"), this.leftPos + 248, this.topPos + 68, 30, 0x85AAB5);
        drawTrimmed(gg, text("history.header.kdr"), this.leftPos + 286, this.topPos + 68, 24, 0x85AAB5);

        int visible = Math.min(HISTORY_VISIBLE_ROWS, rows.size());
        for (int i = 0; i < visible; i++) {
            HistoryRow rowData = rows.get(this.historyScroll + i);
            MatchRecord record = rowData.record();
            Rect row = new Rect(84, HISTORY_ROW_Y + i * HISTORY_ROW_HEIGHT, 228, HISTORY_ROW_HEIGHT);
            boolean selected = rowData.sourceIndex() == this.selectedHistoryIndex;
            boolean hovering = isHovering(row, mouseX, mouseY);
            if (selected || hovering) {
                blit(gg, selected ? HISTORY_ROW_SELECTED : HISTORY_ROW_HOVER, this.leftPos + row.x, this.topPos + row.y);
            }

            drawTrimmed(gg, formatDate(record.timestamp()), this.leftPos + row.x + 4, this.topPos + row.y + 4, 38, 0xBCD3DA);
            blit(gg, modeIcon(record.modeId()), this.leftPos + row.x + 44, this.topPos + row.y + 4);
            drawTrimmed(gg, toModeDisplayName(record.modeId()), this.leftPos + row.x + 56, this.topPos + row.y + 4, 66, 0xF2FBFF);
            renderResultBadge(gg, record.result(), this.leftPos + row.x + 128, this.topPos + row.y + 3);
            drawTrimmed(gg, record.kills() + "/" + record.deaths(), this.leftPos + row.x + 164, this.topPos + row.y + 4, 32, 0xD8E8ED);
            drawTrimmed(gg, formatKdr(record.kills(), record.deaths()), this.leftPos + row.x + 202, this.topPos + row.y + 4, 24, 0xD8E8ED);
        }

        if (rows.isEmpty()) {
            drawTrimmed(gg, text("history.empty"), this.leftPos + 92, this.topPos + 92, 200, 0x86A7B2);
        }

        renderScrollBar(gg, HISTORY_TRACK, rows.size(), HISTORY_VISIBLE_ROWS, this.historyScroll);
        renderMatchDetail(gg, rows);
    }

    private void renderResultBadge(GuiGraphics gg, String result, int x, int y) {
        ResultKind kind = ResultKind.from(result);
        blit(gg, kind.badge, x, y);
        String label = text(kind.shortKey);
        int tx = x + (28 - this.font.width(label)) / 2;
        gg.drawString(this.font, label, tx, y + 1, kind.textColor, false);
    }

    private void renderMatchDetail(GuiGraphics gg, List<HistoryRow> rows) {
        gg.fill(this.leftPos + 85, this.topPos + 167, this.leftPos + 311, this.topPos + 185, 0x8A07151B);
        gg.fill(this.leftPos + 85, this.topPos + 167, this.leftPos + 311, this.topPos + 168, 0x9900DDFD);
        HistoryRow selected = rows.stream()
                .filter(row -> row.sourceIndex() == this.selectedHistoryIndex)
                .findFirst()
                .orElse(rows.isEmpty() ? null : rows.get(0));
        if (selected == null) {
            drawTrimmed(gg, text("history.empty"), this.leftPos + 90, this.topPos + 173, 210, 0x86A7B2);
            return;
        }

        MatchRecord record = selected.record();
        String detail = I18n.get(
                key("history.detail"),
                formatDate(record.timestamp()),
                toModeDisplayName(record.modeId()),
                toResultDisplayName(record.result()),
                record.kills(),
                record.deaths(),
                formatKdr(record.kills(), record.deaths())
        );
        drawTrimmed(gg, detail, this.leftPos + 90, this.topPos + 173, 216, 0xD8E8ED);
    }

    private void renderRanking(GuiGraphics gg, int mouseX, int mouseY) {
        renderRankSubTab(gg, RANK_SUBTAB_RANKING, RankSubPage.RANKING, mouseX, mouseY);
        renderRankSubTab(gg, RANK_SUBTAB_BADGES, RankSubPage.BADGES, mouseX, mouseY);
        if (this.rankSubPage == RankSubPage.RANKING) {
            renderRankingSidebar(gg, mouseX, mouseY);
            renderRankingTable(gg, mouseX, mouseY);
        } else {
            renderBadgePage(gg);
        }
    }

    private void renderRankSubTab(GuiGraphics gg, Rect rect, RankSubPage page, int mouseX, int mouseY) {
        boolean selected = this.rankSubPage == page;
        if (selected || isHovering(rect, mouseX, mouseY)) {
            gg.fill(this.leftPos + rect.x, this.topPos + rect.y,
                    this.leftPos + rect.x + rect.w, this.topPos + rect.y + rect.h,
                    selected ? 0xCC0B3742 : 0x88304A55);
        }
        drawCenteredTrimmed(gg, text(page.key), this.leftPos + rect.x + 2, this.topPos + rect.y + 4,
                rect.w - 4, selected ? 0xFFFFFF : 0xBFD6DE);
    }

    private void renderBadgePage(GuiGraphics gg) {
        drawTrimmed(gg, text("badge.category"), this.leftPos + 6, this.topPos + 50, 64, 0x9FD9E9);
        renderTextButton(gg, new Rect(6, 64, 60, MODE_ROW_HEIGHT),
                Component.translatable(key("badge.category.combat")), true, false, -1, -1);
        drawTrimmed(gg, text("badge.title"), this.leftPos + 84, this.topPos + 68, 150, 0xF2FBFF);
        drawTrimmed(gg, text("badge.disabled"), this.leftPos + 206, this.topPos + 68, 100, 0x7898A4);

        List<BadgeDefinition> badges = BadgeSystem.DEFINITIONS;
        for (int i = 0; i < badges.size(); i++) {
            BadgeDefinition badge = badges.get(i);
            int x = this.leftPos + 84 + (i % 2) * 114;
            int y = this.topPos + 82 + (i / 2) * 31;
            gg.fill(x, y, x + 108, y + 27, 0xA00A1C23);
            gg.fill(x, y, x + 2, y + 27, 0xFF435B66);
            drawTrimmed(gg, I18n.get(badge.translationKey()), x + 7, y + 4, 94, 0xBFD6DE);
            drawTrimmed(gg, text("badge.not_implemented"), x + 7, y + 15, 94, 0x7898A4);
        }
    }

    private void renderRankingSidebar(GuiGraphics gg, int mouseX, int mouseY) {
        drawTrimmed(gg, text("ranking.mode_filter"), this.leftPos + 6, this.topPos + 50, 64, 0x9FD9E9);
        List<String> modes = rankingModes();
        this.rankingModeScroll = Mth.clamp(this.rankingModeScroll, 0, Math.max(0, modes.size() - RANKING_VISIBLE_ROWS));
        int visible = Math.min(RANKING_VISIBLE_ROWS, modes.size());
        for (int i = 0; i < visible; i++) {
            String modeId = modes.get(this.rankingModeScroll + i);
            boolean overall = RankingEntry.OVERALL_MODE_ID.equals(modeId);
            Rect row = new Rect(6, 64 + i * MODE_ROW_HEIGHT, 60, MODE_ROW_HEIGHT);
            renderModeRow(
                    gg,
                    row,
                    Component.literal(overall ? text("mode.overall") : toModeDisplayName(modeId)),
                    overall ? ICON_MODE_OVERALL : modeIcon(modeId),
                    modeId.equals(selectedRankingMode()),
                    true,
                    mouseX,
                    mouseY
            );
        }
        renderScrollBar(gg, RANKING_TRACK, modes.size(), RANKING_VISIBLE_ROWS, this.rankingModeScroll);
    }

    private void renderRankingTable(GuiGraphics gg, int mouseX, int mouseY) {
        String selectedModeId = selectedRankingMode();
        boolean overall = RankingEntry.OVERALL_MODE_ID.equals(selectedModeId);
        PvpModeDefinition selectedDefinition = overall ? null : modeDefinition(selectedModeId);
        boolean rankingDisabled = selectedDefinition != null && !selectedDefinition.rankingEnabled();
        if (rankingDisabled) {
            renderNotice(gg, 90, 92, 210, 34, Component.translatable(key("ranking.disabled")));
            return;
        }

        List<RankingEntry> entries = sortedRankingEntries();
        renderSortButton(gg, SORT_KILLS, RankingSort.KILLS, mouseX, mouseY);
        renderSortButton(gg, SORT_WIN_RATE, RankingSort.WIN_RATE, mouseX, mouseY);

        drawTrimmed(gg, text("ranking.header.rank"), this.leftPos + 88, this.topPos + 84, 24, 0x85AAB5);
        drawTrimmed(gg, text("ranking.header.player"), this.leftPos + 114, this.topPos + 84, 76, 0x85AAB5);
        drawTrimmed(gg, text("ranking.header.kills"), this.leftPos + 198, this.topPos + 84, 32, 0x85AAB5);
        drawTrimmed(gg, text("ranking.header.win_rate"), this.leftPos + 236, this.topPos + 84, 36, 0x85AAB5);
        drawTrimmed(gg, text("ranking.header.matches"), this.leftPos + 278, this.topPos + 84, 30, 0x85AAB5);

        int visible = Math.min(RANKING_VISIBLE_ROWS, entries.size());
        for (int i = 0; i < visible; i++) {
            RankingEntry entry = entries.get(i);
            int rowY = this.topPos + RANKING_ROW_Y + i * RANKING_ROW_HEIGHT;
            boolean hovering = mouseX >= this.leftPos + 84 && mouseX < this.leftPos + 312 && mouseY >= rowY && mouseY < rowY + RANKING_ROW_HEIGHT;
            if (hovering) {
                blit(gg, RANKING_ROW_HOVER, this.leftPos + 84, rowY);
            }
            drawTrimmed(gg, "#" + (i + 1), this.leftPos + 88, rowY + 3, 24, 0xF2FBFF);
            drawTrimmed(gg, entry.mcid(), this.leftPos + 114, rowY + 3, 78, 0xD8E8ED);
            drawTrimmed(gg, formatCount(entry.kills()), this.leftPos + 198, rowY + 3, 32, 0xD8E8ED);
            drawTrimmed(gg, formatWinRate(entry.wins(), entry.matches()), this.leftPos + 236, rowY + 3, 36, 0xD8E8ED);
            drawTrimmed(gg, formatCount(entry.matches()), this.leftPos + 278, rowY + 3, 30, 0xD8E8ED);
        }

        if (entries.isEmpty()) {
            renderNotice(gg, 90, 92, 210, 34, Component.translatable(key("ranking.empty")));
        }

        drawTrimmed(gg, currentPlayerRankingLine(entries), this.leftPos + 90, this.topPos + 174, 216, 0xD8E8ED);
    }

    private void renderSortButton(GuiGraphics gg, Rect rect, RankingSort sort, int mouseX, int mouseY) {
        boolean selected = this.rankingSort == sort;
        blitScaled(gg, selected ? SORT_BUTTON_SELECTED : SORT_BUTTON_NORMAL,
                this.leftPos + rect.x, this.topPos + rect.y, rect.w, rect.h);
        String label = text(sort.key);
        int tx = this.leftPos + rect.x + (rect.w - this.font.width(label)) / 2;
        gg.drawString(this.font, label, tx, this.topPos + rect.y + 3, selected ? 0xFFFFFF : 0xBFD6DE, false);
    }

    private void renderSettings(GuiGraphics gg, int mouseX, int mouseY) {
        drawTrimmed(gg, text("settings.category"), this.leftPos + 6, this.topPos + 50, 64, 0x9FD9E9);
        renderTextButton(gg, SETTINGS_CATEGORY_PRIVACY,
                Component.translatable(key("settings.category.privacy")), this.settingsCategory == SettingsCategory.PRIVACY,
                true, mouseX, mouseY);
        renderTextButton(gg, SETTINGS_CATEGORY_DISPLAY,
                Component.translatable(key("settings.category.display")), this.settingsCategory == SettingsCategory.DISPLAY,
                true, mouseX, mouseY);

        if (this.settingsCategory == SettingsCategory.DISPLAY) {
            drawTrimmed(gg, text("settings.display.title"), this.leftPos + 92, this.topPos + 54, 120, 0xF2FBFF);
            renderChoiceSettingRow(gg, SETTING_LOADOUT_THEME, key("setting.loadout_theme"),
                    loadoutThemeLabel(ClientConfig.LOADOUT_THEME_MODE.get()), mouseX, mouseY);
            renderChoiceSettingRow(gg, SETTING_DAMAGE_FEEDBACK_MODE, key("setting.damage_feedback_mode"),
                    damageFeedbackModeLabel(ClientConfig.DAMAGE_FEEDBACK_MODE.get()), mouseX, mouseY);
        } else {
            drawTrimmed(gg, text("settings.title"), this.leftPos + 92, this.topPos + 54, 120, 0xF2FBFF);
            PlayerPrivacySettings settings = this.payload.privacySettings();
            renderSettingRow(gg, SETTING_SHOW_RANK, key("setting.show_rank"), settings.showRank(), mouseX, mouseY);
            renderSettingRow(gg, SETTING_SHOW_STATS, key("setting.show_stats"), settings.showStats(), mouseX, mouseY);
            renderSettingRow(gg, SETTING_SHOW_MATCH_HISTORY, key("setting.show_match_history"), settings.showMatchHistory(), mouseX, mouseY);
            renderSettingRow(gg, SETTING_JOIN_LEADERBOARDS, key("setting.join_leaderboards"), settings.joinLeaderboards(), mouseX, mouseY);
            if (!this.payload.editableSettings()) {
                drawTrimmed(gg, text("settings.not_editable"), this.leftPos + 100, this.topPos + 173, 194, 0xBFD6DE);
            }
        }
    }

    private void renderSettingRow(GuiGraphics gg, Rect rect, String labelKey, boolean enabled, int mouseX, int mouseY) {
        boolean hovering = isHovering(rect, mouseX, mouseY) && this.payload.editableSettings();
        int x = this.leftPos + rect.x;
        int y = this.topPos + rect.y;
        if (hovering) {
            gg.fill(x + 3, y + 2, x + rect.w - 3, y + rect.h - 2, 0x66304A55);
        }
        int toggleX = x + rect.w - 28;
        blit(gg, enabled ? TOGGLE_ON : TOGGLE_OFF, toggleX, y + 3);
        if (hovering) {
            blit(gg, TOGGLE_HOVER, toggleX, y + 3);
        }
        drawTrimmed(gg, text(labelKey), x + 8, y + 5, rect.w - 44, this.payload.editableSettings() ? 0xD8E8ED : 0x7898A4);
    }

    private void renderChoiceSettingRow(GuiGraphics gg, Rect rect, String labelKey, String value, int mouseX, int mouseY) {
        int x = this.leftPos + rect.x;
        int y = this.topPos + rect.y;
        Rect control = choiceControl(rect);
        boolean hovering = isHovering(control, mouseX, mouseY);
        drawTrimmed(gg, text(labelKey), x + 8, y + 5, 104, 0xD8E8ED);
        blitScaled(gg, hovering ? SORT_BUTTON_SELECTED : SORT_BUTTON_NORMAL,
                this.leftPos + control.x, this.topPos + control.y, control.w, control.h);
        blit(gg, CHOICE_ARROW_LEFT, this.leftPos + control.x + 3, this.topPos + control.y + 3);
        blit(gg, CHOICE_ARROW_RIGHT, this.leftPos + control.x + control.w - 11, this.topPos + control.y + 3);
        drawCenteredTrimmed(gg, value, this.leftPos + control.x + 14, this.topPos + control.y + 3,
                control.w - 28, hovering ? 0xFFFFFF : 0xE9FDFF);
    }

    private void renderFooter(GuiGraphics gg) {
        String footer;
        if (!this.payload.statsVisible()) {
            footer = text("private.stats.short");
        } else {
            footer = I18n.get(
                    key("footer.summary"),
                    safeTargetName(),
                    this.payload.modeStats().size(),
                    this.payload.recentMatches().size(),
                    this.payload.rankingEntries().size()
            );
        }
        drawTrimmed(gg, footer, this.leftPos + 8, this.topPos + 198, 304, 0x86A7B2);
    }

    private void renderModeRow(GuiGraphics gg, Rect rect, Component label, Tex icon, boolean selected, boolean enabled, int mouseX, int mouseY) {
        Tex rowTexture = selected ? MODE_ROW_SELECTED : isHovering(rect, mouseX, mouseY) ? MODE_ROW_HOVER : MODE_ROW_NORMAL;
        blit(gg, rowTexture, this.leftPos + rect.x, this.topPos + rect.y);
        blit(gg, icon, this.leftPos + rect.x + 3, this.topPos + rect.y + 3);
        drawTrimmed(gg, label.getString(), this.leftPos + rect.x + 14, this.topPos + rect.y + 4, rect.w - 17, enabled ? 0xD8E8ED : 0x6E8790);
    }

    private void renderTextButton(GuiGraphics gg, Rect rect, Component label, boolean selected, boolean enabled, int mouseX, int mouseY) {
        Tex rowTexture = selected ? FILTER_ROW_SELECTED : isHovering(rect, mouseX, mouseY) ? FILTER_ROW_HOVER : FILTER_ROW_NORMAL;
        blit(gg, rowTexture, this.leftPos + rect.x, this.topPos + rect.y);
        drawCenteredTrimmed(gg, label.getString(), this.leftPos + rect.x + 3, this.topPos + rect.y + 4, rect.w - 6, enabled ? 0xD8E8ED : 0x6E8790);
    }

    private void renderScrollBar(GuiGraphics gg, Rect track, int total, int visibleRows, int scroll) {
        blitScaled(gg, SCROLL_TRACK, this.leftPos + track.x, this.topPos + track.y, track.w, track.h);
        if (total <= visibleRows || total <= 0) {
            return;
        }

        int thumbHeight = Math.max(10, track.h * visibleRows / total);
        int maxScroll = Math.max(1, total - visibleRows);
        int maxThumbTravel = Math.max(0, track.h - thumbHeight);
        int thumbY = this.topPos + track.y + (maxThumbTravel * scroll / maxScroll);
        blitScaled(gg, SCROLL_THUMB, this.leftPos + track.x, thumbY, track.w, thumbHeight);
    }

    private void renderNotice(GuiGraphics gg, int relX, int relY, int width, int height, Component message) {
        int x = this.leftPos + relX;
        int y = this.topPos + relY;
        gg.fill(x, y, x + width, y + height, 0xAA07151B);
        gg.fill(x, y, x + width, y + 1, 0xAA00DDFD);
        drawCenteredTrimmed(gg, message.getString(), x + 8, y + height / 2 - 4, width - 16, 0xBFD6DE);
    }

    private void renderTooltips(GuiGraphics gg, int mouseX, int mouseY) {
        if (isHovering(CLOSE_RECT, mouseX, mouseY)) {
            gg.renderTooltip(this.font, Component.translatable("gui.dpvptweaks.pvp_stats.tooltip.close"), mouseX, mouseY);
            return;
        }

        for (Page page : Page.values()) {
            if (isHovering(page.rect, mouseX, mouseY)) {
                gg.renderTooltip(this.font, Component.translatable(page.key), mouseX, mouseY);
                return;
            }
        }

        if (this.currentPage == Page.OVERVIEW && renderOverviewModeTooltip(gg, mouseX, mouseY)) {
            return;
        }

        if (this.currentPage == Page.RANKING && renderRankingModeTooltip(gg, mouseX, mouseY)) {
            return;
        }

        if (this.currentPage == Page.OVERVIEW && isHovering(new Rect(108, 50, 190, 24), mouseX, mouseY)) {
            gg.renderTooltip(this.font, Component.literal(safeTargetName()), mouseX, mouseY);
            return;
        }

        if (this.currentPage == Page.HISTORY && this.payload.historyVisible()) {
            List<HistoryRow> rows = filteredHistoryRows();
            int visible = Math.min(HISTORY_VISIBLE_ROWS, rows.size());
            for (int i = 0; i < visible; i++) {
                int idx = this.historyScroll + i;
                Rect row = new Rect(84, HISTORY_ROW_Y + i * HISTORY_ROW_HEIGHT, 228, HISTORY_ROW_HEIGHT);
                if (idx < rows.size() && isHovering(row, mouseX, mouseY)) {
                    MatchRecord record = rows.get(idx).record();
                    renderWrappedTooltip(gg, List.of(
                            Component.literal(toModeDisplayName(record.modeId())),
                            Component.literal(formatDate(record.timestamp())),
                            Component.literal(toResultDisplayName(record.result()) + "  " + record.kills() + "/" + record.deaths()),
                            Component.translatable(key("history.match_id"), record.matchId())
                    ), mouseX, mouseY);
                    return;
                }
            }
        }

        if (this.currentPage == Page.SETTINGS) {
            renderSettingsTooltip(gg, mouseX, mouseY);
        }
    }

    private boolean renderOverviewModeTooltip(GuiGraphics gg, int mouseX, int mouseY) {
        List<Map.Entry<String, AggregateStats>> entries = filteredModes();
        int visible = Math.min(MODE_VISIBLE_ROWS, entries.size());
        for (int i = 0; i < visible; i++) {
            int index = this.modeScroll + i;
            Rect row = new Rect(6, MODE_ROW_Y + i * MODE_ROW_HEIGHT, 60, MODE_ROW_HEIGHT);
            if (index < entries.size() && isHovering(row, mouseX, mouseY)) {
                return renderModeDescriptionTooltip(gg, entries.get(index).getKey(), mouseX, mouseY);
            }
        }
        return false;
    }

    private boolean renderRankingModeTooltip(GuiGraphics gg, int mouseX, int mouseY) {
        List<String> modes = rankingModes();
        int visible = Math.min(RANKING_VISIBLE_ROWS, modes.size());
        for (int i = 0; i < visible; i++) {
            int index = this.rankingModeScroll + i;
            Rect row = new Rect(6, 64 + i * MODE_ROW_HEIGHT, 60, MODE_ROW_HEIGHT);
            if (index < modes.size() && isHovering(row, mouseX, mouseY)) {
                return renderModeDescriptionTooltip(gg, modes.get(index), mouseX, mouseY);
            }
        }
        return false;
    }

    private boolean renderModeDescriptionTooltip(GuiGraphics gg, String modeId, int mouseX, int mouseY) {
        Component description = modeDescription(modeId);
        if (description == null) {
            return false;
        }
        renderWrappedTooltip(gg, List.of(
                Component.literal(toModeDisplayName(modeId)),
                description
        ), mouseX, mouseY);
        return true;
    }

    private void renderSettingsTooltip(GuiGraphics gg, int mouseX, int mouseY) {
        if (isHovering(SETTINGS_CATEGORY_PRIVACY, mouseX, mouseY)) {
            renderWrappedTooltip(gg, List.of(
                    Component.translatable(key("settings.category.privacy.tooltip"))
            ), mouseX, mouseY);
            return;
        }
        if (isHovering(SETTINGS_CATEGORY_DISPLAY, mouseX, mouseY)) {
            renderWrappedTooltip(gg, List.of(
                    Component.translatable(key("settings.category.display.tooltip"))
            ), mouseX, mouseY);
            return;
        }
        if (this.settingsCategory == SettingsCategory.DISPLAY) {
            if (renderSettingTooltip(gg, SETTING_LOADOUT_THEME, "setting.loadout_theme", mouseX, mouseY)) {
                return;
            }
            renderSettingTooltip(gg, SETTING_DAMAGE_FEEDBACK_MODE, "setting.damage_feedback_mode", mouseX, mouseY);
            return;
        }
        if (renderSettingTooltip(gg, SETTING_SHOW_RANK, "setting.show_rank", mouseX, mouseY)
                || renderSettingTooltip(gg, SETTING_SHOW_STATS, "setting.show_stats", mouseX, mouseY)
                || renderSettingTooltip(gg, SETTING_SHOW_MATCH_HISTORY, "setting.show_match_history", mouseX, mouseY)
                || renderSettingTooltip(gg, SETTING_JOIN_LEADERBOARDS, "setting.join_leaderboards", mouseX, mouseY)) {
            return;
        }
    }

    private boolean renderSettingTooltip(GuiGraphics gg, Rect rect, String settingKey, int mouseX, int mouseY) {
        if (!isHovering(rect, mouseX, mouseY)) {
            return false;
        }
        renderWrappedTooltip(gg, List.of(
                Component.translatable(key(settingKey)),
                Component.translatable(key(settingKey + ".tooltip"))
        ), mouseX, mouseY);
        return true;
    }

    private void renderWrappedTooltip(GuiGraphics gg, List<Component> lines, int mouseX, int mouseY) {
        List<FormattedCharSequence> wrappedLines = new ArrayList<>();
        for (Component line : lines) {
            wrappedLines.addAll(this.font.split(line, 180));
        }
        gg.renderTooltip(this.font, wrappedLines, mouseX, mouseY);
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

        if (isHovering(CONTENT_RECT, mouseX, mouseY)) {
            ContentClient.open(this);
            return true;
        }

        for (Page page : Page.values()) {
            if (isHovering(page.rect, mouseX, mouseY)) {
                setPage(page);
                return true;
            }
        }

        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        return switch (this.currentPage) {
            case OVERVIEW -> handleOverviewClick(mouseX, mouseY);
            case HISTORY -> handleHistoryClick(mouseX, mouseY);
            case RANKING -> handleRankingClick(mouseX, mouseY);
            case SETTINGS -> handleSettingsClick(mouseX, mouseY);
        };
    }

    private boolean handleOverviewClick(double mouseX, double mouseY) {
        if (isHovering(OVERVIEW_ALL_MODE, mouseX, mouseY)) {
            this.selectedModeId = null;
            return true;
        }

        List<Map.Entry<String, AggregateStats>> entries = filteredModes();
        int visible = Math.min(MODE_VISIBLE_ROWS, entries.size());
        for (int i = 0; i < visible; i++) {
            int idx = this.modeScroll + i;
            Rect row = new Rect(6, MODE_ROW_Y + i * MODE_ROW_HEIGHT, 60, MODE_ROW_HEIGHT);
            if (idx < entries.size() && isHovering(row, mouseX, mouseY)) {
                this.selectedModeId = entries.get(idx).getKey();
                return true;
            }
        }
        return false;
    }

    private boolean handleHistoryClick(double mouseX, double mouseY) {
        HistoryFilter[] filters = HistoryFilter.values();
        for (int i = 0; i < filters.length; i++) {
            Rect row = new Rect(6, 64 + i * MODE_ROW_HEIGHT, 60, MODE_ROW_HEIGHT);
            if (isHovering(row, mouseX, mouseY)) {
                this.historyFilter = filters[i];
                this.historyScroll = 0;
                return true;
            }
        }

        List<HistoryRow> rows = filteredHistoryRows();
        int visible = Math.min(HISTORY_VISIBLE_ROWS, rows.size());
        for (int i = 0; i < visible; i++) {
            int idx = this.historyScroll + i;
            Rect row = new Rect(84, HISTORY_ROW_Y + i * HISTORY_ROW_HEIGHT, 228, HISTORY_ROW_HEIGHT);
            if (idx < rows.size() && isHovering(row, mouseX, mouseY)) {
                this.selectedHistoryIndex = rows.get(idx).sourceIndex();
                return true;
            }
        }
        return false;
    }

    private boolean handleRankingClick(double mouseX, double mouseY) {
        if (isHovering(RANK_SUBTAB_RANKING, mouseX, mouseY)) {
            this.rankSubPage = RankSubPage.RANKING;
            return true;
        }
        if (isHovering(RANK_SUBTAB_BADGES, mouseX, mouseY)) {
            this.rankSubPage = RankSubPage.BADGES;
            return true;
        }
        if (this.rankSubPage == RankSubPage.BADGES) {
            return false;
        }
        if (isHovering(SORT_KILLS, mouseX, mouseY)) {
            this.rankingSort = RankingSort.KILLS;
            return true;
        }
        if (isHovering(SORT_WIN_RATE, mouseX, mouseY)) {
            this.rankingSort = RankingSort.WIN_RATE;
            return true;
        }

        List<String> modes = rankingModes();
        int visible = Math.min(RANKING_VISIBLE_ROWS, modes.size());
        for (int i = 0; i < visible; i++) {
            int idx = this.rankingModeScroll + i;
            Rect row = new Rect(6, 64 + i * MODE_ROW_HEIGHT, 60, MODE_ROW_HEIGHT);
            if (idx < modes.size() && isHovering(row, mouseX, mouseY)) {
                this.selectedRankingModeId = modes.get(idx);
                return true;
            }
        }
        return false;
    }

    private boolean handleSettingsClick(double mouseX, double mouseY) {
        if (isHovering(SETTINGS_CATEGORY_PRIVACY, mouseX, mouseY)) {
            this.settingsCategory = SettingsCategory.PRIVACY;
            return true;
        }
        if (isHovering(SETTINGS_CATEGORY_DISPLAY, mouseX, mouseY)) {
            this.settingsCategory = SettingsCategory.DISPLAY;
            return true;
        }

        if (this.settingsCategory == SettingsCategory.DISPLAY) {
            Rect loadoutControl = choiceControl(SETTING_LOADOUT_THEME);
            if (isHovering(loadoutControl, mouseX, mouseY)) {
                cycleLoadoutTheme(isHovering(choiceLeft(loadoutControl), mouseX, mouseY) ? -1 : 1);
                return true;
            }
            Rect damageControl = choiceControl(SETTING_DAMAGE_FEEDBACK_MODE);
            if (isHovering(damageControl, mouseX, mouseY)) {
                cycleDamageFeedbackMode(isHovering(choiceLeft(damageControl), mouseX, mouseY) ? -1 : 1);
                return true;
            }
            return false;
        }

        if (!this.payload.editableSettings()) {
            return false;
        }

        PlayerPrivacySettings settings = this.payload.privacySettings();
        if (isHovering(SETTING_SHOW_RANK, mouseX, mouseY)) {
            submitSettings(new PlayerPrivacySettings(!settings.showRank(), settings.showStats(), settings.showMatchHistory(), settings.joinLeaderboards()));
            return true;
        }
        if (isHovering(SETTING_SHOW_STATS, mouseX, mouseY)) {
            submitSettings(new PlayerPrivacySettings(settings.showRank(), !settings.showStats(), settings.showMatchHistory(), settings.joinLeaderboards()));
            return true;
        }
        if (isHovering(SETTING_SHOW_MATCH_HISTORY, mouseX, mouseY)) {
            submitSettings(new PlayerPrivacySettings(settings.showRank(), settings.showStats(), !settings.showMatchHistory(), settings.joinLeaderboards()));
            return true;
        }
        if (isHovering(SETTING_JOIN_LEADERBOARDS, mouseX, mouseY)) {
            submitSettings(new PlayerPrivacySettings(settings.showRank(), settings.showStats(), settings.showMatchHistory(), !settings.joinLeaderboards()));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int amount = delta > 0.0D ? -1 : 1;
        if (this.currentPage == Page.OVERVIEW) {
            this.modeScroll = Mth.clamp(this.modeScroll + amount, 0, Math.max(0, filteredModes().size() - MODE_VISIBLE_ROWS));
            return true;
        }
        if (this.currentPage == Page.HISTORY) {
            this.historyScroll = Mth.clamp(this.historyScroll + amount, 0, Math.max(0, filteredHistoryRows().size() - HISTORY_VISIBLE_ROWS));
            return true;
        }
        if (this.currentPage == Page.RANKING) {
            this.rankingModeScroll = Mth.clamp(this.rankingModeScroll + amount, 0, Math.max(0, rankingModes().size() - RANKING_VISIBLE_ROWS));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void setPage(Page page) {
        if (this.currentPage == page) {
            return;
        }
        this.currentPage = page;
        this.searchText = "";
        if (this.searchBox != null) {
            this.searchBox.setValue("");
            this.searchBox.setFocused(false);
        }
        this.modeScroll = 0;
        this.historyScroll = 0;
        this.rankingModeScroll = 0;
        updateSearchBoxState();
    }

    private void submitSettings(PlayerPrivacySettings settings) {
        PvpStatsClient.updatePrivacySettings(settings);
    }

    private void cycleLoadoutTheme(int direction) {
        ClientConfig.LoadoutThemeMode[] modes = ClientConfig.LoadoutThemeMode.values();
        ClientConfig.LoadoutThemeMode current = ClientConfig.LOADOUT_THEME_MODE.get();
        ClientConfig.LOADOUT_THEME_MODE.set(modes[Math.floorMod(current.ordinal() + direction, modes.length)]);
    }

    private static String loadoutThemeLabel(ClientConfig.LoadoutThemeMode mode) {
        return text("setting.loadout_theme.value." + mode.name().toLowerCase(Locale.ROOT));
    }

    private void cycleDamageFeedbackMode(int direction) {
        ClientConfig.DamageFeedbackMode[] modes = ClientConfig.DamageFeedbackMode.values();
        ClientConfig.DamageFeedbackMode current = ClientConfig.DAMAGE_FEEDBACK_MODE.get();
        ClientConfig.DAMAGE_FEEDBACK_MODE.set(modes[Math.floorMod(current.ordinal() + direction, modes.length)]);
        ClientDamageFeedback.clear();
    }

    private static String damageFeedbackModeLabel(ClientConfig.DamageFeedbackMode mode) {
        return text("setting.damage_feedback_mode.value." + mode.name().toLowerCase(Locale.ROOT));
    }

    private static Rect choiceControl(Rect row) {
        return new Rect(row.x + 116, row.y + 2, row.w - 124, 14);
    }

    private static Rect choiceLeft(Rect control) {
        return new Rect(control.x, control.y, 14, control.h);
    }

    private void onSearchChanged(String value) {
        this.searchText = value == null ? "" : value;
        this.modeScroll = 0;
        this.historyScroll = 0;
        this.rankingModeScroll = 0;
    }

    private void updateSearchBoxState() {
        if (this.searchBox == null) {
            return;
        }
        boolean visible = this.currentPage != Page.SETTINGS;
        this.searchBox.visible = visible;
        this.searchBox.active = visible;
    }

    private List<Map.Entry<String, AggregateStats>> filteredModes() {
        String query = normalizedSearch();
        List<Map.Entry<String, AggregateStats>> entries = new ArrayList<>();
        for (Map.Entry<String, AggregateStats> entry : this.payload.modeStats().entrySet()) {
            if (query.isBlank() || matchesMode(entry.getKey(), query)) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private List<HistoryRow> filteredHistoryRows() {
        String query = normalizedSearch();
        List<HistoryRow> rows = new ArrayList<>();
        List<MatchRecord> records = this.payload.recentMatches();
        for (int i = 0; i < records.size(); i++) {
            MatchRecord record = records.get(i);
            if (!this.historyFilter.matches(record, this)) {
                continue;
            }
            if (!query.isBlank() && !matchesHistory(record, query)) {
                continue;
            }
            rows.add(new HistoryRow(record, i));
        }
        return rows;
    }

    private List<String> rankingModes() {
        Set<String> modes = new LinkedHashSet<>();
        modes.add(RankingEntry.OVERALL_MODE_ID);
        for (PvpModeDefinition definition : this.payload.modeDefinitions()) {
            if (definition.visible()) {
                modes.add(definition.modeId());
            }
        }
        for (RankingEntry entry : this.payload.rankingEntries()) {
            if (!entry.modeId().equals(RankingEntry.OVERALL_MODE_ID)) {
                modes.add(entry.modeId());
            }
        }
        modes.addAll(this.payload.modeStats().keySet());
        String query = normalizedSearch();
        if (query.isBlank()) {
            return new ArrayList<>(modes);
        }
        List<String> filtered = new ArrayList<>();
        for (String mode : modes) {
            if (RankingEntry.OVERALL_MODE_ID.equals(mode) || matchesMode(mode, query)) {
                filtered.add(mode);
            }
        }
        return filtered;
    }

    private List<RankingEntry> sortedRankingEntries() {
        String modeId = selectedRankingMode();
        String query = normalizedSearch();
        List<RankingEntry> entries = new ArrayList<>();
        for (RankingEntry entry : this.payload.rankingEntries()) {
            if (!entry.modeId().equals(modeId)) {
                continue;
            }
            if (!query.isBlank() && !entry.mcid().toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            entries.add(entry);
        }
        entries.sort(this.rankingSort.comparator);
        return entries;
    }

    private String selectedRankingMode() {
        if (this.selectedRankingModeId == null || this.selectedRankingModeId.isBlank()) {
            return RankingEntry.OVERALL_MODE_ID;
        }
        return this.selectedRankingModeId;
    }

    private AggregateStats selectedOverviewStats() {
        if (this.selectedModeId == null) {
            return this.payload.global();
        }
        AggregateStats stats = this.payload.modeStats().get(this.selectedModeId);
        return stats == null ? new AggregateStats() : stats;
    }

    private String currentPlayerRankingLine(List<RankingEntry> entries) {
        for (int i = 0; i < entries.size(); i++) {
            RankingEntry entry = entries.get(i);
            if (entry.mcid().equalsIgnoreCase(safeTargetName())) {
                return I18n.get(
                        key("ranking.your_rank"),
                        i + 1,
                        formatCount(entry.kills()),
                        formatWinRate(entry.wins(), entry.matches())
                );
            }
        }
        return text("ranking.your_rank_unlisted");
    }

    private boolean matchesMode(String modeId, String query) {
        return modeId.toLowerCase(Locale.ROOT).contains(query)
                || toModeDisplayName(modeId).toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean matchesHistory(MatchRecord record, String query) {
        return matchesMode(record.modeId(), query)
                || formatDate(record.timestamp()).toLowerCase(Locale.ROOT).contains(query)
                || toResultDisplayName(record.result()).toLowerCase(Locale.ROOT).contains(query)
                || record.result().toLowerCase(Locale.ROOT).contains(query);
    }

    private String normalizedSearch() {
        return this.searchText == null ? "" : this.searchText.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isHovering(Rect rect, double mouseX, double mouseY) {
        int x = this.leftPos + rect.x;
        int y = this.topPos + rect.y;
        return mouseX >= x && mouseX < x + rect.w && mouseY >= y && mouseY < y + rect.h;
    }

    private void blit(GuiGraphics gg, Tex tex, int x, int y) {
        blitScaled(gg, tex, x, y, tex.logicalW, tex.logicalH);
    }

    private void blitScaled(GuiGraphics gg, Tex tex, int x, int y, int drawW, int drawH) {
        gg.blit(tex.location, x, y, drawW, drawH, 0.0F, 0.0F, tex.textureW, tex.textureH, tex.textureW, tex.textureH);
    }

    private void drawTrimmed(GuiGraphics gg, String value, int x, int y, int maxWidth, int color) {
        gg.drawString(this.font, trimToWidth(value, maxWidth), x, y, color, false);
    }

    private void drawCenteredTrimmed(GuiGraphics gg, String value, int x, int y, int maxWidth, int color) {
        String trimmed = trimToWidth(value, maxWidth);
        gg.drawString(this.font, trimmed, x + (maxWidth - this.font.width(trimmed)) / 2, y, color, false);
    }

    private String trimToWidth(String text, int maxWidth) {
        Font font = this.font;
        String safeText = text == null ? "" : text;
        if (font.width(safeText) <= maxWidth) {
            return safeText;
        }
        int ellipsisWidth = font.width("...");
        if (maxWidth <= ellipsisWidth) {
            return "";
        }
        return font.plainSubstrByWidth(safeText, maxWidth - ellipsisWidth) + "...";
    }

    private String safeTargetName() {
        return this.payload.targetName().isBlank() ? "-" : this.payload.targetName();
    }

    private ResourceLocation playerSkin() {
        UUID targetId = this.payload.targetId();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null) {
            PlayerInfo playerInfo = minecraft.getConnection().getPlayerInfo(targetId);
            if (playerInfo != null) {
                return playerInfo.getSkinLocation();
            }
        }
        return DefaultPlayerSkin.getDefaultSkin(targetId);
    }

    private String formatDate(long timestamp) {
        return HISTORY_DATE_FORMAT.format(new Date(timestamp));
    }

    private String formatCount(long value) {
        if (value >= 1_000_000L) {
            return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0D);
        }
        if (value >= 10_000L) {
            return String.format(Locale.ROOT, "%.1fK", value / 1_000.0D);
        }
        return Long.toString(Math.max(0L, value));
    }

    private String formatKdr(AggregateStats stats) {
        return formatKdr(stats.kills(), stats.deaths());
    }

    private String formatKdr(long kills, long deaths) {
        if (deaths <= 0L) {
            return kills <= 0L ? "-" : text("stat.kdr.perfect");
        }
        return String.format(Locale.ROOT, "%.2f", (double) kills / (double) deaths);
    }

    private String formatWinRate(AggregateStats stats) {
        return formatWinRate(stats.wins(), stats.matches());
    }

    private String formatWinRate(long wins, long matches) {
        if (matches <= 0L) {
            return "-";
        }
        return String.format(Locale.ROOT, "%.1f%%", (double) wins * 100.0D / (double) matches);
    }

    private String toModeDisplayName(String modeId) {
        if (modeId == null || modeId.isBlank() || RankingEntry.OVERALL_MODE_ID.equals(modeId)) {
            return text("mode.overall");
        }

        PvpModeDefinition definition = modeDefinition(modeId);
        if (definition != null) {
            if (!definition.translationKey().isBlank() && I18n.exists(definition.translationKey())) {
                return I18n.get(definition.translationKey());
            }
            if (!definition.displayName().isBlank()) {
                return definition.displayName();
            }
        }

        String normalized = normalizeModeKey(modeId);
        String pvpModeKey = "pvp_mode." + DpvpTweaks.MODID + "." + normalized;
        if (I18n.exists(pvpModeKey)) {
            return I18n.get(pvpModeKey);
        }

        String oldKey = "gui." + DpvpTweaks.MODID + ".pvp_stats.mode." + modeId.replace('/', '.');
        if (I18n.exists(oldKey)) {
            return I18n.get(oldKey);
        }

        String oldNormalizedKey = "gui." + DpvpTweaks.MODID + ".pvp_stats.mode." + normalized;
        if (I18n.exists(oldNormalizedKey)) {
            return I18n.get(oldNormalizedKey);
        }
        return modeId;
    }

    private String toResultDisplayName(String result) {
        ResultKind kind = ResultKind.from(result);
        return text(kind.key);
    }

    private Tex modeIcon(String modeId) {
        PvpModeDefinition definition = modeDefinition(modeId);
        if (definition != null && definition.icon() != null) {
            ResourceLocation configured = definition.icon();
            ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(
                    configured.getNamespace(),
                    "textures/" + configured.getPath() + ".png"
            );
            if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) {
                return new Tex(texture, 8, 8, 32, 32);
            }
        }
        return switch (normalizeModeKey(modeId)) {
            case "ctf", "capture_the_flag" -> ICON_MODE_CTF;
            case "ffa", "free_for_all" -> ICON_MODE_FFA;
            case "dom", "domination", "hardpoint" -> ICON_MODE_DOMINATION;
            case "gun_game" -> ICON_MODE_GUN_GAME;
            case "sniper", "sniper_only" -> ICON_MODE_SNIPER;
            case "pistol", "pistol_only" -> ICON_MODE_PISTOL;
            case "shotgun", "shotgun_only" -> ICON_MODE_SHOTGUN;
            case "ranked" -> ICON_MODE_RANKED;
            case "casual" -> ICON_MODE_CASUAL;
            case "event", "winter_event" -> ICON_MODE_EVENT;
            case "overall" -> ICON_MODE_OVERALL;
            case "tdm", "team_deathmatch" -> ICON_MODE_TDM;
            default -> ICON_MODE_FALLBACK;
        };
    }

    private PvpModeDefinition modeDefinition(String modeId) {
        if (modeId == null) {
            return null;
        }
        for (PvpModeDefinition definition : this.payload.modeDefinitions()) {
            if (definition.modeId().equalsIgnoreCase(modeId)) {
                return definition;
            }
        }
        return null;
    }

    private Component modeDescription(String modeId) {
        PvpModeDefinition definition = modeDefinition(modeId);
        if (definition == null) {
            return null;
        }
        if (!definition.descriptionTranslationKey().isBlank()
                && I18n.exists(definition.descriptionTranslationKey())) {
            return Component.translatable(definition.descriptionTranslationKey());
        }
        if (!definition.description().isBlank()) {
            return Component.literal(definition.description());
        }
        return null;
    }

    private boolean modeHasTag(String modeId, String tag) {
        PvpModeDefinition definition = modeDefinition(modeId);
        return definition != null && definition.hasTag(tag);
    }

    private String normalizeModeKey(String modeId) {
        if (modeId == null || modeId.isBlank()) {
            return RankingEntry.OVERALL_MODE_ID;
        }
        return modeId.trim()
                .toLowerCase(Locale.ROOT)
                .replace(':', '_')
                .replace('/', '_')
                .replace('-', '_');
    }

    private static String key(String suffix) {
        return KEY_PREFIX + suffix;
    }

    private static String text(String suffix) {
        if (suffix.startsWith("gui.") || suffix.startsWith("pvp_mode.") || suffix.startsWith("key.")) {
            return I18n.get(suffix);
        }
        return I18n.get(key(suffix));
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(DpvpTweaks.MODID, "textures/gui/combat_record/" + path + ".png");
    }

    private static Tex tex(String path, int logicalW, int logicalH) {
        return new Tex(texture(path), logicalW, logicalH, logicalW * 4, logicalH * 4);
    }

    private enum Page {
        OVERVIEW(TAB_OVERVIEW, key("page.overview")),
        HISTORY(TAB_HISTORY, key("page.history")),
        RANKING(TAB_RANKING, key("page.ranking")),
        SETTINGS(TAB_SETTINGS, key("page.settings"));

        private final Rect rect;
        private final String key;

        Page(Rect rect, String key) {
            this.rect = rect;
            this.key = key;
        }
    }

    private enum SettingsCategory {
        PRIVACY,
        DISPLAY
    }

    private enum HistoryFilter {
        ALL(key("history.filter.all")) {
            @Override
            boolean matches(MatchRecord record, PvpStatsScreen screen) {
                return true;
            }
        },
        WINS(key("history.filter.wins")) {
            @Override
            boolean matches(MatchRecord record, PvpStatsScreen screen) {
                return ResultKind.from(record.result()) == ResultKind.WIN;
            }
        },
        LOSSES(key("history.filter.losses")) {
            @Override
            boolean matches(MatchRecord record, PvpStatsScreen screen) {
                return ResultKind.from(record.result()) == ResultKind.LOSS;
            }
        },
        RANKED(key("history.filter.ranked")) {
            @Override
            boolean matches(MatchRecord record, PvpStatsScreen screen) {
                return screen.modeHasTag(record.modeId(), "ranked")
                        || record.modeId().toLowerCase(Locale.ROOT).contains("ranked");
            }
        },
        EVENT(key("history.filter.event")) {
            @Override
            boolean matches(MatchRecord record, PvpStatsScreen screen) {
                return screen.modeHasTag(record.modeId(), "event")
                        || record.modeId().toLowerCase(Locale.ROOT).contains("event");
            }
        };

        private final String key;

        HistoryFilter(String key) {
            this.key = key;
        }

        abstract boolean matches(MatchRecord record, PvpStatsScreen screen);
    }

    private enum RankingSort {
        KILLS(key("ranking.sort.kills"), Comparator
                .comparingLong(RankingEntry::kills).reversed()
                .thenComparing(Comparator.comparingDouble((RankingEntry entry) -> rankingWinRate(entry)).reversed())
                .thenComparing(RankingEntry::mcid, String.CASE_INSENSITIVE_ORDER)),
        WIN_RATE(key("ranking.sort.win_rate"), Comparator
                .comparingDouble((RankingEntry entry) -> rankingWinRate(entry)).reversed()
                .thenComparing(Comparator.comparingLong(RankingEntry::wins).reversed())
                .thenComparing(RankingEntry::mcid, String.CASE_INSENSITIVE_ORDER));

        private final String key;
        private final Comparator<RankingEntry> comparator;

        RankingSort(String key, Comparator<RankingEntry> comparator) {
            this.key = key;
            this.comparator = comparator;
        }

        private static double rankingWinRate(RankingEntry entry) {
            if (entry.matches() <= 0L) {
                return 0.0D;
            }
            return (double) entry.wins() / (double) entry.matches();
        }
    }

    private enum RankSubPage {
        RANKING("rank.tab.ranking"),
        BADGES("rank.tab.badges");

        private final String key;

        RankSubPage(String key) {
            this.key = key;
        }
    }

    private enum ResultKind {
        WIN("result.win", "result.win.short", RESULT_BADGE_WIN, 0xEAFEDF),
        LOSS("result.loss", "result.loss.short", RESULT_BADGE_LOSS, 0xFFE1E1),
        DRAW("result.draw", "result.draw.short", RESULT_BADGE_DRAW, 0xFFF6D1),
        UNKNOWN("result.unknown", "result.unknown.short", RESULT_BADGE_DRAW, 0xE1E8EB);

        private final String key;
        private final String shortKey;
        private final Tex badge;
        private final int textColor;

        ResultKind(String key, String shortKey, Tex badge, int textColor) {
            this.key = key;
            this.shortKey = shortKey;
            this.badge = badge;
            this.textColor = textColor;
        }

        private static ResultKind from(String result) {
            String normalized = result == null ? "" : result.trim().toUpperCase(Locale.ROOT);
            return switch (normalized) {
                case "WIN", "WINS" -> WIN;
                case "LOSS", "LOSE", "LOSSES" -> LOSS;
                case "DRAW", "DRAWS" -> DRAW;
                default -> UNKNOWN;
            };
        }
    }

    private record HistoryRow(MatchRecord record, int sourceIndex) {
    }

    private record Tex(ResourceLocation location, int logicalW, int logicalH, int textureW, int textureH) {
    }

    private record Rect(int x, int y, int w, int h) {
    }
}
