package com.dousiyo.dpvptweaks.client.pvpstats;

import com.dousiyo.dpvptweaks.client.pvpstats.screen.PvpStatsScreen;
import com.dousiyo.dpvptweaks.pvpstats.model.StatsGuiPayload;
import com.dousiyo.dpvptweaks.pvpstats.network.PvpStatsNetwork;
import com.dousiyo.dpvptweaks.pvpstats.network.c2s.RequestOwnStatsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class PvpStatsClient {
    private PvpStatsClient() {
    }

    public static void tryOpenOwnStats() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        PvpStatsNetwork.CHANNEL.sendToServer(new RequestOwnStatsPacket());
    }

    public static void openStatsScreen(StatsGuiPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new PvpStatsScreen(payload));
    }

    public static void showError(Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(message, true);
        }
    }
}
