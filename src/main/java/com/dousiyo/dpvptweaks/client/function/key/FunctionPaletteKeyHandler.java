package com.dousiyo.dpvptweaks.client.function.key;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.client.function.FunctionPaletteClient;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FunctionPaletteKeyHandler {
    private static boolean wasPressed;

    private FunctionPaletteKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) {
            return;
        }

        boolean pressed = InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_P);
        if (pressed && !wasPressed) {
            FunctionPaletteClient.tryOpenPalette();
        }
        wasPressed = pressed;
    }
}
