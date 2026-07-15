package com.dousiyo.dpvptweaks.arsenal;

import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.FireMode;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

public record ArsenalWeaponStage(
        ResourceLocation gunId,
        FireMode fireMode,
        Map<AttachmentType, ResourceLocation> attachments,
        int reserveMagazines
) {
    public ArsenalWeaponStage {
        attachments = attachments == null ? Map.of() : Map.copyOf(attachments);
        reserveMagazines = Math.max(0, reserveMagazines);
    }

    public EnumMap<AttachmentType, ResourceLocation> attachmentMap() {
        EnumMap<AttachmentType, ResourceLocation> result = new EnumMap<>(AttachmentType.class);
        result.putAll(attachments);
        return result;
    }
}
