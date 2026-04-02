package com.dousiyo.dpvptweaks.client.capture;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.network.CaptureFeatureStateS2CPacket;
import com.dousiyo.dpvptweaks.network.CapturePointEventS2CPacket;
import com.dousiyo.dpvptweaks.network.PlayerPointFocusS2CPacket;
import com.dousiyo.dpvptweaks.network.PlayerPointHudStateS2CPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CaptureClientModBusEvents {
    private CaptureClientModBusEvents() {}

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        CapturePointEventS2CPacket.CLIENT_HANDLER = ClientCapturePointsState::applyPointEvent;
        CaptureFeatureStateS2CPacket.CLIENT_HANDLER = ClientCapturePointsState::applyCaptureFeatureState;
        PlayerPointFocusS2CPacket.CLIENT_HANDLER = ClientCapturePointsState::applyFocusSlot;
        PlayerPointHudStateS2CPacket.CLIENT_HANDLER = ClientCapturePointsState::applyFocusedBoosted;

        event.registerAboveAll("capture_overview_hud", (gui, g, partialTick, w, h) -> {
            OverviewHudOverlay.render(g, w, h);
        });
    }
}