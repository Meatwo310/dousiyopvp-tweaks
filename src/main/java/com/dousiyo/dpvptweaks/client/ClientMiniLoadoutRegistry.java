package com.dousiyo.dpvptweaks.client;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
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
        register("tb_mini_1", "偵察兵", "AR PI", "軽量で扱いやすい前線向け構成。",
                id("maxstuff", "sr16"), id("cib", "js9"), id("roughtweaks", "medkit"));
        register("tb_mini_2", "狙撃兵", "SR PI", "後方から援護しやすい安定構成。",
                id("tacz", "ai_awp"), id("tacz", "uzi"), id("roughtweaks", "medkit"));
        register("tb_mini_3", "遊撃兵", "LMG PI", "弾幕を意識した制圧寄りの構成。",
                id("maxstuff", "ump9"), id("maxstuff", "m870t"), id("roughtweaks", "medkit"));
        register("tb_mini_4", "強襲兵", "UTIL ME", "ユーティリティ重視の柔軟な構成。",
                id("maxstuff", "ak74"), id("tacz", "m1911"), id("roughtweaks", "medkit"));
        register("tb_mini_5", "遊撃兵", "SG PI", "近距離の迎撃に強い防衛構成。",
                id("maxstuff", "aug_a2"), id("cib", "686"), id("roughtweaks", "medkit"));
        register("tb_mini_6", "工兵", "DMR PI", "索敵しながら中距離を維持する構成。",
                id("tacz", "qbz_95"), id("cib", "r8"), id("roughtweaks", "medkit"));
//        register("tb_mini_7", "ミニ突破", "AR ME", "短時間で押し込む突破向け構成。",
//                id("minecraft", "crossbow"), id("minecraft", "iron_axe"), id("roughtweaks", "medkit"));
//        register("tb_mini_8", "ミニ汎用", "AR PI UTIL", "状況を選ばず使える汎用構成。",
//                id("minecraft", "bow"), id("minecraft", "iron_sword"), id("roughtweaks", "medkit"));
    }

    private ClientMiniLoadoutRegistry() {
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    private static void register(String id, String name, String weapons, String desc, ResourceLocation... gunIds) {
        List<ItemStack> list = new ArrayList<>();
        for (ResourceLocation gunId : gunIds) {
            list.add(createGunStack(gunId));
        }
        LOADOUTS.put(id, new ClientLoadout(id, name, weapons, list, desc));
    }

    private static ItemStack createGunStack(ResourceLocation gunId) {
        String ns = gunId.getNamespace();
        if ("tacz".equals(ns) || "maxstuff".equals(ns) || "elitex".equals(ns) || "cib".equals(ns)) {
            ResourceLocation baseItemId = ResourceLocation.fromNamespaceAndPath("tacz", "modern_kinetic_gun");
            Item base = ForgeRegistries.ITEMS.getValue(baseItemId);
            if (base == null) {
                DpvpTweaks.LOGGER.warn("[ClientMiniLoadoutRegistry] missing base gun item: {}", baseItemId);
                return new ItemStack(Items.BARRIER);
            }
            ItemStack stack = new ItemStack(base);
            stack.getOrCreateTag().putString("GunId", gunId.toString());
            return stack;
        }

        Item direct = ForgeRegistries.ITEMS.getValue(gunId);
        if (direct == null) {
            DpvpTweaks.LOGGER.warn("[ClientMiniLoadoutRegistry] missing item: {}", gunId);
            return new ItemStack(Items.BARRIER);
        }
        return new ItemStack(direct);
    }

    public static Map<String, ClientLoadout> all() {
        return LOADOUTS;
    }

    public static ClientLoadout get(String id) {
        return LOADOUTS.get(id);
    }
}