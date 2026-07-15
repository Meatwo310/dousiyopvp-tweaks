package com.dousiyo.dpvptweaks.client;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IntelDraftClientConnectionEvents {
    private IntelDraftClientConnectionEvents() {}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientIntelDraftState.update(false, java.util.Set.of());
    }
}
