package com.dousiyo.dpvptweaks.server.function;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.functionpalette.FunctionPaletteAction;
import com.dousiyo.dpvptweaks.functionpalette.FunctionPaletteCategory;
import net.minecraft.server.MinecraftServer;

import java.util.*;

public final class FunctionPaletteManager {
    private static volatile Snapshot current = new Snapshot(0, List.of(), Map.of());

    private FunctionPaletteManager() {}

    public static synchronized ReloadResult reload(MinecraftServer server) {
        try {
            List<FunctionPaletteDefinitionLoader.ServerButton> buttons = FunctionPaletteDefinitionLoader.load();
            Map<String, FunctionPaletteDefinitionLoader.ServerButton> byId = new HashMap<>();
            for (var button : buttons) byId.put(button.id(), button);
            long revision = current.revision == Long.MAX_VALUE ? 1 : current.revision + 1;
            current = new Snapshot(revision, buttons, Map.copyOf(byId));
            DpvpTweaks.LOGGER.info("[{}] Loaded {} Dousiyo buttons (revision {})", DpvpTweaks.MOD_NAME, buttons.size(), revision);
            return new ReloadResult(true, buttons.size());
        } catch (FunctionPaletteDefinitionLoader.LoadException e) {
            DpvpTweaks.LOGGER.error("[{}] Dousiyo definition reload failed; keeping revision {}", DpvpTweaks.MOD_NAME, current.revision, e);
            return new ReloadResult(false, current.buttons.size());
        }
    }

    public static FunctionPaletteCategory clientData() {
        Snapshot snapshot = current;
        List<FunctionPaletteAction> actions = snapshot.buttons.stream()
                .map(b -> new FunctionPaletteAction(b.id(), b.name(), b.description(), b.icon(), b.confirmation()))
                .toList();
        return new FunctionPaletteCategory(snapshot.revision, actions);
    }

    public static long revision() { return current.revision; }
    public static FunctionPaletteDefinitionLoader.ServerButton find(String id) { return current.byId.get(id); }

    private record Snapshot(long revision, List<FunctionPaletteDefinitionLoader.ServerButton> buttons,
                            Map<String, FunctionPaletteDefinitionLoader.ServerButton> byId) {}
    public record ReloadResult(boolean success, int count) {}
}
