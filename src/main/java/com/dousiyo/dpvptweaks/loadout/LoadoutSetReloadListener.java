package com.dousiyo.dpvptweaks.loadout;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LoadoutSetReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    public LoadoutSetReloadListener() {
        super(GSON, DpvpTweaks.MODID + "/loadout_sets");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, LoadoutSetDefinition> loaded = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
            LoadoutSetDefinition definition = LoadoutSetDefinitionLoader.fromDataPack(entry.getKey(), entry.getValue(), "datapack loadout set");
            if (definition != null) {
                loaded.put(entry.getKey(), definition);
            }
        }
        LoadoutDataManager.replaceSets(loaded);
    }
}
