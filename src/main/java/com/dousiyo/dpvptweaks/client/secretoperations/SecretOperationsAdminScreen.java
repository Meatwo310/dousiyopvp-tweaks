package com.dousiyo.dpvptweaks.client.secretoperations;

import com.dousiyo.dpvptweaks.network.secretoperations.OpenSecretOperationsAdminPacket;
import com.dousiyo.dpvptweaks.network.secretoperations.SecretOperationsAdminActionPacket;
import com.dousiyo.dpvptweaks.network.secretoperations.SecretOperationsNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import com.dousiyo.dpvptweaks.secretoperations.SecretOperationMode;

public final class SecretOperationsAdminScreen extends Screen {
    private final OpenSecretOperationsAdminPacket data;
    private EditBox duration;
    private EditBox interval;
    private SecretOperationMode mode;

    private SecretOperationsAdminScreen(OpenSecretOperationsAdminPacket data) {
        super(Component.literal("SECRET OPERATIONS 管理")); this.data = data; this.mode = data.mode();
    }
    public static void open(OpenSecretOperationsAdminPacket data) {
        Minecraft.getInstance().setScreen(new SecretOperationsAdminScreen(data));
    }

    @Override protected void init() {
        int cx = width / 2;
        duration = new EditBox(font, cx - 145, 68, 120, 20, Component.literal("試合時間"));
        duration.setValue(Integer.toString(data.durationMinutes())); duration.setFilter(s -> s.matches("\\d{0,2}")); addRenderableWidget(duration);
        interval = new EditBox(font, cx + 25, 68, 120, 20, Component.literal("ドラフト間隔"));
        interval.setValue(Integer.toString(data.draftIntervalMinutes())); interval.setFilter(s -> s.matches("\\d{0,2}")); addRenderableWidget(interval);

        addRenderableWidget(Button.builder(Component.literal("モード切替"), b -> {
            mode = mode == SecretOperationMode.SHOWDOWN ? SecretOperationMode.CONVOY : SecretOperationMode.SHOWDOWN;
            send(SecretOperationsAdminActionPacket.Action.REFRESH);
        }).bounds(cx - 50, 40, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("ランダム編成"), b -> send(SecretOperationsAdminActionPacket.Action.RANDOMIZE))
                .bounds(cx - 155, height - 64, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("開始"), b -> send(SecretOperationsAdminActionPacket.Action.START))
                .bounds(cx - 50, height - 64, 65, 20).build());
        addRenderableWidget(Button.builder(Component.literal("中止"), b -> send(SecretOperationsAdminActionPacket.Action.STOP))
                .bounds(cx + 20, height - 64, 65, 20).build());
        addRenderableWidget(Button.builder(Component.literal("JSON再読込"), b -> send(SecretOperationsAdminActionPacket.Action.RELOAD))
                .bounds(cx + 90, height - 64, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("更新"), b -> send(SecretOperationsAdminActionPacket.Action.REFRESH))
                .bounds(cx - 50, height - 38, 100, 20).build());
    }

    private void send(SecretOperationsAdminActionPacket.Action action) {
        SecretOperationsNetwork.CHANNEL.sendToServer(new SecretOperationsAdminActionPacket(mode, action, parse(duration, 20), parse(interval, 2)));
    }
    private int parse(EditBox box, int fallback) { try { return Integer.parseInt(box.getValue()); } catch (NumberFormatException ignored) { return fallback; } }

    @Override public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        renderBackground(gg); int cx = width / 2;
        gg.drawCenteredString(font, "SECRET OPERATIONS", cx, 16, 0xFFFFD36A);
        gg.drawCenteredString(font, "SECRET: " + data.mode() + "  /  " + data.phaseLabel()
                + (data.round() > 0 ? "  ROUND " + data.round() : ""), cx, 28, 0xFFFFFFFF);
        gg.drawString(font, "試合時間（分）1～60", cx - 145, 56, 0xFFBBBBBB);
        gg.drawString(font, "追加ドラフト間隔（分）1～10", cx + 25, 56, 0xFFBBBBBB);
        gg.drawCenteredString(font, "参加者 " + data.participants() + "   RED " + data.redScore() + " - " + data.blueScore() + " BLUE", cx, 96, 0xFFFFFFFF);
        drawRoster(gg, "RED", data.redPlayers(), cx - 155, 116, 0xFFFF6666);
        drawRoster(gg, "BLUE", data.bluePlayers(), cx + 15, 116, 0xFF6699FF);
        if (!data.configError().isBlank()) gg.drawCenteredString(font, data.configError(), cx, height - 88, 0xFFFF5555);
        else if (!data.notice().isBlank()) gg.drawCenteredString(font, data.notice(), cx, height - 88, 0xFF77FF77);
        super.render(gg, mouseX, mouseY, partialTick);
    }
    private void drawRoster(GuiGraphics gg, String title, List<String> names, int x, int y, int color) {
        gg.fill(x, y, x + 140, Math.min(height - 100, y + 18 + Math.max(1, names.size()) * 11), 0x99101010);
        gg.drawString(font, title + " (" + names.size() + ")", x + 5, y + 5, color);
        int max = Math.max(0, (height - 136) / 11);
        for (int i = 0; i < Math.min(max, names.size()); i++) gg.drawString(font, names.get(i), x + 8, y + 18 + i * 11, 0xFFDDDDDD);
    }
    @Override public boolean isPauseScreen() { return false; }
}
