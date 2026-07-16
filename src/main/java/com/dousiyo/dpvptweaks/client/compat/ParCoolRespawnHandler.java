package com.dousiyo.dpvptweaks.client.compat;

import com.alrex.parcool.api.Stamina;
import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ParCoolRespawnHandler {
    private static final String PARCOOL_MOD_ID = "parcool";

    private ParCoolRespawnHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerClone(ClientPlayerNetworkEvent.Clone event) {
        if (!ModList.get().isLoaded(PARCOOL_MOD_ID) || !event.getOldPlayer().isDeadOrDying()) {
            return;
        }

        Stamina stamina = Stamina.get(event.getNewPlayer());
        if (stamina != null) {
            // recover() also clears ParCool's exhausted state when stamina reaches its maximum.
            stamina.recover(stamina.getMaxValue());
        }
    }
}
