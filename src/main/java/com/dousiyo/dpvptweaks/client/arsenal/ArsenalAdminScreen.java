package com.dousiyo.dpvptweaks.client.arsenal;

import com.dousiyo.dpvptweaks.network.arsenal.ArsenalAdminActionPacket;
import com.dousiyo.dpvptweaks.network.arsenal.ArsenalNetwork;
import com.dousiyo.dpvptweaks.network.arsenal.OpenArsenalAdminPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ArsenalAdminScreen extends Screen {
    private final OpenArsenalAdminPacket data;
    private EditBox weaponSet;
    private EditBox stage;
    private EditBox reserveMagazines;

    private ArsenalAdminScreen(OpenArsenalAdminPacket data) {
        super(Component.literal("ARSENAL 管理"));
        this.data = data;
    }

    public static void open(OpenArsenalAdminPacket data) {
        Minecraft.getInstance().setScreen(new ArsenalAdminScreen(data));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        weaponSet = new EditBox(font, cx - 155, 57, 150, 20, Component.literal("武器セットID"));
        weaponSet.setValue(data.selectedWeaponSet());
        weaponSet.setMaxLength(64);
        weaponSet.setFilter(value -> value.matches("[a-zA-Z0-9_.-]{0,64}"));
        addRenderableWidget(weaponSet);

        stage = new EditBox(font, cx + 5, 57, 65, 20, Component.literal("段階"));
        stage.setValue("1"); stage.setFilter(value -> value.matches("\\d{0,2}"));
        addRenderableWidget(stage);
        reserveMagazines = new EditBox(font, cx + 80, 57, 75, 20, Component.literal("予備マガジン"));
        reserveMagazines.setValue("4"); reserveMagazines.setFilter(value -> value.matches("\\d{0,3}"));
        addRenderableWidget(reserveMagazines);

        addRenderableWidget(Button.builder(Component.literal("段階登録"), button -> send(ArsenalAdminActionPacket.Action.REGISTER))
                .tooltip(Tooltip.create(Component.literal("メインハンドのアイテムを入力した段階へ登録します。TaCZ銃は構成を正規化し、その他は個数とNBTを保存します。")))
                .bounds(cx - 155, 84, 72, 20).build());
        addRenderableWidget(Button.builder(Component.literal("同じ銃×30"), button -> send(ArsenalAdminActionPacket.Action.REGISTER_ALL))
                .tooltip(Tooltip.create(Component.literal("デバッグ用：メインハンドの同じアイテムを全30段階へ一括登録します。")))
                .bounds(cx - 78, 84, 78, 20).build());
        addRenderableWidget(Button.builder(Component.literal("持ち物30丁"), button -> send(ArsenalAdminActionPacket.Action.REGISTER_INVENTORY))
                .tooltip(Tooltip.create(Component.literal("空でない30スタックを、メイン欄の左上→右下、続いてホットバー左→右の順で登録します。空き枠だけを飛ばします。")))
                .bounds(cx + 5, 84, 85, 20).build());
        addRenderableWidget(Button.builder(Component.literal("検証"), button -> send(ArsenalAdminActionPacket.Action.VALIDATE))
                .tooltip(Tooltip.create(Component.literal("30段階、銃ID、アタッチメント、射撃モード、予備弾設定を検証します。")))
                .bounds(cx + 95, 84, 60, 20).build());

        addRenderableWidget(Button.builder(Component.literal("開始"), button -> send(ArsenalAdminActionPacket.Action.START))
                .tooltip(Tooltip.create(Component.literal("非adminのオンラインプレイヤー全員で試合を開始します。2人以上と有効な武器セットが必要です。")))
                .bounds(cx - 155, 110, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("中止"), button -> send(ArsenalAdminActionPacket.Action.STOP))
                .tooltip(Tooltip.create(Component.literal("進行中の試合をFINISHEDにし、戦績を記録せず試合装備を除去します。")))
                .bounds(cx - 80, 110, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("リセット"), button -> send(ArsenalAdminActionPacket.Action.RESET))
                .tooltip(Tooltip.create(Component.literal("試合状態と参加者進行を消去してWAITINGへ戻します。次の試合前に使用します。")))
                .bounds(cx - 5, 110, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("JSON再読込"), button -> send(ArsenalAdminActionPacket.Action.RELOAD))
                .tooltip(Tooltip.create(Component.literal("arsenal.jsonと全武器セットJSONをディスクから再読み込みします。進行中の試合には反映しません。")))
                .bounds(cx + 70, 110, 85, 20).build());
        addRenderableWidget(Button.builder(Component.literal("更新"), button -> send(ArsenalAdminActionPacket.Action.REFRESH))
                .tooltip(Tooltip.create(Component.literal("試合状態、武器セット一覧、参加者一覧をサーバーから再取得します。")))
                .bounds(cx - 40, height - 27, 80, 20).build());
    }

    private void send(ArsenalAdminActionPacket.Action action) {
        ArsenalNetwork.CHANNEL.sendToServer(new ArsenalAdminActionPacket(action, weaponSet.getValue(),
                parse(stage, 1), parse(reserveMagazines, 4)));
    }

    private int parse(EditBox box, int fallback) {
        try { return Integer.parseInt(box.getValue()); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int cx = width / 2;
        graphics.drawCenteredString(font, "ARSENAL 管理", cx, 12, 0xFFFFD36A);
        graphics.drawCenteredString(font, "状態: " + data.state()
                + (data.activeWeaponSet().isBlank() ? "" : "  /  使用中: " + data.activeWeaponSet()), cx, 27, 0xFFFFFFFF);
        graphics.drawString(font, "武器セットID", cx - 155, 45, 0xFFBBBBBB);
        graphics.drawString(font, "段階 1～30", cx + 5, 45, 0xFFBBBBBB);
        graphics.drawString(font, "予備MAG", cx + 80, 45, 0xFFBBBBBB);

        int panelTop = 140;
        int panelBottom = Math.max(panelTop + 30, height - 50);
        graphics.fill(cx - 155, panelTop, cx - 5, panelBottom, 0x99101010);
        graphics.fill(cx + 5, panelTop, cx + 155, panelBottom, 0x99101010);
        graphics.drawString(font, "武器セット", cx - 150, panelTop + 5, 0xFF77CCFF);
        graphics.drawString(font, "参加者（●オンライン）", cx + 10, panelTop + 5, 0xFF77FF77);
        drawList(graphics, data.weaponSets(), cx - 150, panelTop + 18, panelBottom);
        drawList(graphics, data.participants(), cx + 10, panelTop + 18, panelBottom);

        String message = !data.configError().isBlank() ? data.configError() : data.notice();
        if (!message.isBlank()) graphics.drawCenteredString(font, message, cx, height - 42,
                data.configError().isBlank() ? 0xFF77FF77 : 0xFFFF5555);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawList(GuiGraphics graphics, java.util.List<String> values, int x, int y, int bottom) {
        int max = Math.max(0, (bottom - y - 3) / 11);
        if (values.isEmpty()) graphics.drawString(font, "なし", x, y, 0xFF999999);
        for (int i = 0; i < Math.min(max, values.size()); i++)
            graphics.drawString(font, values.get(i), x, y + i * 11, 0xFFDDDDDD);
    }

    @Override public boolean isPauseScreen() { return false; }
}
