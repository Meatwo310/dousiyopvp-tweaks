package com.dousiyo.dpvptweaks.arsenal;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class ArsenalEquipmentService {
    private static final String TAG = "dpvptweaksArsenal";
    private ArsenalEquipmentService() {}

    public static boolean giveStage(ServerPlayer player, UUID matchId, int stageIndex, ArsenalWeaponStage stage) {
        ArsenalWeaponFactory.Result generated = ArsenalWeaponFactory.create(stage);
        if (!generated.valid()) {
            DpvpTweaks.LOGGER.error("Could not grant Arsenal stage {} to {}: {}", stageIndex + 1,
                    player.getGameProfile().getName(), generated.error());
            return false;
        }
        removeMatchEquipment(player);
        ItemStack gun = generated.gun().copy();
        mark(gun, matchId, stageIndex, "gun");
        int selectedSlot = player.getInventory().selected;
        ItemStack displaced = player.getInventory().getItem(selectedSlot);
        player.getInventory().setItem(selectedSlot, gun);
        if (!displaced.isEmpty() && !player.getInventory().add(displaced)) {
            player.getInventory().setItem(selectedSlot, displaced);
            DpvpTweaks.LOGGER.warn("Inventory full while switching Arsenal gun for {}", player.getGameProfile().getName());
            return false;
        }
        for (ItemStack rawAmmo : generated.ammo()) {
            ItemStack ammo = rawAmmo.copy();
            mark(ammo, matchId, stageIndex, "ammo");
            if (!player.getInventory().add(ammo)) {
                DpvpTweaks.LOGGER.warn("Inventory full while granting Arsenal ammo to {}", player.getGameProfile().getName());
                break;
            }
        }
        player.containerMenu.broadcastChanges();
        return true;
    }

    public static void removeMatchEquipment(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isMatchEquipment(stack)) player.getInventory().setItem(slot, ItemStack.EMPTY);
        }
        player.containerMenu.broadcastChanges();
    }

    public static boolean isMatchEquipment(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTag() && stack.getTag().contains(TAG);
    }

    private static void mark(ItemStack stack, UUID matchId, int stageIndex, String kind) {
        CompoundTag marker = new CompoundTag();
        marker.putUUID("MatchId", matchId); marker.putInt("Stage", stageIndex); marker.putString("Kind", kind);
        stack.getOrCreateTag().put(TAG, marker);
    }
}
