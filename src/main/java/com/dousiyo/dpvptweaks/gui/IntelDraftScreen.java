package com.dousiyo.dpvptweaks.gui;

import com.dousiyo.dpvptweaks.inteldraft.IntelDraftDefinition;
import com.dousiyo.dpvptweaks.network.LoadoutGuiNetwork;
import com.dousiyo.dpvptweaks.network.RerollIntelDraftPacket;
import com.dousiyo.dpvptweaks.network.SelectIntelDraftPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class IntelDraftScreen extends Screen {
    private static final int CARD_COUNT = 3;
    private static final int MAX_CARD_W = 164;
    private static final int MIN_CARD_W = 108;
    private static final int CARD_H = 252;
    private static final int MAX_CARD_GAP = 22;
    private static final int REROLL_W = 112;
    private static final int REROLL_H = 38;
    private static final long SPIN_BASE_DURATION_MS = 1_350L;
    private static final long SPIN_STAGGER_MS = 260L;
    private static final int SPIN_BASE_STEPS = 12;

    private static final int COLOR_BG_TOP = 0xE8202020;
    private static final int COLOR_BG_BOTTOM = 0xEE111111;
    private static final int COLOR_CARD = 0xEE202020;
    private static final int COLOR_CARD_HOVER = 0xF02B2B2B;
    private static final int COLOR_CARD_BORDER = 0xFF6A5A2A;
    private static final int COLOR_CARD_BORDER_HOVER = 0xFFFFD36A;
    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_TECH = 0xFFFFFFFF;
    private static final int COLOR_DESC = 0xFFE5C35A;
    private static final int COLOR_AND = 0xFFD8D8D8;
    private static final int COLOR_DISABLED = 0xFF777777;

    private final IntelDraftDefinition definition;
    private final long spinStartedAtMillis = System.currentTimeMillis();
    private final int[] previousSpinSteps = {-1, -1, -1};
    private final boolean[] previousCardSpinning = {true, true, true};
    private int remainingRerolls;
    private int cardW = MAX_CARD_W;
    private int cardGap = MAX_CARD_GAP;
    private int cardStartX;
    private int cardY;
    private int rerollX;
    private int rerollY;
    private boolean awaitingServer;

    public IntelDraftScreen(IntelDraftDefinition definition) {
        super(Component.literal("Intel Draft"));
        this.definition = Objects.requireNonNull(definition);
        this.remainingRerolls = definition.remainingRerolls();
    }

    @Override
    protected void init() {
        super.init();
        int availableWidth = Math.max(MIN_CARD_W * CARD_COUNT + 24, this.width - 48);
        this.cardGap = Math.min(MAX_CARD_GAP, Math.max(8, availableWidth / 28));
        this.cardW = Math.min(MAX_CARD_W, Math.max(MIN_CARD_W, (availableWidth - (CARD_COUNT - 1) * cardGap) / CARD_COUNT));
        int totalCardsWidth = CARD_COUNT * cardW + (CARD_COUNT - 1) * cardGap;
        this.cardStartX = (this.width - totalCardsWidth) / 2;
        this.cardY = Math.max(68, (this.height - CARD_H) / 2);
        this.rerollX = this.width - REROLL_W - 18;
        // Keep reroll entirely above the selectable cards, including its shadow.
        this.rerollY = 14;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return definition.closeAllowed();
    }

    @Override
    public void tick() {
        super.tick();
        if (definition.expiresAtMillis() > 0L && System.currentTimeMillis() >= definition.expiresAtMillis()
                && this.minecraft != null) this.minecraft.setScreen(null);

        updateSpinSounds();
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        renderBackdrop(gg);
        boolean revealComplete = isRevealComplete();
        renderLargeGothicTitle(gg, revealComplete ? "技術選択" : "候補抽選中...");
        if (definition.expiresAtMillis() > 0L) {
            long seconds = Math.max(0L, (definition.expiresAtMillis() - System.currentTimeMillis() + 999L) / 1000L);
            gg.drawCenteredString(this.font, "残り " + seconds + "秒", this.width / 2, 44,
                    seconds <= 5 ? 0xFFFF7777 : COLOR_DESC);
        } else {
            gg.drawCenteredString(this.font, definition.closeAllowed() ? "ESCで保留 / Iで再開" : "選択して出撃", this.width / 2, 44, COLOR_DESC);
        }

        IntelDraftDefinition.ChoiceDefinition tooltipChoice = null;
        int tooltipX = 0;
        for (int i = 0; i < CARD_COUNT; i++) {
            int x = cardStartX + i * (cardW + cardGap);
            IntelDraftDefinition.ChoiceDefinition choice = displayedChoice(i);
            boolean cardSpinning = isCardSpinning(i);
            boolean hovered = revealComplete && isInside(mouseX, mouseY, x, cardY, cardW, CARD_H);
            renderCard(gg, x, cardY, choice, hovered);
            if (cardSpinning) renderSpinFrame(gg, x, cardY, i);
            if (choice != null && hovered) {
                tooltipChoice = choice; tooltipX = x;
            }
        }

        renderRerollButton(gg, mouseX, mouseY);
        if (!definition.acquiredTechNames().isEmpty()) {
            String acquired = "取得済み: " + String.join(", ", definition.acquiredTechNames());
            drawCenteredTrimmed(gg, acquired, 12, this.height - 12, this.width - 24, 0xFFBBBBBB);
        }
        super.render(gg, mouseX, mouseY, partialTick);
        if (tooltipChoice != null) renderItemTooltips(gg, tooltipChoice, tooltipX, mouseX, mouseY);
    }

    private void renderBackdrop(GuiGraphics gg) {
        gg.fillGradient(0, 0, this.width, this.height, COLOR_BG_TOP, COLOR_BG_BOTTOM);
        int bandY = this.height / 2 - 40;
        gg.fill(0, bandY, this.width, bandY + 92, 0x30181818);
        gg.hLine(cardStartX - 40, cardStartX + CARD_COUNT * cardW + (CARD_COUNT - 1) * cardGap + 40, cardY + 58, 0xAA705A22);
        gg.hLine(cardStartX - 40, cardStartX + CARD_COUNT * cardW + (CARD_COUNT - 1) * cardGap + 40, cardY + CARD_H - 54, 0xAA705A22);
    }

    /** Enlarged standard Japanese glyphs with a backdrop for clean, Gothic-style readability. */
    private void renderLargeGothicTitle(GuiGraphics gg, String title) {
        float scale = "技術選択".equals(title) ? 2.2F : 1.75F;
        float scaledWidth = this.width / scale;
        float x = (scaledWidth - this.font.width(title)) / 2.0F;
        float y = 7.0F;
        int visualWidth = Math.round(this.font.width(title) * scale);
        int visualX = (this.width - visualWidth) / 2;
        gg.fill(visualX - 8, 10, visualX + visualWidth + 8, 38, 0x66000000);
        gg.pose().pushPose();
        gg.pose().scale(scale, scale, 1.0F);
        gg.drawString(this.font, title, (int) x, (int) y, COLOR_TITLE, false);
        gg.pose().popPose();
    }

    private void renderCard(GuiGraphics gg, int x, int y, IntelDraftDefinition.ChoiceDefinition choice, boolean hovered) {
        int border = choice == null ? 0xFF444444 : (hovered ? COLOR_CARD_BORDER_HOVER : COLOR_CARD_BORDER);
        int fill = choice == null ? 0xCC181818 : (hovered ? COLOR_CARD_HOVER : COLOR_CARD);
        gg.fill(x, y, x + cardW, y + CARD_H, fill);
        gg.fill(x, y, x + cardW, y + 2, border);
        gg.fill(x, y + CARD_H - 2, x + cardW, y + CARD_H, border);
        gg.fill(x, y, x + 2, y + CARD_H, border);
        gg.fill(x + cardW - 2, y, x + cardW, y + CARD_H, border);

        if (choice == null) {
            gg.drawCenteredString(this.font, "候補なし", x + cardW / 2, y + CARD_H / 2 - 4, COLOR_DISABLED);
            return;
        }

        ItemStack techIcon = choice.tech().iconStack();
        if (!techIcon.isEmpty()) {
            renderScaledItem(gg, techIcon, x + cardW / 2 - 16, y + 20, 2.0F);
        }

        drawCenteredTrimmed(gg, choice.tech().name(), x + 12, y + 58, cardW - 24, COLOR_TECH);
        drawCenteredWrapped(gg, choice.tech().description(), x + 12, y + 76, cardW - 24, 3, COLOR_DESC);

        int dividerY = y + 116;
        gg.hLine(x + 34, x + cardW / 2 - 16, dividerY + 4, 0xAA8A753E);
        gg.drawCenteredString(this.font, "と", x + cardW / 2, dividerY, COLOR_AND);
        gg.hLine(x + cardW / 2 + 16, x + cardW - 34, dividerY + 4, 0xAA8A753E);

        ItemStack gunIcon = choice.gun().gunStack();
        if (!gunIcon.isEmpty()) {
            renderScaledItem(gg, gunIcon, x + cardW / 2 - 18, y + 138, 2.25F);
        }

        drawCenteredTrimmed(gg, choice.gun().name(), x + 10, y + 174, cardW - 20, COLOR_TECH);

        gg.drawCenteredString(this.font, "+", x + cardW / 2, y + 190, COLOR_AND);
        ItemStack attachmentIcon = choice.attachment().attachmentStack();
        if (!attachmentIcon.isEmpty()) renderScaledItem(gg, attachmentIcon, x + cardW / 2 - 12, y + 202, 1.5F);
        drawCenteredTrimmed(gg, choice.attachment().name(), x + 10, y + 230, cardW - 20, COLOR_TECH);
    }

    private void renderSpinFrame(GuiGraphics gg, int x, int y, int cardIndex) {
        long elapsed = Math.max(0L, System.currentTimeMillis() - spinStartedAtMillis);
        int pulse = (int) ((elapsed / 90L + cardIndex * 2L) % 6L);
        int glow = 0x50 + pulse * 0x10;
        int color = (glow << 24) | 0x00FFD36A;
        int scanY = y + 5 + (int) ((elapsed / 7L + cardIndex * 71L) % (CARD_H - 10));

        gg.fill(x - 1, y - 1, x + cardW + 1, y + 2, color);
        gg.fill(x - 1, y + CARD_H - 2, x + cardW + 1, y + CARD_H + 1, color);
        gg.fill(x, scanY, x + cardW, scanY + 2, 0x55FFD36A);
        gg.fill(x + 3, scanY + 2, x + cardW - 3, scanY + 3, 0x18FFFFFF);
    }

    private void renderScaledItem(GuiGraphics gg, ItemStack stack, int x, int y, float scale) {
        gg.pose().pushPose();
        gg.pose().translate(x, y, 0.0F);
        gg.pose().scale(scale, scale, 1.0F);
        gg.renderItem(stack, 0, 0);
        gg.pose().popPose();
    }

    private void renderRerollButton(GuiGraphics gg, int mouseX, int mouseY) {
        boolean enabled = isRevealComplete() && !awaitingServer && remainingRerolls > 0 && !definition.choices().isEmpty();
        boolean hovered = enabled && isInside(mouseX, mouseY, rerollX, rerollY, REROLL_W, REROLL_H);
        int top = enabled ? (hovered ? 0xFFE8C75E : 0xFFD4A83A) : 0xFF5C5545;
        int bottom = enabled ? (hovered ? 0xFFC99428 : 0xFF8C6A20) : 0xFF35322D;
        int border = enabled ? (hovered ? 0xFFFFE08A : 0xFFE6C35A) : 0xFF777067;
        int shadow = 0x99000000;
        int text = enabled ? 0xFF17130A : 0xFFB4AEA4;
        int subText = enabled ? 0xFF332814 : 0xFF8F887F;

        gg.fill(rerollX + 3, rerollY + 4, rerollX + REROLL_W + 3, rerollY + REROLL_H + 4, shadow);
        gg.fillGradient(rerollX, rerollY, rerollX + REROLL_W, rerollY + REROLL_H, top, bottom);
        gg.fill(rerollX, rerollY, rerollX + REROLL_W, rerollY + 2, border);
        gg.fill(rerollX, rerollY + REROLL_H - 2, rerollX + REROLL_W, rerollY + REROLL_H, 0xAA2C210E);
        gg.fill(rerollX, rerollY, rerollX + 2, rerollY + REROLL_H, border);
        gg.fill(rerollX + REROLL_W - 2, rerollY, rerollX + REROLL_W, rerollY + REROLL_H, 0xAA2C210E);
        gg.hLine(rerollX + 8, rerollX + REROLL_W - 8, rerollY + 6, 0x55FFFFFF);

        gg.drawCenteredString(this.font, "再ロール！", rerollX + REROLL_W / 2, rerollY + 8, text);
        gg.drawCenteredString(this.font, "残り " + remainingRerolls, rerollX + REROLL_W / 2, rerollY + 22, subText);
    }

    private void drawCenteredTrimmed(GuiGraphics gg, String text, int x, int y, int width, int color) {
        String trimmed = this.font.plainSubstrByWidth(text == null ? "" : text, width);
        gg.drawString(this.font, trimmed, x + (width - this.font.width(trimmed)) / 2, y, color, false);
    }

    private void drawCenteredWrapped(GuiGraphics gg, String text, int x, int y, int width, int maxLines, int color) {
        List<String> lines = wrap(text == null ? "" : text, width, maxLines);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            gg.drawString(this.font, line, x + (width - this.font.width(line)) / 2, y + i * 11, color, false);
        }
    }

    private List<String> wrap(String text, int width, int maxLines) {
        if (text.isBlank() || maxLines <= 0) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        String remaining = text;
        while (!remaining.isBlank() && lines.size() < maxLines) {
            String line = this.font.plainSubstrByWidth(remaining, width);
            if (line.isEmpty()) {
                break;
            }
            lines.add(line);
            remaining = remaining.substring(line.length()).stripLeading();
        }
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (!isRevealComplete()) return true;

        if (!awaitingServer && remainingRerolls > 0 && !definition.choices().isEmpty() && isInside(mouseX, mouseY, rerollX, rerollY, REROLL_W, REROLL_H)) {
            awaitingServer = true;
            LoadoutGuiNetwork.CHANNEL.sendToServer(new RerollIntelDraftPacket(definition.sessionId()));
            playClick();
            return true;
        }

        if (awaitingServer) return true;
        for (int i = 0; i < definition.choices().size(); i++) {
            int x = cardStartX + i * (cardW + cardGap);
            if (isInside(mouseX, mouseY, x, cardY, cardW, CARD_H)) {
                confirm(i);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void confirm(int choiceIndex) {
        awaitingServer = true;
        playClick();
        LoadoutGuiNetwork.CHANNEL.sendToServer(new SelectIntelDraftPacket(definition.sessionId(), choiceIndex));
    }

    private void renderItemTooltips(GuiGraphics gg, IntelDraftDefinition.ChoiceDefinition choice,
                                    int x, int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY, x + cardW / 2 - 22, cardY + 134, 44, 44))
            gg.renderTooltip(this.font, choice.gun().gunStack(), mouseX, mouseY);
        else if (isInside(mouseX, mouseY, x + cardW / 2 - 16, cardY + 198, 32, 32))
            gg.renderTooltip(this.font, choice.attachment().attachmentStack(), mouseX, mouseY);
        else if (isInside(mouseX, mouseY, x + cardW / 2 - 18, cardY + 18, 36, 36))
            gg.renderTooltip(this.font, choice.tech().iconStack(), mouseX, mouseY);
    }

    private void playClick() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
        }
    }

    private IntelDraftDefinition.ChoiceDefinition displayedChoice(int cardIndex) {
        int choiceCount = definition.choices().size();
        if (cardIndex >= choiceCount) return null;
        if (!isCardSpinning(cardIndex)) return definition.choices().get(cardIndex);

        int steps = spinStep(cardIndex);
        int totalSteps = SPIN_BASE_STEPS + cardIndex * 2;
        int startIndex = Math.floorMod(cardIndex - totalSteps, choiceCount);
        return definition.choices().get(Math.floorMod(startIndex + steps, choiceCount));
    }

    private boolean isRevealComplete() {
        if (definition.choices().isEmpty()) return true;
        int lastCard = Math.min(CARD_COUNT, definition.choices().size()) - 1;
        return !isCardSpinning(lastCard);
    }

    private boolean isCardSpinning(int cardIndex) {
        if (cardIndex >= definition.choices().size()) return false;
        return System.currentTimeMillis() - spinStartedAtMillis < spinDuration(cardIndex);
    }

    private int spinStep(int cardIndex) {
        long elapsed = Math.max(0L, System.currentTimeMillis() - spinStartedAtMillis);
        long duration = spinDuration(cardIndex);
        double progress = Math.min(1.0D, (double) elapsed / duration);
        double eased = 1.0D - Math.pow(1.0D - progress, 2.35D);
        int totalSteps = SPIN_BASE_STEPS + cardIndex * 2;
        return Math.min(totalSteps - 1, (int) (eased * totalSteps));
    }

    private long spinDuration(int cardIndex) {
        return SPIN_BASE_DURATION_MS + cardIndex * SPIN_STAGGER_MS;
    }

    private void updateSpinSounds() {
        if (this.minecraft == null || this.minecraft.player == null) return;

        boolean advanced = false;
        for (int i = 0; i < Math.min(CARD_COUNT, definition.choices().size()); i++) {
            boolean spinning = isCardSpinning(i);
            int step = spinning ? spinStep(i) : SPIN_BASE_STEPS + i * 2;
            if (spinning && previousSpinSteps[i] >= 0 && step != previousSpinSteps[i]) advanced = true;
            if (!spinning && previousCardSpinning[i]) {
                this.minecraft.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.55F, 1.15F + i * 0.12F);
            }
            previousSpinSteps[i] = step;
            previousCardSpinning[i] = spinning;
        }

        if (advanced) {
            this.minecraft.player.playSound(SoundEvents.NOTE_BLOCK_HAT.value(), 0.18F, 1.35F);
        }
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
