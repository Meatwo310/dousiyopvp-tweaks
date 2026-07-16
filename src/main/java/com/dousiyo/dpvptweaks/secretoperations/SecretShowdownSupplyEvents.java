package com.dousiyo.dpvptweaks.secretoperations;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SecretShowdownSupplyEvents {
    private SecretShowdownSupplyEvents() {}

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !SecretShowdownSupplyManager.isMarked(event.getTarget())
                || SecretShowdownSupplyManager.isUnlocked(event.getTarget())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
        SecretShowdownSupplyManager.handleHold(player, event.getTarget().getId(), true);
    }
}
