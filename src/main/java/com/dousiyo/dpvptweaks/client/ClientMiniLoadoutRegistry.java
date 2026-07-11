package com.dousiyo.dpvptweaks.client;

import com.dousiyo.dpvptweaks.loadout.LoadoutDefinition;
import com.dousiyo.dpvptweaks.loadout.LoadoutDefinitionLoader;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ClientMiniLoadoutRegistry {
    public static final class ClientLoadout {
        public final String id;
        public final String name;
        public final String weapons;
        public final List<ItemStack> gunStacks;
        public final String description;

        public ClientLoadout(String id, String name, String weapons, List<ItemStack> gunStacks, String description) {
            this.id = id;
            this.name = name;
            this.weapons = weapons;
            this.gunStacks = gunStacks;
            this.description = description;
        }
    }

    private static final Map<String, ClientLoadout> LOADOUTS = new LinkedHashMap<>();

    static {
        for (LoadoutDefinition loadout : LoadoutDefinitionLoader.load("mini_loadout_gui.json", "ClientMiniLoadoutRegistry")) {
            LOADOUTS.put(loadout.id(), new ClientLoadout(loadout.id(), loadout.name(), loadout.weapons(), loadout.gunStacks(), loadout.description()));
        }
    }

    private ClientMiniLoadoutRegistry() {
    }

    public static Map<String, ClientLoadout> all() {
        return LOADOUTS;
    }

    public static ClientLoadout get(String id) {
        return LOADOUTS.get(id);
    }
}
