package com.dousiyo.dpvptweaks.client.pvpstats;

import com.dousiyo.dpvptweaks.client.pvpstats.screen.PvpStatsScreen;
import com.dousiyo.dpvptweaks.pvpstats.model.PlayerPrivacySettings;
import com.dousiyo.dpvptweaks.pvpstats.model.StatsGuiPayload;
import com.dousiyo.dpvptweaks.pvpstats.network.PvpStatsNetwork;
import com.dousiyo.dpvptweaks.pvpstats.network.c2s.RequestOwnStatsPacket;
import com.dousiyo.dpvptweaks.pvpstats.network.c2s.UpdatePrivacySettingsPacket;
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
        if (mc.screen instanceof PvpStatsScreen statsScreen) {
            statsScreen.refreshPayload(payload);
            return;
        }
        mc.setScreen(new PvpStatsScreen(payload));
    }

    public static void updatePrivacySettings(PlayerPrivacySettings settings) {
        PvpStatsNetwork.CHANNEL.sendToServer(new UpdatePrivacySettingsPacket(settings));
    }

    public static void showError(Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(message, true);
        }
    }
}
