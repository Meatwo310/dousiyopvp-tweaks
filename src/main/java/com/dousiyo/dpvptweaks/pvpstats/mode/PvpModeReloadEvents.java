package com.dousiyo.dpvptweaks.pvpstats.mode;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PvpModeReloadEvents {
    private PvpModeReloadEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new PvpModeReloadListener());
    }
}
