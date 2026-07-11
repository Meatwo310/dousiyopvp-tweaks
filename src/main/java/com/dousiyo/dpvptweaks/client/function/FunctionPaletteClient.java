package com.dousiyo.dpvptweaks.client.function;

import com.dousiyo.dpvptweaks.client.function.screen.FunctionPaletteScreen;
import com.dousiyo.dpvptweaks.functionpalette.FunctionPaletteCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class FunctionPaletteClient {
    private FunctionPaletteClient() {}
    public static void tryOpenPalette() {
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=null && mc.level!=null && mc.screen==null) mc.setScreen(new FunctionPaletteScreen());
    }
    public static void applyPaletteData(FunctionPaletteCategory data) {
        var mc=Minecraft.getInstance(); if(mc.screen instanceof FunctionPaletteScreen screen) screen.applyPaletteData(data);
    }
    public static void showResult(Component message) {
        var mc=Minecraft.getInstance(); if(mc.player!=null) mc.player.displayClientMessage(message, false);
    }
}
