package com.dousiyo.dpvptweaks.server.function;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class FunctionPermissionService {
    private FunctionPermissionService() {
    }

    public static boolean isEnabled() {
        return FunctionPaletteServerConfig.ENABLED.get();
    }

    public static int requiredPermissionLevel() {
        return FunctionPaletteServerConfig.REQUIRED_PERMISSION_LEVEL.get();
    }

    public static boolean canUse(ServerPlayer player) {
        return player != null && isEnabled() && player.hasPermissions(requiredPermissionLevel());
    }

    public static boolean isNamespaceAllowed(ResourceLocation functionId) {
        List<? extends String> allowlist = FunctionPaletteServerConfig.ALLOWED_NAMESPACES.get();
        if (allowlist.isEmpty()) {
            return false;
        }

        String namespace = functionId.getNamespace();
        for (String entry : allowlist) {
            if (entry == null) {
                continue;
            }

            String normalized = entry.trim();
            if (normalized.isEmpty()) {
                continue;
            }

            if ("*".equals(normalized) || namespace.equals(normalized)) {
                return true;
            }
        }
        return false;
    }
}
