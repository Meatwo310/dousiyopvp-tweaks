package com.dousiyo.dpvptweaks.arsenal;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ArsenalWeaponFactory {
    private ArsenalWeaponFactory() {}

    public static Result create(ArsenalWeaponStage stage) {
        if (stage == null) return Result.error("段階定義が空です");
        if (stage.type() == ArsenalWeaponStage.Type.ITEM) {
            ItemStack item = stage.itemTemplate();
            return item.isEmpty() ? Result.error("汎用アイテムが空です") : Result.ok(item, List.of(), 0);
        }
        if (stage.gunId() == null || stage.fireMode() == null) return Result.error("TaCZ銃定義が空です");
        var index = TimelessAPI.getCommonGunIndex(stage.gunId());
        if (index.isEmpty()) return Result.error("銃IDが存在しません: " + stage.gunId());
        var gunData = index.get().getGunData();
        if (!gunData.getFireModeSet().contains(stage.fireMode()))
            return Result.error("射撃モードを使用できません: " + stage.fireMode());

        GunItemBuilder builder = GunItemBuilder.create().setId(stage.gunId()).setCount(1)
                .setFireMode(stage.fireMode()).setAmmoCount(0);
        ItemStack gunStack = builder.build();
        if (gunStack.isEmpty()) return Result.error("銃を生成できません: " + stage.gunId());
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun == null) return Result.error("生成結果がTaCZ銃ではありません: " + stage.gunId());

        for (var entry : stage.attachments().entrySet()) {
            var attachmentIndex = TimelessAPI.getCommonAttachmentIndex(entry.getValue());
            if (attachmentIndex.isEmpty()) return Result.error("アタッチメントIDが存在しません: " + entry.getValue());
            if (attachmentIndex.get().getType() != entry.getKey())
                return Result.error("アタッチメント種別が一致しません: " + entry.getValue());
            ItemStack attachment = AttachmentItemBuilder.create().setId(entry.getValue()).setCount(1).build();
            if (attachment.isEmpty() || !gun.allowAttachment(gunStack, attachment))
                return Result.error("銃へ装着できません: " + entry.getValue());
            gun.installAttachment(gunStack, attachment);
            if (!entry.getValue().equals(gun.getAttachmentId(gunStack, entry.getKey())))
                return Result.error("アタッチメントを装着できません: " + entry.getValue());
        }

        int capacity;
        try {
            capacity = AttachmentDataUtils.getAmmoCountWithAttachment(gunStack, gunData);
        } catch (RuntimeException exception) {
            return Result.error("最終装弾数を取得できません: " + exception.getMessage());
        }
        if (capacity <= 0) return Result.error("最終装弾数が不正です: " + capacity);
        gun.setCurrentAmmoCount(gunStack, capacity);
        Bolt bolt = gunData.getBolt();
        gun.setBulletInBarrel(gunStack, bolt == Bolt.CLOSED_BOLT || bolt == Bolt.MANUAL_ACTION);

        long reserveRounds = (long) capacity * stage.reserveMagazines();
        if (reserveRounds > Integer.MAX_VALUE) return Result.error("予備弾数が大きすぎます");
        List<ItemStack> ammo = new ArrayList<>();
        while (reserveRounds > 0) {
            int amount = (int) Math.min(64, reserveRounds);
            ItemStack stack = AmmoItemBuilder.create().setId(gunData.getAmmoId()).setCount(amount).build();
            if (stack.isEmpty()) return Result.error("弾薬を生成できません: " + gunData.getAmmoId());
            ammo.add(stack);
            reserveRounds -= amount;
        }
        return Result.ok(gunStack, ammo, capacity);
    }

    public record Result(ItemStack gun, List<ItemStack> ammo, int capacity, String error) {
        static Result ok(ItemStack gun, List<ItemStack> ammo, int capacity) { return new Result(gun, List.copyOf(ammo), capacity, null); }
        static Result error(String error) { return new Result(ItemStack.EMPTY, List.of(), 0, error); }
        public boolean valid() { return error == null && !gun.isEmpty(); }
    }
}
