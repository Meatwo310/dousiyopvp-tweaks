package com.dousiyo.dpvptweaks.loadout;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
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

    private static volatile Map<ResourceLocation, LoadoutSetDefinition> sets = Map.of();

    private LoadoutDataManager() {
    }

    public static void replaceLoadouts(Map<ResourceLocation, LoadoutDefinition> loaded) {
        // Individual datapack loadout definitions are intentionally ignored.
    }

    public static void replaceSets(Map<ResourceLocation, LoadoutSetDefinition> loaded) {
        sets = orderedCopy(loaded);
        DpvpTweaks.LOGGER.info("[{}] Loaded {} datapack loadout set definition(s)", DpvpTweaks.MOD_NAME, sets.size());
    }

    public static List<ResourceLocation> setIds() {
        return sets.keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList();
    }

    public static List<ResourceLocation> loadoutIds() {
        return sets.values().stream().flatMap(set -> set.loadouts().stream())
                .map(entry -> ResourceLocation.fromNamespaceAndPath(DpvpTweaks.MODID, entry.id()))
                .distinct().sorted(Comparator.comparing(ResourceLocation::toString)).toList();
    }

    public static Collection<LoadoutSetDefinition> allSets() {
        return sets.values();
    }

    public static LoadoutSetDefinition getSet(ResourceLocation id) {
        return sets.get(id);
    }

    public static ResourceLocation findSetIdByPath(String path) {
        if (path == null || path.isBlank()) return null;
        return sets.keySet().stream()
                .filter(id -> id.getPath().equals(path))
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .findFirst().orElse(null);
    }

    public static LoadoutDefinition getLoadout(ResourceLocation id) {
        if (id == null) return null;
        for (LoadoutSetDefinition set : sets.values()) {
            for (LoadoutSetDefinition.Entry entry : set.loadouts()) {
                if (entry.id().equals(id.getPath())) return SavedLoadoutPreviewLoader.load(entry);
            }
        }
        return null;
    }

    public static List<LoadoutDefinition> getLoadoutsForSet(ResourceLocation setId, ServerPlayer player) {
        return getAvailableLoadoutsForSet(setId, player).stream().map(AvailableLoadout::preview).toList();
    }

    public static List<AvailableLoadout> getAvailableLoadoutsForSet(ResourceLocation setId, ServerPlayer player) {
        LoadoutSetDefinition set = sets.get(setId);
        if (set == null) {
            return legacyFallback(setId).stream().map(loadout -> new AvailableLoadout(loadout, null)).toList();
        }

        List<AvailableLoadout> result = new ArrayList<>();
        for (LoadoutSetDefinition.Entry entry : set.loadouts()) {
            if (entry.isRandom()) {
                if (RandomLoadoutProfileManager.availabilityError(entry.random()) != null) continue;
                List<ItemStack> hoppers = new ArrayList<>();
                for (int i = 0; i < entry.random().weaponCount(); i++) hoppers.add(new ItemStack(ModItems.RANDOM_LOADOUT_ICON.get()));
                LoadoutDefinition preview = new LoadoutDefinition(entry.id(), entry.displayName(), "RANDOM",
                        hoppers, entry.description(), List.of(), entry.afterApply());
                result.add(new AvailableLoadout(preview, entry));
                continue;
            }
            LoadoutDefinition loadout = SavedLoadoutPreviewLoader.load(entry);
            if (loadout != null) result.add(new AvailableLoadout(loadout, entry));
        }
        return List.copyOf(result);
    }

    public static LoadoutDefinition findLoadout(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }

        String wanted = rawId.trim();
        for (LoadoutSetDefinition set : sets.values()) for (LoadoutSetDefinition.Entry entry : set.loadouts())
            if (entry.id().equals(wanted)) return SavedLoadoutPreviewLoader.load(entry);
        return null;
    }

    public static List<String> validate(MinecraftServer server) {
        List<String> issues = new ArrayList<>();
        if (sets.isEmpty()) {
            issues.add("No datapack loadout sets were loaded from data/*/dpvptweaks/loadout_sets.");
        }

        for (Map.Entry<ResourceLocation, LoadoutSetDefinition> entry : sets.entrySet()) {
            LoadoutSetDefinition set = entry.getValue();
            if (set.loadouts().isEmpty()) {
                issues.add(entry.getKey() + " has no loadouts.");
            }
            for (LoadoutSetDefinition.Entry loadoutEntry : set.loadouts()) {
                if (loadoutEntry.isRandom()) {
                    String error = RandomLoadoutProfileManager.availabilityError(loadoutEntry.random());
                    if (error != null) issues.add(entry.getKey() + " random entry " + loadoutEntry.id() + ": " + error);
                    if (loadoutEntry.afterApply() != null && server != null && server.getFunctions().get(loadoutEntry.afterApply()).isEmpty())
                        issues.add(entry.getKey() + " references missing after_apply function " + loadoutEntry.afterApply() + ".");
                    continue;
                }
                LoadoutDefinition loadout = SavedLoadoutPreviewLoader.load(loadoutEntry);
                if (loadout == null) issues.add(entry.getKey() + " references missing saved loadout " + loadoutEntry.id() + ".");
                if (loadoutEntry.afterApply() != null && server != null && server.getFunctions().get(loadoutEntry.afterApply()).isEmpty())
                    issues.add(entry.getKey() + " references missing after_apply function " + loadoutEntry.afterApply() + ".");
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

    private static List<LoadoutDefinition> legacyFallback(ResourceLocation setId) {
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

    public record AvailableLoadout(LoadoutDefinition preview, LoadoutSetDefinition.Entry entry) {
        public boolean isRandom() {
            return entry != null && entry.isRandom();
        }
    }
}
