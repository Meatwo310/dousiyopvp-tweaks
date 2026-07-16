package com.dousiyo.dpvptweaks.client.content;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.content.ContentEntry;
import com.dousiyo.dpvptweaks.content.ContentType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class ContentScreen extends Screen {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(DpvpTweaks.MODID,
            "textures/gui/combat_record/pages/content_bg.png");
    private static final int GUI_W = 320;
    private static final int GUI_H = 210;
    private static final int VISIBLE_ENTRIES = 6;
    private static final int VISIBLE_LINES = 11;
    private static final int ENTRY_Y = 47;
    private static final int ENTRY_STEP = 22;
    private static final int ENTRY_HEIGHT = 20;
    private static final SimpleDateFormat DATE = new SimpleDateFormat("yyyy/MM/dd", Locale.ROOT);

    private final Screen parent;
    private ContentType type;
    private List<ContentEntry> entries = List.of();
    private String selectedKey = "";
    private String detailTitle = "";
    private List<FormattedCharSequence> detailLines = List.of();
    private int entryScroll;
    private int bodyScroll;
    private int leftPos;
    private int topPos;
    private boolean loading = true;

    public ContentScreen(Screen parent, ContentType type) {
        super(Component.literal("インフォメーション"));
        this.parent = parent;
        this.type = type;
    }

    @Override protected void init() {
        leftPos = (width - GUI_W) / 2;
        topPos = (height - GUI_H) / 2;
    }

    @Override public boolean isPauseScreen() { return false; }

    public void receiveList(ContentType responseType, List<ContentEntry> responseEntries) {
        if (responseType != type) return;
        entries = responseEntries;
        loading = false;
        entryScroll = Mth.clamp(entryScroll, 0, Math.max(0, entries.size() - VISIBLE_ENTRIES));
    }

    public void receiveDetail(ContentType responseType, String key, String title, String markdown) {
        if (responseType != type || !key.equals(selectedKey)) return;
        detailTitle = title;
        detailLines = parseMarkdown(markdown);
        bodyScroll = 0;
        ContentClient.requestList(type);
    }

    @Override public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        renderBackground(gg);
        gg.blit(BACKGROUND, leftPos, topPos, GUI_W, GUI_H, 0, 0, 1280, 840, 1280, 840);
        gg.drawString(font, "インフォメーション", leftPos + 27, topPos + 8, 0xE8D9A4, false);
        drawCentered(gg, "お知らせ", 88, 27, 70, type == ContentType.ANNOUNCEMENT ? 0xFFD15A : 0xA59B73);
        drawCentered(gg, "ルール", 160, 27, 70, type == ContentType.RULE ? 0xFFD15A : 0xA59B73);

        if (loading) gg.drawCenteredString(font, "読み込み中...", leftPos + 58, topPos + 108, 0xA59B73);
        else if (entries.isEmpty()) gg.drawCenteredString(font,
                type == ContentType.ANNOUNCEMENT ? "お知らせはありません" : "ルールはありません",
                leftPos + 58, topPos + 108, 0x8E866A);
        else renderEntries(gg, mouseX, mouseY);

        if (selectedKey.isBlank()) {
            gg.drawCenteredString(font, "左の一覧から選択してください", leftPos + 218, topPos + 110, 0x8E866A);
        } else if (detailLines.isEmpty()) {
            gg.drawCenteredString(font, "本文を読み込み中...", leftPos + 218, topPos + 110, 0xA59B73);
        } else renderDetail(gg);

        int unread = (int) entries.stream().filter(ContentEntry::unread).count();
        gg.drawString(font, type == ContentType.ANNOUNCEMENT ? "未読 " + unread + "件 / 全" + entries.size() + "件" : "公開ルール " + entries.size() + "件",
                leftPos + 8, topPos + 191, 0xB7AC7F, false);
        super.render(gg, mouseX, mouseY, partialTick);
    }

    private void renderEntries(GuiGraphics gg, int mouseX, int mouseY) {
        int visible = Math.min(VISIBLE_ENTRIES, entries.size() - entryScroll);
        for (int i = 0; i < visible; i++) {
            ContentEntry entry = entries.get(entryScroll + i);
            int y = ENTRY_Y + i * ENTRY_STEP;
            boolean selected = entry.key().equals(selectedKey);
            boolean hover = inside(mouseX, mouseY, 5, y, 107, ENTRY_HEIGHT);
            if (selected || hover) gg.fill(leftPos + 6, topPos + y + 1, leftPos + 111, topPos + y + ENTRY_HEIGHT - 1,
                    selected ? 0x553C3212 : 0x332B281C);
            int color = entry.unread() ? 0xFFD15A : 0xD5CEB2;
            String prefix = entry.unread() ? "● " : "○ ";
            drawTrimmed(gg, prefix + entry.title(), leftPos + 10, topPos + y + 1, 96, color);
            gg.drawString(font, DATE.format(new Date(entry.updatedAt())), leftPos + 74, topPos + y + 10, 0x77705A, false);
            if ("CRITICAL".equals(entry.badge())) gg.drawString(font, "!", leftPos + 100, topPos + y + 10, 0xE25445, false);
        }
    }

    private void renderDetail(GuiGraphics gg) {
        drawTrimmed(gg, detailTitle, leftPos + 122, topPos + 50, 176, 0xF2E8C6);
        int max = Math.min(detailLines.size(), bodyScroll + VISIBLE_LINES);
        int y = topPos + 66;
        for (int i = bodyScroll; i < max; i++) {
            gg.drawString(font, detailLines.get(i), leftPos + 122, y, 0xD7D0B8, false);
            y += 10;
        }
        if (detailLines.size() > VISIBLE_LINES) {
            int trackY = topPos + 65;
            int thumbY = trackY + (int) ((112 - 12) * (bodyScroll / (double) Math.max(1, detailLines.size() - VISIBLE_LINES)));
            gg.fill(leftPos + 304, thumbY, leftPos + 306, thumbY + 12, 0xFFB53B);
        }
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        if (inside(mouseX, mouseY, 296, 3, 20, 20)) { onClose(); return true; }
        if (inside(mouseX, mouseY, 88, 27, 70, 16)) { switchType(ContentType.ANNOUNCEMENT); return true; }
        if (inside(mouseX, mouseY, 160, 27, 70, 16)) { switchType(ContentType.RULE); return true; }
        for (int i = 0; i < Math.min(VISIBLE_ENTRIES, entries.size() - entryScroll); i++) {
            if (inside(mouseX, mouseY, 5, ENTRY_Y + i * ENTRY_STEP, 107, ENTRY_HEIGHT)) {
                ContentEntry entry = entries.get(entryScroll + i);
                selectedKey = entry.key(); detailTitle = ""; detailLines = List.of();
                ContentClient.requestDetail(type, selectedKey);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int amount = delta > 0 ? -1 : 1;
        if (inside(mouseX, mouseY, 116, 63, 192, 120)) {
            bodyScroll = Mth.clamp(bodyScroll + amount, 0, Math.max(0, detailLines.size() - VISIBLE_LINES));
        } else entryScroll = Mth.clamp(entryScroll + amount, 0, Math.max(0, entries.size() - VISIBLE_ENTRIES));
        return true;
    }

    @Override public void onClose() { minecraft.setScreen(parent); }

    private void switchType(ContentType newType) {
        if (type == newType) return;
        type = newType; entries = List.of(); selectedKey = ""; detailLines = List.of(); entryScroll = 0; bodyScroll = 0; loading = true;
        ContentClient.requestList(type);
    }

    private List<FormattedCharSequence> parseMarkdown(String markdown) {
        List<FormattedCharSequence> result = new ArrayList<>();
        boolean codeBlock = false;
        for (String raw : markdown.replace("\r", "").split("\n", -1)) {
            String line = raw;
            if (line.stripLeading().startsWith("```")) { codeBlock = !codeBlock; continue; }
            MutableComponent component;
            if (codeBlock) component = Component.literal(line).withStyle(ChatFormatting.GRAY);
            else if (line.matches("^\\s*([-*_])\\1{2,}\\s*$")) component = Component.literal("────────────────────────").withStyle(ChatFormatting.DARK_GRAY);
            else if (line.startsWith("### ")) component = inline(line.substring(4)).withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD);
            else if (line.startsWith("## ")) component = inline(line.substring(3)).withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW);
            else if (line.startsWith("# ")) component = inline(line.substring(2)).withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD);
            else if (line.matches("^\\s*[-*+] .+")) component = Component.literal("• ").withStyle(ChatFormatting.GOLD).append(inline(line.replaceFirst("^\\s*[-*+] ", "")));
            else if (line.matches("^\\s*\\d+\\. .+")) component = inline(line.strip());
            else if (line.startsWith("> ")) component = Component.literal("│ ").withStyle(ChatFormatting.DARK_GRAY).append(inline(line.substring(2)).withStyle(ChatFormatting.ITALIC));
            else component = inline(line);
            if (line.isEmpty()) result.add(FormattedCharSequence.EMPTY);
            else result.addAll(font.split(component, 176));
        }
        return result;
    }

    private MutableComponent inline(String text) {
        MutableComponent out = Component.empty();
        int i = 0;
        while (i < text.length()) {
            int bold = text.indexOf("**", i), code = text.indexOf('`', i), italic = text.indexOf('*', i);
            int next = minPositive(bold, code, italic);
            if (next < 0) { out.append(Component.literal(text.substring(i))); break; }
            if (next > i) out.append(Component.literal(text.substring(i, next)));
            if (next == bold) {
                int end = text.indexOf("**", next + 2);
                if (end > next) { out.append(Component.literal(text.substring(next + 2, end)).withStyle(ChatFormatting.BOLD)); i = end + 2; continue; }
            } else if (next == code) {
                int end = text.indexOf('`', next + 1);
                if (end > next) { out.append(Component.literal(text.substring(next + 1, end)).withStyle(ChatFormatting.GOLD)); i = end + 1; continue; }
            } else {
                int end = text.indexOf('*', next + 1);
                if (end > next) { out.append(Component.literal(text.substring(next + 1, end)).withStyle(ChatFormatting.ITALIC)); i = end + 1; continue; }
            }
            out.append(Component.literal(text.substring(next, next + 1))); i = next + 1;
        }
        return out;
    }

    private static int minPositive(int... values) { int min = Integer.MAX_VALUE; for (int v : values) if (v >= 0 && v < min) min = v; return min == Integer.MAX_VALUE ? -1 : min; }
    private boolean inside(double mx, double my, int x, int y, int w, int h) { return mx >= leftPos + x && mx < leftPos + x + w && my >= topPos + y && my < topPos + y + h; }
    private void drawCentered(GuiGraphics gg, String text, int x, int y, int w, int color) { gg.drawString(font, text, leftPos + x + (w - font.width(text)) / 2, topPos + y + 5, color, false); }
    private void drawTrimmed(GuiGraphics gg, String text, int x, int y, int width, int color) {
        String shown = font.width(text) <= width ? text : font.plainSubstrByWidth(text, Math.max(0, width - font.width("..."))) + "...";
        gg.drawString(font, shown, x, y, color, false);
    }
}
