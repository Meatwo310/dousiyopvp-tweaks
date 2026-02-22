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

public final class ClientLoadoutRegistry {
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
        register("tb_1", "偵察兵", "AR SMG RPG", "中距離から遠距離まで安定して戦える、バランス重視のロードアウト。",
                id("maxstuff", "sr16"), id("cib", "js9"), id("superbwarfare", "igla_9k38"));
        register("tb_2", "狙撃兵", "SR PI GL", "遠距離で圧をかけつつ、近距離の保険も確保した構成。",
                id("tacz", "ai_awp"), id("tacz", "uzi"), id("superbwarfare", "m18_smoke_grenade"));
        register("tb_3", "遊撃兵", "SMG SG ME", "近距離戦へ素早く切り込むための突入特化セット。",
                id("tacz", "ump45"), id("maxstuff", "m870t"), id("superbwarfare", "lunge_mine"));
        register("tb_4", "強襲兵", "AR PI RPG", "前線で制圧しやすい、エリア拒否向けの構成。",
                id("maxstuff", "ak74"), id("tacz", "m1911"), id("superbwarfare", "rgo_grenade"));
        register("tb_5", "遊撃兵", "AR SG ME", "機動力を保ちつつ、補助と継戦能力を両立した支援型。",
                id("maxstuff", "aug_a2"), id("cib", "686"), id("superbwarfare", "repair_tool"));
        register("tb_6", "工兵", "AR RPG HC", "爆発物を絡めて高い制圧力を出せる攻撃的セット。",
                id("tacz", "qbz_95"), id("tacz", "rpg7"), id("cib", "r8"));
        register("tb_7", "援護兵", "LMG PI RPG", "対装甲を重視した、ユーティリティ寄りの編成。",
                id("tacz", "m249"), id("maxstuff", "m17"), id("superbwarfare", "javelin"));
        register("tb_8", "支援射撃兵", "DMR SMG PI", "中遠距離で安定して圧をかけ続けられる構成。",
                id("maxstuff", "dragonuv_svd"), id("maxstuff", "udp9"), id("tacz", "m320"));
        register("tb_default", "Default", "", "選択情報がない場合に使用される予備ロードアウト。");
    }

    private ClientLoadoutRegistry() {
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
                DpvpTweaks.LOGGER.warn("[ClientLoadoutRegistry] missing base gun item: {}", baseItemId);
                return new ItemStack(Items.BARRIER);
            }
            ItemStack stack = new ItemStack(base);
            stack.getOrCreateTag().putString("GunId", gunId.toString());
            return stack;
        }

        Item direct = ForgeRegistries.ITEMS.getValue(gunId);
        if (direct == null) {
            DpvpTweaks.LOGGER.warn("[ClientLoadoutRegistry] missing item: {}", gunId);
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


