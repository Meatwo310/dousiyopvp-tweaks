package com.dousiyo.dpvptweaks.client.function.screen;

import com.dousiyo.dpvptweaks.config.ClientConfig;
import com.dousiyo.dpvptweaks.functionpalette.FunctionPaletteAction;
import com.dousiyo.dpvptweaks.functionpalette.FunctionPaletteCategory;
import com.dousiyo.dpvptweaks.network.FunctionPaletteNetwork;
import com.dousiyo.dpvptweaks.network.functionpalette.c2s.RequestFunctionListPacket;
import com.dousiyo.dpvptweaks.network.functionpalette.c2s.RunFunctionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class FunctionPaletteScreen extends Screen {
    private static final int BUTTON_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 6;

    private final List<FunctionPaletteCategory> categories = new ArrayList<>();
    private final List<Button> pageButtons = new ArrayList<>();

    private Button refreshButton;
    private Button backButton;
    private FunctionPaletteCategory activeCategory;
    private boolean loading = true;
    private boolean requestedList;

    public FunctionPaletteScreen() {
        super(Component.translatable("gui.dpvptweaks.function_palette.title"));
    }

    @Override
    protected void init() {
        super.init();
        rebuildWidgets();

        if (!requestedList) {
            requestedList = true;
            requestFunctionList();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();
        pageButtons.clear();

        int centerX = this.width / 2;
        int top = topY();

        this.refreshButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.dpvptweaks.function_palette.refresh"),
                        button -> requestFunctionList()
                )
                .bounds(centerX + 48, top, 84, BUTTON_HEIGHT)
                .build());

        if (activeCategory != null) {
            this.backButton = addRenderableWidget(Button.builder(
                            Component.translatable("gui.dpvptweaks.function_palette.back"),
                            button -> openMainPage()
                    )
                    .bounds(centerX - 132, top, 84, BUTTON_HEIGHT)
                    .build());
        } else {
            this.backButton = null;
        }

        List<ButtonSpec> specs = currentButtonSpecs();
        int totalHeight = specs.size() * BUTTON_HEIGHT + Math.max(0, specs.size() - 1) * BUTTON_GAP;
        int startY = top + 42 + Math.max(0, (176 - totalHeight) / 2);

        for (int i = 0; i < specs.size(); i++) {
            ButtonSpec spec = specs.get(i);
            Button button = addRenderableWidget(Button.builder(spec.label(), clicked -> spec.onClick().run())
                    .bounds(centerX - BUTTON_WIDTH / 2, startY + i * (BUTTON_HEIGHT + BUTTON_GAP), BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
            pageButtons.add(button);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int centerX = this.width / 2;
        int top = topY();
        guiGraphics.drawCenteredString(this.font, this.title, centerX, top - 20, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, currentSubtitle(), centerX, top - 8, 0xA0A0A0);

        if (loading) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.dpvptweaks.function_palette.status.loading"), centerX, top + 26, 0xA0A0A0);
        } else if (pageButtons.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.dpvptweaks.function_palette.menu_empty"), centerX, top + 26, 0xA0A0A0);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    public void applyPaletteData(List<FunctionPaletteCategory> categories) {
        this.loading = false;
        this.categories.clear();
        this.categories.addAll(categories);

        if (activeCategory != null) {
            activeCategory = this.categories.stream()
                    .filter(category -> category.id().equals(activeCategory.id()))
                    .findFirst()
                    .orElse(null);
        }

        rebuildWidgets();
    }

    private void requestFunctionList() {
        this.loading = true;
        FunctionPaletteNetwork.CHANNEL.sendToServer(new RequestFunctionListPacket());
    }

    private void openMainPage() {
        this.activeCategory = null;
        rebuildWidgets();
    }

    private void openCategory(FunctionPaletteCategory category) {
        this.activeCategory = category;
        rebuildWidgets();
    }

    private void runAction(FunctionPaletteAction action) {
        FunctionPaletteNetwork.CHANNEL.sendToServer(new RunFunctionPacket(action.functionId()));
        if (ClientConfig.FUNCTION_PALETTE_CLOSE_AFTER_RUN.get()) {
            this.onClose();
        }
    }

    private List<ButtonSpec> currentButtonSpecs() {
        List<ButtonSpec> specs = new ArrayList<>();
        if (activeCategory == null) {
            for (FunctionPaletteCategory category : categories) {
                specs.add(new ButtonSpec(Component.literal(category.displayName()), () -> openCategory(category)));
            }
            return specs;
        }

        for (FunctionPaletteAction action : activeCategory.actions()) {
            specs.add(new ButtonSpec(Component.literal(action.label()), () -> runAction(action)));
        }
        return specs;
    }

    private Component currentSubtitle() {
        if (activeCategory == null) {
            return Component.translatable("gui.dpvptweaks.function_palette.menu_heading");
        }
        return Component.translatable("gui.dpvptweaks.function_palette.category_title", activeCategory.displayName());
    }

    private int topY() {
        return (this.height - 220) / 2;
    }

    private record ButtonSpec(Component label, Runnable onClick) {
    }
}
