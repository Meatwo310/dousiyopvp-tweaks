package com.dousiyo.dpvptweaks.client.pvpstats.key;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.client.pvpstats.PvpStatsClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PvpStatsKeyHandler {
    private PvpStatsKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        while (PvpStatsKeyMappings.OPEN_PVP_STATS.consumeClick()) {
            PvpStatsClient.tryOpenOwnStats();
        }
    }
}
