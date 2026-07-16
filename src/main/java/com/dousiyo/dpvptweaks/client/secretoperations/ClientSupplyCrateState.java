package com.dousiyo.dpvptweaks.client.secretoperations;

import com.dousiyo.dpvptweaks.network.SupplyCrateProgressPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class ClientSupplyCrateState {
    private static volatile boolean active;
    private static volatile int entityId = -1;
    private static volatile int progressTicks;
    private static volatile int totalTicks = 200;

    private ClientSupplyCrateState() {}

    public static void update(SupplyCrateProgressPacket packet) {
        active = packet.active();
        entityId = packet.entityId();
        progressTicks = Math.max(0, packet.progressTicks());
        totalTicks = Math.max(1, packet.totalTicks());
        if (!active) entityId = -1;
    }

    public static void clear() {
        active = false; entityId = -1; progressTicks = 0; totalTicks = 200;
    }

    public static void render(GuiGraphics graphics, int width, int height) {
        if (!active) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) return;
        int barWidth = 104;
        int x = (width - barWidth) / 2;
        int y = height / 2 + 28;
        float ratio = Mth.clamp(progressTicks / (float) totalTicks, 0.0F, 1.0F);
        graphics.fill(x, y, x + barWidth, y + 12, 0xCC101010);
        graphics.fill(x + 2, y + 2, x + 2 + Math.round((barWidth - 4) * ratio), y + 10, 0xFFE0B64B);
        int seconds = Math.max(0, (totalTicks - progressTicks + 19) / 20);
        graphics.drawCenteredString(minecraft.font, "補給物資を開封中 " + seconds + "秒", width / 2, y - 11, 0xFFFFFFFF);
    }
}
