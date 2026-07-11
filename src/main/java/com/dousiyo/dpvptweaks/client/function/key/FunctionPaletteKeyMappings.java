package com.dousiyo.dpvptweaks.client.function.key;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid=DpvpTweaks.MODID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class FunctionPaletteKeyMappings {
    public static final KeyMapping OPEN = new KeyMapping("key.dpvptweaks.open_function_palette", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P, "key.categories.dpvptweaks");
    private FunctionPaletteKeyMappings() {}
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent event){ event.register(OPEN); }
}
