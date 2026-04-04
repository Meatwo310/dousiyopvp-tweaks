package com.dousiyo.dpvptweaks.pvpstats.service;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class ScoreHolderResolveService {
    private static final Set<String> WARNED_HOLDERS = new HashSet<>();

    private ScoreHolderResolveService() {
    }

    public static Optional<UUID> resolvePlayerUuid(MinecraftServer server, String scoreHolder) {
        if (scoreHolder == null || scoreHolder.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(UUID.fromString(scoreHolder.trim()));
        } catch (IllegalArgumentException ignored) {
        }

        ServerPlayer onlinePlayer = server.getPlayerList().getPlayerByName(scoreHolder);
        if (onlinePlayer != null) {
            return Optional.of(onlinePlayer.getUUID());
        }

        Optional<GameProfile> cachedProfile = server.getProfileCache().get(scoreHolder);
        if (cachedProfile.isPresent() && cachedProfile.get().getId() != null) {
            return Optional.of(cachedProfile.get().getId());
        }

        if (WARNED_HOLDERS.add(scoreHolder)) {
            DpvpTweaks.LOGGER.warn("[{}] Could not resolve scoreboard holder '{}' to a player UUID", DpvpTweaks.MOD_NAME, scoreHolder);
        }
        return Optional.empty();
    }

    public static String resolveLastKnownName(MinecraftServer server, UUID uuid, String fallbackName) {
        if (uuid == null) {
            return fallbackName == null ? "" : fallbackName;
        }

        ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getGameProfile().getName();
        }

        Optional<GameProfile> cachedProfile = server.getProfileCache().get(uuid);
        if (cachedProfile.isPresent() && cachedProfile.get().getName() != null) {
            return cachedProfile.get().getName();
        }

        return fallbackName == null ? "" : fallbackName;
    }
}
