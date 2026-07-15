package com.dousiyo.dpvptweaks.client.secretoperations;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

/** Top-center escort HUD for SECRET: CONVOY. */
@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SecretConvoyHudEvents {
    private static final int CYAN = 0xFF27D7EE;
    private static final int TRACK = 0xD91A2029;
    private static final int WHITE = 0xFFF4F7FA;
    private static final int RED = 0xFFF04444;

    private SecretConvoyHudEvents() {}

    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        if (!ClientSecretConvoyHudState.visible()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.font == null) return;

        int centerX = screenWidth / 2;
        int barWidth = Math.max(100, Math.min(280, screenWidth - 92));
        int barLeft = centerX - barWidth / 2;
        int barRight = barLeft + barWidth;
        int titleY = 5;
        int barY = 20;

        graphics.drawCenteredString(minecraft.font,
                Component.translatable("hud.dpvptweaks.secret_convoy.title"), centerX, titleY, WHITE);

        graphics.fill(barLeft - 1, barY - 1, barRight + 1, barY + 6, 0xB0000000);
        graphics.fill(barLeft, barY, barRight, barY + 5, TRACK);
        int fillRight = barLeft + Math.round(barWidth * ClientSecretConvoyHudState.progress());
        if (fillRight > barLeft) graphics.fill(barLeft, barY, fillRight, barY + 5, CYAN);
        graphics.fill(barLeft, barY, barRight, barY + 1, 0x55FFFFFF);

        long seconds = (ClientSecretConvoyHudState.displayedRemainingTicks() + 19L) / 20L;
        String time = String.format(Locale.ROOT, "%d:%02d", seconds / 60L, seconds % 60L);
        String distance = String.format(Locale.ROOT, "%.0fm", ClientSecretConvoyHudState.remainingRouteDistance());
        graphics.drawString(minecraft.font, time, barLeft - minecraft.font.width(time) - 7, barY - 2, WHITE);
        graphics.drawString(minecraft.font, distance, barRight + 7, barY - 2, WHITE);

        int markerX = Math.max(barLeft, Math.min(barRight, fillRight));
        if (ClientSecretConvoyHudState.enemyBlocking()) drawBlockedMarker(graphics, minecraft, markerX, barY + 2);
        else drawCargoMarker(graphics, markerX, barY + 2);

        String nearby = Component.translatable("hud.dpvptweaks.secret_convoy.nearby",
                ClientSecretConvoyHudState.nearbyEscorts()).getString();
        int countX = Math.max(minecraft.font.width(nearby) / 2 + 2,
                Math.min(screenWidth - minecraft.font.width(nearby) / 2 - 2, markerX));
        graphics.drawCenteredString(minecraft.font, nearby, countX, barY + 13,
                ClientSecretConvoyHudState.nearbyEscorts() > 0 ? WHITE : 0xFFB5BBC3);

        if (ClientSecretConvoyHudState.enemyBlocking())
            graphics.drawCenteredString(minecraft.font,
                    Component.translatable("hud.dpvptweaks.secret_convoy.blocked"), centerX, barY + 25, RED);
        else if (ClientSecretConvoyHudState.nearbyEscorts() == 0)
            graphics.drawCenteredString(minecraft.font,
                    Component.translatable("hud.dpvptweaks.secret_convoy.waiting"), centerX, barY + 25, 0xFFB5BBC3);
    }

    private static void drawCargoMarker(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 5, y - 5, x + 6, y + 6, 0xE7000000);
        graphics.fill(x - 4, y - 4, x + 5, y + 5, WHITE);
        graphics.fill(x - 2, y - 2, x + 3, y + 3, CYAN);
        graphics.fill(x - 4, y - 4, x + 5, y - 3, 0xFFFFFFFF);
    }

    private static void drawBlockedMarker(GuiGraphics graphics, Minecraft minecraft, int x, int y) {
        graphics.fill(x - 5, y - 4, x + 6, y + 5, 0xE7000000);
        graphics.fill(x - 4, y - 5, x + 5, y + 6, 0xE7000000);
        graphics.fill(x - 4, y - 3, x + 5, y + 4, RED);
        graphics.fill(x - 3, y - 4, x + 4, y + 5, RED);
        graphics.drawCenteredString(minecraft.font, "!", x, y - 4, WHITE);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientSecretConvoyHudState.clear();
    }
}
