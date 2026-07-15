package com.dousiyo.dpvptweaks.client.secretoperations;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.network.OpenPendingSecretDraftPacket;
import com.dousiyo.dpvptweaks.network.SecretOperationsNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SecretDraftKeyInput {
    private SecretDraftKeyInput() {}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        while (SecretDraftKeyMappings.OPEN_DRAFT.consumeClick())
            if (mc.screen == null && ClientShowdownState.participating() && ClientShowdownState.pendingDrafts() > 0)
                SecretOperationsNetwork.CHANNEL.sendToServer(new OpenPendingSecretDraftPacket());
    }
}
