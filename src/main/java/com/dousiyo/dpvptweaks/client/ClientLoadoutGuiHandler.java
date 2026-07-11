package com.dousiyo.dpvptweaks.client;

import com.dousiyo.dpvptweaks.gui.LoadoutScreen;
import com.dousiyo.dpvptweaks.gui.IntelDraftScreen;
import com.dousiyo.dpvptweaks.gui.MiniLoadoutScreen;
import com.dousiyo.dpvptweaks.inteldraft.IntelDraftDefinition;
import com.dousiyo.dpvptweaks.loadout.LoadoutDefinition;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class ClientLoadoutGuiHandler {
    private ClientLoadoutGuiHandler() {
    }

    public static void openLoadoutScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        mc.setScreen(new LoadoutScreen());
    }

    public static void openLoadoutScreen(List<LoadoutDefinition> loadouts) {
        openLoadoutScreen(loadouts, 0L);
    }

    public static void openLoadoutScreen(List<LoadoutDefinition> loadouts, long sessionId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        mc.setScreen(new LoadoutScreen(LoadoutScreen.buildPreviews(loadouts), LoadoutScreen.buildPacketIds(loadouts), sessionId));
    }

    public static void openMiniLoadoutScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        mc.setScreen(new MiniLoadoutScreen());
    }

    public static void openMiniLoadoutScreen(List<LoadoutDefinition> loadouts) {
        openMiniLoadoutScreen(loadouts, 0L);
    }

    public static void openMiniLoadoutScreen(List<LoadoutDefinition> loadouts, long sessionId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        mc.setScreen(new MiniLoadoutScreen(loadouts, sessionId));
    }

    public static void openIntelDraftScreen(IntelDraftDefinition definition) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        mc.setScreen(new IntelDraftScreen(definition));
    }

    public static void closeIntelDraftScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (mc.screen instanceof IntelDraftScreen) {
            mc.setScreen(null);
        }
    }

    public static void closeLoadoutScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (mc.screen instanceof LoadoutScreen) {
            mc.setScreen(null);
        }
    }
}
