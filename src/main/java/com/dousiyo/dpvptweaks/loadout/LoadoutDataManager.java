package com.dousiyo.dpvptweaks.loadout;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.Team;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LoadoutDataManager {
    public static final ResourceLocation DEFAULT_LOADOUT_SET = ResourceLocation.fromNamespaceAndPath(DpvpTweaks.MODID, "tb");
    public static final ResourceLocation DEFAULT_MINI_LOADOUT_SET = ResourceLocation.fromNamespaceAndPath(DpvpTweaks.MODID, "tb_mini");

    private static volatile Map<ResourceLocation, LoadoutDefinition> loadouts = Map.of();
    private static volatile Map<ResourceLocation, LoadoutSetDefinition> sets = Map.of();

    private LoadoutDataManager() {
    }

    public static void replaceLoadouts(Map<ResourceLocation, LoadoutDefinition> loaded) {
        loadouts = orderedCopy(loaded);
        DpvpTweaks.LOGGER.info("[{}] Loaded {} datapack loadout definition(s)", DpvpTweaks.MOD_NAME, loadouts.size());
    }

    public static void replaceSets(Map<ResourceLocation, LoadoutSetDefinition> loaded) {
        sets = orderedCopy(loaded);
        DpvpTweaks.LOGGER.info("[{}] Loaded {} datapack loadout set definition(s)", DpvpTweaks.MOD_NAME, sets.size());
    }

    public static List<ResourceLocation> setIds() {
        return sets.keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList();
    }

    public static List<ResourceLocation> loadoutIds() {
        return loadouts.keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList();
    }

    public static Collection<LoadoutSetDefinition> allSets() {
        return sets.values();
    }

    public static LoadoutSetDefinition getSet(ResourceLocation id) {
        return sets.get(id);
    }

    public static LoadoutDefinition getLoadout(ResourceLocation id) {
        return loadouts.get(id);
    }

    public static List<LoadoutDefinition> getLoadoutsForSet(ResourceLocation setId, ServerPlayer player) {
        LoadoutSetDefinition set = sets.get(setId);
        if (set == null) {
            return legacyFallback(setId);
        }

        List<LoadoutDefinition> result = new ArrayList<>();
        for (ResourceLocation loadoutId : set.loadouts()) {
            LoadoutDefinition loadout = loadouts.get(loadoutId);
            if (loadout != null && matchesPlayer(loadout, player)) {
                result.add(loadout);
            }
        }
        return List.copyOf(result);
    }

    public static LoadoutDefinition findLoadout(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }

        ResourceLocation id = ResourceLocation.tryParse(rawId.trim());
        if (id != null) {
            LoadoutDefinition byLocation = loadouts.get(id);
            if (byLocation != null) {
                return byLocation;
            }
        }

        for (LoadoutDefinition loadout : loadouts.values()) {
            if (loadout.id().equals(rawId.trim())) {
                return loadout;
            }
        }
        return null;
    }

    public static List<String> validate(MinecraftServer server) {
        List<String> issues = new ArrayList<>();
        if (loadouts.isEmpty()) {
            issues.add("No datapack loadouts were loaded from data/*/dpvptweaks/loadouts.");
        }
        if (sets.isEmpty()) {
            issues.add("No datapack loadout sets were loaded from data/*/dpvptweaks/loadout_sets.");
        }

        for (Map.Entry<ResourceLocation, LoadoutDefinition> entry : loadouts.entrySet()) {
            LoadoutDefinition loadout = entry.getValue();
            if (loadout.name().isBlank()) {
                issues.add(entry.getKey() + " has an empty display.name.");
            }
            if (loadout.applyFunction() == null) {
                issues.add(entry.getKey() + " has no apply.function.");
            } else if (server != null && server.getFunctions().get(loadout.applyFunction()).isEmpty()) {
                issues.add(entry.getKey() + " references missing function " + loadout.applyFunction() + ".");
            }
            for (int i = 0; i < loadout.gunStacks().size(); i++) {
                if (loadout.gunStacks().get(i).is(Items.BARRIER)) {
                    issues.add(entry.getKey() + " preview item #" + (i + 1) + " resolved to minecraft:barrier.");
                }
            }
        }

        for (Map.Entry<ResourceLocation, LoadoutSetDefinition> entry : sets.entrySet()) {
            LoadoutSetDefinition set = entry.getValue();
            if (set.loadouts().isEmpty()) {
                issues.add(entry.getKey() + " has no loadouts.");
            }
            for (ResourceLocation loadoutId : set.loadouts()) {
                if (!loadouts.containsKey(loadoutId)) {
                    issues.add(entry.getKey() + " references missing loadout " + loadoutId + ".");
                }
            }
        }
        return List.copyOf(issues);
    }

    public static ResourceLocation parseSetId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (!trimmed.contains(":")) {
            trimmed = DpvpTweaks.MODID + ":" + trimmed;
        }
        return ResourceLocation.tryParse(trimmed);
    }

    private static boolean matchesPlayer(LoadoutDefinition loadout, ServerPlayer player) {
        if (loadout.teams().isEmpty() || player == null) {
            return true;
        }
        Team team = player.getTeam();
        String teamName = team == null ? "" : team.getName();
        return loadout.teams().stream().anyMatch(teamName::equals);
    }

    private static List<LoadoutDefinition> legacyFallback(ResourceLocation setId) {
        if (DEFAULT_LOADOUT_SET.equals(setId)) {
            return LoadoutDefinitionLoader.load("loadout_gui.json", "legacy loadout fallback");
        }
        if (DEFAULT_MINI_LOADOUT_SET.equals(setId)) {
            return LoadoutDefinitionLoader.load("mini_loadout_gui.json", "legacy mini loadout fallback");
        }
        return List.of();
    }

    private static <T> Map<ResourceLocation, T> orderedCopy(Map<ResourceLocation, T> loaded) {
        if (loaded == null || loaded.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<ResourceLocation, T> ordered = new LinkedHashMap<>();
        loaded.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));
        return Collections.unmodifiableMap(ordered);
    }
}
