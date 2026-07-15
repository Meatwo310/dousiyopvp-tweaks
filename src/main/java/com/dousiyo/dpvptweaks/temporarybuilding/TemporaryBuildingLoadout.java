package com.dousiyo.dpvptweaks.temporarybuilding;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class TemporaryBuildingLoadout {
    public static final int TOOL_SLOT = 6;
    public static final int STONE_SLOT = 7;
    public static final int WOOD_SLOT = 8;

    private TemporaryBuildingLoadout() {}

    public static void grantInitial(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        inventory.setItem(TOOL_SLOT, tool(false));
        inventory.setItem(STONE_SLOT, new ItemStack(ModTemporaryBlocks.STONE.get(), 20));
        inventory.setItem(WOOD_SLOT, new ItemStack(ModTemporaryBlocks.WOOD.get(), 20));
        player.containerMenu.broadcastChanges();
    }

    public static void grantExtraMaterials(ServerPlayer player) {
        add(player, new ItemStack(ModTemporaryBlocks.STONE.get(), 40), STONE_SLOT);
        add(player, new ItemStack(ModTemporaryBlocks.WOOD.get(), 40), WOOD_SLOT);
        player.containerMenu.broadcastChanges();
    }

    public static void upgradeTool(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            if (inventory.getItem(slot).is(Items.IRON_PICKAXE)) {
                inventory.setItem(slot, tool(true));
                player.containerMenu.broadcastChanges();
                return;
            }
        }
        inventory.setItem(TOOL_SLOT, tool(true));
        player.containerMenu.broadcastChanges();
    }

    private static ItemStack tool(boolean upgraded) {
        ItemStack stack = new ItemStack(upgraded ? Items.NETHERITE_PICKAXE : Items.IRON_PICKAXE);
        stack.getOrCreateTag().putBoolean("Unbreakable", true);
        ListTag canDestroy = new ListTag();
        canDestroy.add(StringTag.valueOf("dpvptweaks:wood"));
        canDestroy.add(StringTag.valueOf("dpvptweaks:stone"));
        stack.getOrCreateTag().put("CanDestroy", canDestroy);
        return stack;
    }

    static ItemStack baseToolForTest() {
        return tool(false);
    }

    private static void add(ServerPlayer player, ItemStack stack, int preferredSlot) {
        ItemStack preferred = player.getInventory().getItem(preferredSlot);
        if (ItemStack.isSameItemSameTags(preferred, stack) && preferred.getCount() < preferred.getMaxStackSize()) {
            int moved = Math.min(stack.getCount(), preferred.getMaxStackSize() - preferred.getCount());
            preferred.grow(moved);
            stack.shrink(moved);
        }
        if (!stack.isEmpty() && !player.getInventory().add(stack)) player.drop(stack, false);
    }
}
