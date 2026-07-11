package com.dousiyo.dpvptweaks.gui;

import com.dousiyo.dpvptweaks.client.ClientLoadoutRegistry;
import com.dousiyo.dpvptweaks.config.ClientConfig;
import com.dousiyo.dpvptweaks.loadout.LoadoutDefinition;
import com.dousiyo.dpvptweaks.network.ClientNetwork;
import com.dousiyo.dpvptweaks.network.LoadoutGuiNetwork;
import com.dousiyo.dpvptweaks.network.SelectLoadoutPacket;
import com.dousiyo.dpvptweaks.network.SelectLoadoutGuiPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LoadoutScreen extends Screen {
    private static final String MODID = "dpvptweaks";

    private static final int PANEL_W = 384;
    private static final int PANEL_H = 192;

    private static final int GRID_COLS = 4;
    private static final int GRID_ROWS = 2;
    private static final int SLOTS_PER_PAGE = GRID_COLS * GRID_ROWS;

    private static final int CARD_W = 84;
    private static final int CARD_H = 48;
    private static final int CARD_GAP = 6;

    private static final int GRID_X = 15;
    private static final int GRID_Y = 34;
    private static final int GRID_STEP_X = CARD_W + CARD_GAP;
    private static final int GRID_STEP_Y = CARD_H + CARD_GAP;

    private static final int ICON_Y = 6;
    private static final int ICON1_X = 16;
    private static final int ICON2_X = 34;
    private static final int ICON3_X = 52;
    private static final int NAME_Y = 30;

    private static final int PAGE_LEFT_X = 303;
    private static final int PAGE_Y = 8;
    private static final int PAGE_ARROW_W = 16;
    private static final int PAGE_ARROW_H = 16;

    private static final int PAGE_BOX_X = 323;
    private static final int PAGE_BOX_Y = 8;
    private static final int PAGE_BOX_W = 26;

    private static final int PAGE_RIGHT_X = 353;

    private static final int CONFIRM_X = 144;
    private static final int CONFIRM_Y = 164;
    private static final int CONFIRM_W = 96;
    private static final int CONFIRM_H = 20;

    private static final int COLOR_TITLE = 0xFFE7E7E7;
    private static final int COLOR_LINE = 0xFF5A5A5A;
    private static final int COLOR_PAGE = 0xFFD9D9D9;
    private static final int COLOR_NAME_NORMAL = 0xFFD8D8D8;
    private static final int COLOR_NAME_SELECTED = 0xFFFFD37A;
    private static final int COLOR_FOOTER = 0xFFBBBBBB;

    private static final int CARD_TEX_W = 336;
    private static final int CARD_TEX_H = 48;
    private static final int CONFIRM_TEX_W = 288;
    private static final int CONFIRM_TEX_H = 20;
    private static final int ARROWS_TEX_W = 96;
    private static final int ARROWS_TEX_H = 16;

    private int guiLeft;
    private int guiTop;

    private final ResourceLocation panelTex;
    private final ResourceLocation cardTex;
    private final ResourceLocation confirmTex;
    private final ResourceLocation arrowsTex;

    private final List<LoadoutPreview> loadouts;
    private final Map<Integer, String> packetLoadoutIdByPreviewId;
    private final long sessionId;
    private int currentPage = 0;
    private Integer selectedLoadoutId = null;

    public LoadoutScreen() {
        this(buildDefaultPreviews(), buildDefaultPacketIds(), 0L);
    }

    public LoadoutScreen(List<LoadoutPreview> loadouts) {
        this(loadouts, buildSequentialPacketIds(loadouts), 0L);
    }

    public LoadoutScreen(List<LoadoutPreview> loadouts, Map<Integer, String> packetLoadoutIdByPreviewId) {
        this(loadouts, packetLoadoutIdByPreviewId, 0L);
    }

    public LoadoutScreen(List<LoadoutPreview> loadouts, Map<Integer, String> packetLoadoutIdByPreviewId, long sessionId) {
        super(Component.literal("Loadout Select"));
        String themeFolder = ClientConfig.LOADOUT_THEME_MODE.get().resolveFolder();
        this.panelTex = texture(themeFolder, "loadout_panel.png");
        this.cardTex = texture(themeFolder, "loadout_card_buttons.png");
        this.confirmTex = texture(themeFolder, "confirm_button.png");
        this.arrowsTex = texture(themeFolder, "page_arrows.png");
        this.loadouts = new ArrayList<>(Objects.requireNonNull(loadouts));
        this.packetLoadoutIdByPreviewId = new LinkedHashMap<>(Objects.requireNonNull(packetLoadoutIdByPreviewId));
        this.sessionId = sessionId;
    }

    private static ResourceLocation texture(String folder, String fileName) {
        return ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/" + folder + "/" + fileName);
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - PANEL_W) / 2;
        this.guiTop = (this.height - PANEL_H) / 2;
        clampPage();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gg);

        RenderSystem.enableBlend();
        gg.blit(panelTex, guiLeft, guiTop, 0, 0, PANEL_W, PANEL_H, PANEL_W, PANEL_H);

        gg.drawCenteredString(this.font, "ロードアウトを選択", guiLeft + (PANEL_W / 2), guiTop + 10, COLOR_TITLE);
        gg.hLine(guiLeft + 15, guiLeft + 284, guiTop + 24, COLOR_LINE);

        renderPageControls(gg, mouseX, mouseY);
        renderCards(gg, mouseX, mouseY);

        gg.hLine(guiLeft + 15, guiLeft + 368, guiTop + 146, COLOR_LINE);
        Component footer = Component.literal("選択中のロードアウトを適用");
        gg.drawCenteredString(this.font, footer, guiLeft + (PANEL_W / 2), guiTop + 152, COLOR_FOOTER);

        renderConfirmButton(gg, mouseX, mouseY);

        super.render(gg, mouseX, mouseY, partialTick);
        renderButtonTooltips(gg, mouseX, mouseY);
        renderCardTooltips(gg, mouseX, mouseY);
    }

    private void renderPageControls(GuiGraphics gg, int mouseX, int mouseY) {
        boolean canPrev = currentPage > 0;
        boolean canNext = currentPage < getPageCount() - 1;

        boolean hoverPrev = canPrev && isInside(mouseX, mouseY, PAGE_LEFT_X, PAGE_Y, PAGE_ARROW_W, PAGE_ARROW_H);
        boolean hoverNext = canNext && isInside(mouseX, mouseY, PAGE_RIGHT_X, PAGE_Y, PAGE_ARROW_W, PAGE_ARROW_H);

        int leftFrame = !canPrev ? 2 : (hoverPrev ? 1 : 0);
        blitArrowFrame(gg, guiLeft + PAGE_LEFT_X, guiTop + PAGE_Y, leftFrame);

        int rightFrame = !canNext ? 5 : (hoverNext ? 4 : 3);
        blitArrowFrame(gg, guiLeft + PAGE_RIGHT_X, guiTop + PAGE_Y, rightFrame);

        String pageText = (currentPage + 1) + "/" + getPageCount();
        int textX = guiLeft + PAGE_BOX_X + (PAGE_BOX_W - this.font.width(pageText)) / 2;
        int textY = guiTop + PAGE_BOX_Y + 4;
        gg.drawString(this.font, pageText, textX, textY, COLOR_PAGE, false);
    }

    private void renderCards(GuiGraphics gg, int mouseX, int mouseY) {
        int pageStart = currentPage * SLOTS_PER_PAGE;
        for (int slot = 0; slot < SLOTS_PER_PAGE; slot++) {
            int col = slot % GRID_COLS;
            int row = slot / GRID_COLS;

            int cardX = GRID_X + col * GRID_STEP_X;
            int cardY = GRID_Y + row * GRID_STEP_Y;

            int globalIndex = pageStart + slot;
            LoadoutPreview preview = globalIndex < loadouts.size() ? loadouts.get(globalIndex) : null;
            boolean hovered = isInside(mouseX, mouseY, cardX, cardY, CARD_W, CARD_H);

            CardVisualState state;
            if (preview == null) {
                state = CardVisualState.DISABLED;
            } else if (selectedLoadoutId != null && selectedLoadoutId == preview.id()) {
                state = CardVisualState.SELECTED;
            } else if (hovered) {
                state = CardVisualState.HOVER;
            } else {
                state = CardVisualState.NORMAL;
            }

            blitCardFrame(gg, guiLeft + cardX, guiTop + cardY, state);
            if (preview != null) {
                renderCardContents(gg, preview, guiLeft + cardX, guiTop + cardY, state);
            }
        }
    }

    private void renderCardContents(GuiGraphics gg, LoadoutPreview preview, int absCardX, int absCardY, CardVisualState state) {
        ItemStack s1 = preview.weaponAt(0);
        ItemStack s2 = preview.weaponAt(1);
        ItemStack s3 = preview.weaponAt(2);

        if (!s1.isEmpty()) gg.renderItem(s1, absCardX + ICON1_X, absCardY + ICON_Y);
        if (!s2.isEmpty()) gg.renderItem(s2, absCardX + ICON2_X, absCardY + ICON_Y);
        if (!s3.isEmpty()) gg.renderItem(s3, absCardX + ICON3_X, absCardY + ICON_Y);

        String rawName = preview.name().getString();
        String name = this.font.plainSubstrByWidth(rawName, CARD_W - 8);
        int color = (state == CardVisualState.SELECTED) ? COLOR_NAME_SELECTED : COLOR_NAME_NORMAL;

        gg.drawString(this.font, name, absCardX + (CARD_W - this.font.width(name)) / 2, absCardY + NAME_Y, color, false);
    }

    private void renderConfirmButton(GuiGraphics gg, int mouseX, int mouseY) {
        boolean enabled = selectedLoadoutId != null;
        boolean hovered = enabled && isInside(mouseX, mouseY, CONFIRM_X, CONFIRM_Y, CONFIRM_W, CONFIRM_H);

        int frame = !enabled ? 2 : (hovered ? 1 : 0);
        int u = frame * CONFIRM_W;
        gg.blit(confirmTex, guiLeft + CONFIRM_X, guiTop + CONFIRM_Y, u, 0, CONFIRM_W, CONFIRM_H, CONFIRM_TEX_W, CONFIRM_TEX_H);

        Component label = Component.literal("確定");
        int labelColor = enabled ? 0xFFEDEDED : 0xFF888888;
        gg.drawCenteredString(this.font, label, guiLeft + CONFIRM_X + (CONFIRM_W / 2), guiTop + CONFIRM_Y + 6, labelColor);
    }

    private void renderCardTooltips(GuiGraphics gg, int mouseX, int mouseY) {
        int hoveredSlot = getHoveredCardSlot(mouseX, mouseY);
        if (hoveredSlot < 0) {
            return;
        }
        int globalIndex = currentPage * SLOTS_PER_PAGE + hoveredSlot;
        if (globalIndex < 0 || globalIndex >= loadouts.size()) {
            return;
        }
        LoadoutPreview preview = loadouts.get(globalIndex);
        List<Component> lines = new ArrayList<>();
        lines.add(preview.name());
        if (!preview.weaponSummary().getString().isBlank()) {
            lines.add(preview.weaponSummary());
        }
        if (!preview.description().getString().isBlank()) {
            lines.add(preview.description());
        }
        gg.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    private void renderButtonTooltips(GuiGraphics gg, int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY, PAGE_LEFT_X, PAGE_Y, PAGE_ARROW_W, PAGE_ARROW_H)) {
            if (currentPage > 0) {
                gg.renderTooltip(this.font, Component.literal("前のページ"), mouseX, mouseY);
            } else {
                gg.renderTooltip(this.font, Component.literal("これ以上前のページはありません"), mouseX, mouseY);
            }
            return;
        }

        if (isInside(mouseX, mouseY, PAGE_RIGHT_X, PAGE_Y, PAGE_ARROW_W, PAGE_ARROW_H)) {
            if (currentPage < getPageCount() - 1) {
                gg.renderTooltip(this.font, Component.literal("次のページ"), mouseX, mouseY);
            } else {
                gg.renderTooltip(this.font, Component.literal("これ以上次のページはありません"), mouseX, mouseY);
            }
            return;
        }

        if (isInside(mouseX, mouseY, CONFIRM_X, CONFIRM_Y, CONFIRM_W, CONFIRM_H)) {
            if (selectedLoadoutId != null) {
                gg.renderTooltip(this.font, Component.literal("選択中のロードアウトを確定"), mouseX, mouseY);
            } else {
                gg.renderTooltip(this.font, Component.literal("先にロードアウトを選択してください"), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (isInside(mouseX, mouseY, PAGE_LEFT_X, PAGE_Y, PAGE_ARROW_W, PAGE_ARROW_H) && currentPage > 0) {
            currentPage--;
            return true;
        }
        if (isInside(mouseX, mouseY, PAGE_RIGHT_X, PAGE_Y, PAGE_ARROW_W, PAGE_ARROW_H) && currentPage < getPageCount() - 1) {
            currentPage++;
            return true;
        }

        int hoveredSlot = getHoveredCardSlot(mouseX, mouseY);
        if (hoveredSlot >= 0) {
            int globalIndex = currentPage * SLOTS_PER_PAGE + hoveredSlot;
            if (globalIndex >= 0 && globalIndex < loadouts.size()) {
                this.selectedLoadoutId = loadouts.get(globalIndex).id();
                return true;
            }
        }

        if (isInside(mouseX, mouseY, CONFIRM_X, CONFIRM_Y, CONFIRM_W, CONFIRM_H) && selectedLoadoutId != null) {
            confirmSelection(selectedLoadoutId);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void confirmSelection(int loadoutId) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
        }

        String packetLoadoutId = packetLoadoutIdByPreviewId.get(loadoutId);
        if (packetLoadoutId == null || packetLoadoutId.isBlank()) {
            packetLoadoutId = Integer.toString(loadoutId);
        }
        if (sessionId > 0L) {
            LoadoutGuiNetwork.CHANNEL.sendToServer(new SelectLoadoutGuiPacket(sessionId, packetLoadoutId));
        } else {
            ClientNetwork.CHANNEL.sendToServer(new SelectLoadoutPacket(packetLoadoutId));
        }
        this.onClose();
    }

    private static List<LoadoutPreview> buildDefaultPreviews() {
        List<LoadoutPreview> previews = new ArrayList<>();
        int previewId = 1;
        for (ClientLoadoutRegistry.ClientLoadout loadout : ClientLoadoutRegistry.all().values()) {
            List<ItemStack> weapons = new ArrayList<>(3);
            weapons.add(loadout.gunStacks.size() > 0 ? loadout.gunStacks.get(0) : ItemStack.EMPTY);
            weapons.add(loadout.gunStacks.size() > 1 ? loadout.gunStacks.get(1) : ItemStack.EMPTY);
            weapons.add(loadout.gunStacks.size() > 2 ? loadout.gunStacks.get(2) : ItemStack.EMPTY);
            previews.add(new LoadoutPreview(
                    previewId,
                    Component.literal(loadout.name),
                    weapons,
                    Component.literal(loadout.weapons),
                    Component.literal(loadout.description)
            ));
            previewId++;
        }
        return previews;
    }

    public static List<LoadoutPreview> buildPreviews(List<LoadoutDefinition> definitions) {
        List<LoadoutPreview> previews = new ArrayList<>();
        int previewId = 1;
        for (LoadoutDefinition loadout : definitions) {
            List<ItemStack> weapons = new ArrayList<>(3);
            weapons.add(loadout.gunStacks().size() > 0 ? loadout.gunStacks().get(0) : ItemStack.EMPTY);
            weapons.add(loadout.gunStacks().size() > 1 ? loadout.gunStacks().get(1) : ItemStack.EMPTY);
            weapons.add(loadout.gunStacks().size() > 2 ? loadout.gunStacks().get(2) : ItemStack.EMPTY);
            previews.add(new LoadoutPreview(
                    previewId,
                    Component.literal(loadout.name()),
                    weapons,
                    Component.literal(loadout.weapons()),
                    Component.literal(loadout.description())
            ));
            previewId++;
        }
        return previews;
    }

    private static Map<Integer, String> buildDefaultPacketIds() {
        Map<Integer, String> mapping = new LinkedHashMap<>();
        int previewId = 1;
        for (ClientLoadoutRegistry.ClientLoadout loadout : ClientLoadoutRegistry.all().values()) {
            mapping.put(previewId, loadout.id);
            previewId++;
        }
        return mapping;
    }

    public static Map<Integer, String> buildPacketIds(List<LoadoutDefinition> definitions) {
        Map<Integer, String> mapping = new LinkedHashMap<>();
        int previewId = 1;
        for (LoadoutDefinition loadout : definitions) {
            mapping.put(previewId, loadout.id());
            previewId++;
        }
        return mapping;
    }

    private static Map<Integer, String> buildSequentialPacketIds(List<LoadoutPreview> loadouts) {
        Map<Integer, String> mapping = new LinkedHashMap<>();
        for (LoadoutPreview preview : loadouts) {
            mapping.put(preview.id(), Integer.toString(preview.id()));
        }
        return mapping;
    }

    private int getPageCount() {
        int pages = (loadouts.size() + SLOTS_PER_PAGE - 1) / SLOTS_PER_PAGE;
        return Math.max(1, pages);
    }

    private void clampPage() {
        int max = getPageCount() - 1;
        if (currentPage < 0) currentPage = 0;
        if (currentPage > max) currentPage = max;
    }

    private int getHoveredCardSlot(double mouseX, double mouseY) {
        for (int slot = 0; slot < SLOTS_PER_PAGE; slot++) {
            int col = slot % GRID_COLS;
            int row = slot / GRID_COLS;
            int cardX = GRID_X + col * GRID_STEP_X;
            int cardY = GRID_Y + row * GRID_STEP_Y;
            if (isInside(mouseX, mouseY, cardX, cardY, CARD_W, CARD_H)) {
                return slot;
            }
        }
        return -1;
    }

    private boolean isInside(double mouseX, double mouseY, int relX, int relY, int w, int h) {
        int x1 = guiLeft + relX;
        int y1 = guiTop + relY;
        return mouseX >= x1 && mouseX < x1 + w && mouseY >= y1 && mouseY < y1 + h;
    }

    private void blitCardFrame(GuiGraphics gg, int x, int y, CardVisualState state) {
        int frame = switch (state) {
            case NORMAL -> 0;
            case HOVER -> 1;
            case SELECTED -> 2;
            case DISABLED -> 3;
        };
        gg.blit(cardTex, x, y, frame * CARD_W, 0, CARD_W, CARD_H, CARD_TEX_W, CARD_TEX_H);
    }

    private void blitArrowFrame(GuiGraphics gg, int x, int y, int frame) {
        gg.blit(arrowsTex, x, y, frame * 16, 0, 16, 16, ARROWS_TEX_W, ARROWS_TEX_H);
    }

    private enum CardVisualState {
        NORMAL, HOVER, SELECTED, DISABLED
    }

    public record LoadoutPreview(int id, Component name, List<ItemStack> weaponPreviews, Component weaponSummary, Component description) {
        public LoadoutPreview {
            weaponPreviews = List.copyOf(weaponPreviews);
            weaponSummary = Objects.requireNonNullElse(weaponSummary, Component.empty());
            description = Objects.requireNonNullElse(description, Component.empty());
        }

        public LoadoutPreview(int id, Component name, List<ItemStack> weaponPreviews) {
            this(id, name, weaponPreviews, Component.empty(), Component.empty());
        }

        public ItemStack weaponAt(int index) {
            if (index < 0 || index >= weaponPreviews.size()) return ItemStack.EMPTY;
            ItemStack stack = weaponPreviews.get(index);
            return stack == null ? ItemStack.EMPTY : stack;
        }
    }
}
