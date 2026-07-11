package com.dousiyo.dpvptweaks.server.function;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraft.commands.CommandFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FunctionService {
    private static final Map<UUID, Long> LAST_EXECUTION = new ConcurrentHashMap<>();
    private static final long COOLDOWN_TICKS = 5;
    private FunctionService() {}

    public static RunResult runButton(ServerPlayer player, String buttonId, long revision) {
        if (!FunctionPermissionService.canUse(player)) return failure("message.dpvptweaks.function_palette.no_permission");
        if (revision != FunctionPaletteManager.revision()) return failure("message.dpvptweaks.function_palette.stale");
        var button = FunctionPaletteManager.find(buttonId);
        if (button == null) return failure("message.dpvptweaks.function_palette.unknown_button");

        Optional<CommandFunction> function = player.server.getFunctions().get(button.functionId());
        if (function.isEmpty()) return failure("message.dpvptweaks.function_palette.not_found");
        long now = player.serverLevel().getGameTime();
        Long last = LAST_EXECUTION.get(player.getUUID());
        if (last != null && now - last < COOLDOWN_TICKS) return failure("message.dpvptweaks.function_palette.cooldown");
        LAST_EXECUTION.put(player.getUUID(), now);
        try {
            player.server.getFunctions().execute(function.get(), player.createCommandSourceStack().withPermission(2));
            return new RunResult(true, Component.translatable("message.dpvptweaks.function_palette.run_success", button.name()));
        } catch (Exception e) {
            DpvpTweaks.LOGGER.error("[{}] Failed to execute button '{}' function '{}' for {}", DpvpTweaks.MOD_NAME,
                    button.id(), button.functionId(), player.getGameProfile().getName(), e);
            return failure("message.dpvptweaks.function_palette.run_failed");
        }
    }
    private static RunResult failure(String key) { return new RunResult(false, Component.translatable(key)); }
    public record RunResult(boolean success, Component message) {}
}
