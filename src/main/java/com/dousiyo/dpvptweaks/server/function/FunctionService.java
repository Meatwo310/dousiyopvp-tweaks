package com.dousiyo.dpvptweaks.server.function;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.functionpalette.FunctionPaletteCategory;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class FunctionService {
    private FunctionService() {
    }

    public static List<String> getFunctionIds(MinecraftServer server) {
        List<String> ids = new ArrayList<>();
        if (server == null) {
            return ids;
        }

        for (ResourceLocation functionId : server.getFunctions().getFunctionNames()) {
            if (FunctionPermissionService.isNamespaceAllowed(functionId)) {
                ids.add(functionId.toString());
            }
        }
        ids.sort(Comparator.naturalOrder());
        return ids;
    }

    public static PaletteData getPaletteData(MinecraftServer server) {
        List<String> allFunctionIds = getFunctionIds(server);
        FunctionPaletteDefinitionLoader.Definition definition = FunctionPaletteDefinitionLoader.load(allFunctionIds);
        return new PaletteData(definition.categories());
    }

    public static RunResult runFunction(ServerPlayer player, String rawFunctionId) {
        if (!FunctionPermissionService.isEnabled()) {
            return RunResult.failure(Component.translatable("message.dpvptweaks.function_palette.disabled"));
        }
        if (!FunctionPermissionService.canUse(player)) {
            return RunResult.failure(Component.translatable("message.dpvptweaks.function_palette.no_permission"));
        }

        String requestedId = rawFunctionId == null ? "" : rawFunctionId.trim();
        if (requestedId.isEmpty()) {
            return RunResult.failure(Component.translatable("message.dpvptweaks.function_palette.invalid_id", requestedId));
        }

        if (requestedId.startsWith("#")) {
            if (!FunctionPaletteServerConfig.ALLOW_TAGS.get()) {
                return RunResult.failure(Component.translatable("message.dpvptweaks.function_palette.tags_disabled"));
            }
            return RunResult.failure(Component.translatable("message.dpvptweaks.function_palette.tags_unsupported"));
        }

        ResourceLocation functionId = ResourceLocation.tryParse(requestedId);
        if (functionId == null) {
            return RunResult.failure(Component.translatable("message.dpvptweaks.function_palette.invalid_id", requestedId));
        }
        if (!FunctionPermissionService.isNamespaceAllowed(functionId)) {
            return RunResult.failure(Component.translatable("message.dpvptweaks.function_palette.namespace_denied", functionId.getNamespace()));
        }

        Optional<CommandFunction> function = player.server.getFunctions().get(functionId);
        if (function.isEmpty()) {
            return RunResult.failure(Component.translatable("message.dpvptweaks.function_palette.not_found", requestedId));
        }

        try {
            CommandSourceStack source = player.createCommandSourceStack()
                    .withPermission(FunctionPermissionService.requiredPermissionLevel())
                    .withSuppressedOutput();
            int commandsExecuted = player.server.getFunctions().execute(function.get(), source);
            return RunResult.success(Component.translatable(
                    "message.dpvptweaks.function_palette.run_success",
                    requestedId,
                    commandsExecuted
            ));
        } catch (Exception e) {
            DpvpTweaks.LOGGER.error("[{}] Failed to execute function '{}'", DpvpTweaks.MOD_NAME, requestedId, e);
            return RunResult.failure(Component.translatable("message.dpvptweaks.function_palette.run_failed", requestedId));
        }
    }

    public record RunResult(boolean success, Component message) {
        public static RunResult success(Component message) {
            return new RunResult(true, message);
        }

        public static RunResult failure(Component message) {
            return new RunResult(false, message);
        }
    }

    public record PaletteData(List<FunctionPaletteCategory> categories) {
    }
}
