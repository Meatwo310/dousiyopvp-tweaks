package com.dousiyo.dpvptweaks.inteldraft;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import com.dousiyo.dpvptweaks.secretoperations.SecretOperationsManager;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IntelDraftLifecycleEvents {
    private IntelDraftLifecycleEvents() {}

    @SubscribeEvent public static void started(ServerStartedEvent event) { IntelDraftDefinitionLoader.reload(); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        IntelDraftManager.clearAll();
        SecretOperationsManager.clearAll();
    }
    @SubscribeEvent public static void datapackReloaded(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            IntelDraftDefinitionLoader.reload();
            IntelDraftManager.invalidateSessions(event.getPlayerList().getServer());
        }
    }
}
