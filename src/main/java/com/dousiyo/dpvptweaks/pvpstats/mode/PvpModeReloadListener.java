package com.dousiyo.dpvptweaks.pvpstats.mode;

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

public final class PvpModeReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public PvpModeReloadListener() {
        super(GSON, DpvpTweaks.MODID + "/pvp_modes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, PvpModeDefinition> loaded = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
            PvpModeDefinition definition = PvpModeDefinitionLoader.fromDataPack(entry.getKey(), entry.getValue());
            if (definition != null) {
                loaded.put(definition.modeId(), definition);
            }
        }
        PvpModeManager.replace(loaded);
        DpvpTweaks.LOGGER.info("[{}] Loaded {} PvP mode definitions", DpvpTweaks.MOD_NAME, loaded.size());
    }
}
