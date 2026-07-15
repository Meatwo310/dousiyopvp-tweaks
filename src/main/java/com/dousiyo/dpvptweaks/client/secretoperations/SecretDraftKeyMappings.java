package com.dousiyo.dpvptweaks.client.secretoperations;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SecretDraftKeyMappings {
    public static final KeyMapping OPEN_DRAFT = new KeyMapping("key.dpvptweaks.secret_draft",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, "key.categories.dpvptweaks");
    private SecretDraftKeyMappings() {}
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) { event.register(OPEN_DRAFT); }
}
