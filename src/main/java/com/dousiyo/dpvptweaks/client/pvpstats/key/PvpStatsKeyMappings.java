package com.dousiyo.dpvptweaks.client.pvpstats.key;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PvpStatsKeyMappings {
    public static final KeyMapping OPEN_PVP_STATS = new KeyMapping(
            "key.dpvptweaks.open_pvp_stats",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "key.categories.dpvptweaks"
    );

    private PvpStatsKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_PVP_STATS);
    }
}
