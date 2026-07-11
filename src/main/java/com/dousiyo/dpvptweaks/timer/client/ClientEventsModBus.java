package com.dousiyo.dpvptweaks.timer.client;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.timer.network.CountdownHudS2CPacket;
import com.dousiyo.dpvptweaks.timer.network.TimerHudUpdateS2CPacket;
import com.dousiyo.dpvptweaks.timer.client.ClientCountdownState;
import com.dousiyo.dpvptweaks.timer.client.ClientTimerState;
import com.dousiyo.dpvptweaks.timer.client.CountdownHudOverlay;
import com.dousiyo.dpvptweaks.timer.client.TimerHudOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEventsModBus {
    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent e) {
        TimerHudUpdateS2CPacket.CLIENT_HANDLER = ClientTimerState::apply;
        CountdownHudS2CPacket.CLIENT_HANDLER = ClientCountdownState::apply;

        e.registerAboveAll("countdown_hud", (gui, g, partialTick, w, h) -> {
            CountdownHudOverlay.render(g, w, h);
        });
        e.registerAboveAll("timer_hud", (gui, g, partialTick, w, h) -> {
            TimerHudOverlay.render(g, w, h);
        });
    }
}
