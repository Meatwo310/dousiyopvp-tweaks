package com.dousiyo.dpvptweaks.client.secretoperations;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.secretoperations.SecretShowdownPhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SecretShowdownHudEvents {
    private SecretShowdownHudEvents() {}

    public static void render(GuiGraphics gg, int width, int height) {
        if (!ClientShowdownState.participating()) return;
        Minecraft mc = Minecraft.getInstance(); if (mc.options.hideGui || mc.player == null) return;

        SecretShowdownPhase phase = ClientShowdownState.phase();
        boolean teamKillMatch = phase == SecretShowdownPhase.ACTIVE || phase == SecretShowdownPhase.OVERTIME;
        int cx = width / 2;
        if (teamKillMatch) renderTeamKillScore(gg, mc, cx);
        if (teamKillMatch)
            gg.drawCenteredString(mc.font, "PTS: " + ClientShowdownState.personalPoints(), cx, 60, 0xFFFFD66B);

        ClientSupplyCrateState.render(gg, width, height);

        if (ClientShowdownState.pendingDrafts() > 0)
            gg.drawString(mc.font, "SECRET TECH DRAFT x" + ClientShowdownState.pendingDrafts() + " [I]", 8, height - 28, 0xFF66FFFF);

        int statusY = teamKillMatch ? 45 : 8;
        if (ClientShowdownState.waiting()) gg.drawCenteredString(mc.font, "技術選択待機中", cx, statusY, 0xFFFFFFFF);
        else if (ClientShowdownState.dropProtected()) gg.drawCenteredString(mc.font, "降下保護中", cx, statusY, 0xFF77FFFF);
    }

    private static void renderTeamKillScore(GuiGraphics gg, Minecraft mc, int cx) {
        SecretShowdownPhase phase = ClientShowdownState.phase();
        String timer;
        if (phase == SecretShowdownPhase.OVERTIME) timer = "OVERTIME";
        else {
            long seconds = (ClientShowdownState.displayedRemainingTicks() + 19L) / 20L;
            timer = String.format("%d:%02d", seconds / 60L, seconds % 60L);
        }

        int top = 5;
        int blueLeft = cx - 55;
        int blueRight = cx - 1;
        int redLeft = cx + 1;
        int redRight = cx + 55;

        gg.fill(blueLeft - 1, top - 1, redRight + 1, top + 19, 0x88000000);
        gg.fill(blueLeft, top, blueRight, top + 18, 0xE500A9D6);
        gg.fill(redLeft, top, redRight, top + 18, 0xE5CC2818);
        gg.fill(blueLeft, top, blueRight, top + 1, 0xFF39D4F4);
        gg.fill(redLeft, top, redRight, top + 1, 0xFFFF5945);

        gg.drawCenteredString(mc.font, Integer.toString(ClientShowdownState.blueScore()), cx - 28, top + 5, 0xFFFFFFFF);
        gg.drawCenteredString(mc.font, Integer.toString(ClientShowdownState.redScore()), cx + 28, top + 5, 0xFFFFFFFF);

        gg.fill(cx - 42, top + 18, cx - 39, top + 25, 0xAA007FAD);
        gg.fill(cx + 39, top + 18, cx + 42, top + 25, 0xAA9D1D13);
        drawLargeCentered(gg, mc, timer, cx, top + 24, 0xFFFFFFFF);
    }

    private static void drawLargeCentered(GuiGraphics gg, Minecraft mc, String text, int x, int y, int color) {
        float scale = 1.25F;
        gg.pose().pushPose();
        gg.pose().scale(scale, scale, 1.0F);
        gg.drawCenteredString(mc.font, text, Math.round(x / scale), Math.round(y / scale), color);
        gg.pose().popPose();
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ClientShowdownState.clear(); }
}
