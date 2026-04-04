package com.dousiyo.dpvptweaks.gui;

import com.dousiyo.dpvptweaks.client.ClientMiniLoadoutRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MiniLoadoutScreen extends LoadoutScreen {
    public MiniLoadoutScreen() {
        super(buildPreviews(), buildPacketIds());
    }

    private static List<LoadoutPreview> buildPreviews() {
        List<LoadoutPreview> previews = new ArrayList<>();
        int previewId = 1;
        for (ClientMiniLoadoutRegistry.ClientLoadout loadout : ClientMiniLoadoutRegistry.all().values()) {
            List<ItemStack> weapons = new ArrayList<>(3);
            weapons.add(loadout.gunStacks.size() > 0 ? loadout.gunStacks.get(0) : ItemStack.EMPTY);
            weapons.add(loadout.gunStacks.size() > 1 ? loadout.gunStacks.get(1) : ItemStack.EMPTY);
            weapons.add(loadout.gunStacks.size() > 2 ? loadout.gunStacks.get(2) : ItemStack.EMPTY);
            previews.add(new LoadoutPreview(
                    previewId,
                    Component.literal(loadout.name),
                    weapons,
                    Component.literal(loadout.weapons),
                    Component.literal(loadout.description)
            ));
            previewId++;
        }
        return previews;
    }

    private static Map<Integer, String> buildPacketIds() {
        Map<Integer, String> mapping = new LinkedHashMap<>();
        int previewId = 1;
        for (ClientMiniLoadoutRegistry.ClientLoadout loadout : ClientMiniLoadoutRegistry.all().values()) {
            mapping.put(previewId, loadout.id);
            previewId++;
        }
        return mapping;
    }
}

