package com.dousiyo.dpvptweaks.client.function;

import com.dousiyo.dpvptweaks.client.function.screen.FunctionPaletteScreen;
import com.dousiyo.dpvptweaks.functionpalette.FunctionPaletteCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class FunctionPaletteClient {
    private FunctionPaletteClient() {
    }

    public static void tryOpenPalette() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        if (!hasClientPermissionHint(mc)) {
            mc.player.displayClientMessage(Component.translatable("message.dpvptweaks.function_palette.no_permission"), true);
            return;
        }

        mc.setScreen(new FunctionPaletteScreen());
    }

    public static void applyPaletteData(List<FunctionPaletteCategory> categories) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof FunctionPaletteScreen screen) {
            screen.applyPaletteData(categories);
        }
    }

    private static boolean hasClientPermissionHint(Minecraft mc) {
        if (mc.player == null) {
            return false;
        }
        if (mc.player.hasPermissions(2)) {
            return true;
        }
        return mc.player.connection != null
                && mc.player.connection.getCommands().getRoot().getChild("function") != null;
    }
}
