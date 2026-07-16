package com.dousiyo.dpvptweaks.arsenal;

import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.FireMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

public final class ArsenalWeaponStage {
    public enum Type { TACZ_GUN, ITEM }

    private final Type type;
    private final ResourceLocation gunId;
    private final FireMode fireMode;
    private final Map<AttachmentType, ResourceLocation> attachments;
    private final int reserveMagazines;
    private final ItemStack itemTemplate;

    public ArsenalWeaponStage(ResourceLocation gunId, FireMode fireMode,
                              Map<AttachmentType, ResourceLocation> attachments, int reserveMagazines) {
        this.type = Type.TACZ_GUN;
        this.gunId = gunId;
        this.fireMode = fireMode;
        this.attachments = attachments == null ? Map.of() : Map.copyOf(attachments);
        this.reserveMagazines = Math.max(0, reserveMagazines);
        this.itemTemplate = ItemStack.EMPTY;
    }

    private ArsenalWeaponStage(ItemStack itemTemplate) {
        this.type = Type.ITEM;
        this.gunId = null;
        this.fireMode = null;
        this.attachments = Map.of();
        this.reserveMagazines = 0;
        this.itemTemplate = itemTemplate.copy();
    }

    public static ArsenalWeaponStage item(ItemStack itemTemplate) {
        if (itemTemplate == null || itemTemplate.isEmpty()) throw new IllegalArgumentException("itemTemplate is empty");
        return new ArsenalWeaponStage(itemTemplate);
    }

    public Type type() { return type; }
    public ResourceLocation gunId() { return gunId; }
    public FireMode fireMode() { return fireMode; }
    public Map<AttachmentType, ResourceLocation> attachments() { return attachments; }
    public int reserveMagazines() { return reserveMagazines; }
    public ItemStack itemTemplate() { return itemTemplate.copy(); }

    public EnumMap<AttachmentType, ResourceLocation> attachmentMap() {
        EnumMap<AttachmentType, ResourceLocation> result = new EnumMap<>(AttachmentType.class);
        result.putAll(attachments);
        return result;
    }
}
