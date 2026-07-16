package com.dousiyo.dpvptweaks.content;

import net.minecraft.server.level.ServerPlayer;

/** Extension point for a future rank system. */
@FunctionalInterface
public interface AnnouncementAudienceProvider {
    boolean matches(ServerPlayer player, ContentService.Announcement announcement);
}
