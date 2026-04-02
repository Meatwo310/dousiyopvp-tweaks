package com.dousiyo.dpvptweaks.client;

import com.dousiyo.dpvptweaks.gui.LoadoutScreen;
import com.dousiyo.dpvptweaks.gui.MiniLoadoutScreen;
import net.minecraft.client.Minecraft;

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

    public static void openMiniLoadoutScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        mc.setScreen(new MiniLoadoutScreen());
    }
}