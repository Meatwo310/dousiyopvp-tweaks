package com.dousiyo.dpvptweaks.client.secretoperations;

import com.dousiyo.airstrike.entity.AirdropCrateEntity;
import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.network.SecretOperationsNetwork;
import com.dousiyo.dpvptweaks.network.SupplyCrateHoldPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SupplyCrateClientInput {
    private static int heldEntityId = -1;

    private SupplyCrateClientInput() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        int targetId = -1;
        if (minecraft.player != null && minecraft.screen == null && ClientShowdownState.participating()
                && minecraft.options.keyUse.isDown() && minecraft.hitResult instanceof EntityHitResult hit
                && hit.getEntity() instanceof AirdropCrateEntity) targetId = hit.getEntity().getId();
        if (targetId >= 0) {
            if (heldEntityId >= 0 && heldEntityId != targetId)
                SecretOperationsNetwork.CHANNEL.sendToServer(new SupplyCrateHoldPacket(heldEntityId, false));
            heldEntityId = targetId;
            SecretOperationsNetwork.CHANNEL.sendToServer(new SupplyCrateHoldPacket(targetId, true));
        } else if (heldEntityId >= 0) {
            SecretOperationsNetwork.CHANNEL.sendToServer(new SupplyCrateHoldPacket(heldEntityId, false));
            heldEntityId = -1;
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        heldEntityId = -1;
        ClientSupplyCrateState.clear();
    }
}
