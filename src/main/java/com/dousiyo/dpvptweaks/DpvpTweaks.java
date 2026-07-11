package com.dousiyo.dpvptweaks;

import com.mojang.logging.LogUtils;
import com.dousiyo.dpvptweaks.config.ClientConfig;
import com.dousiyo.dpvptweaks.config.ServerConfig;
import com.dousiyo.dpvptweaks.entity.ModEntities;
import com.dousiyo.dpvptweaks.effect.ModEffects;
import com.dousiyo.dpvptweaks.item.ModCreativeModeTabs;
import com.dousiyo.dpvptweaks.item.ModItems;
import com.dousiyo.dpvptweaks.network.CaptureNetwork;
import com.dousiyo.dpvptweaks.network.DousiyoServerMainReceiverNetwork;
import com.dousiyo.dpvptweaks.network.FunctionPaletteNetwork;
import com.dousiyo.dpvptweaks.network.LoadoutGuiNetwork;
import com.dousiyo.dpvptweaks.pvpstats.network.PvpStatsNetwork;
import com.dousiyo.dpvptweaks.server.function.FunctionPaletteServerConfig;
import com.dousiyo.dpvptweaks.timer.config.TimerClientConfig;
import com.dousiyo.dpvptweaks.timer.network.ModNetwork;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.forgespi.language.IModInfo;
import org.slf4j.Logger;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Mod(DpvpTweaks.MODID)
public class DpvpTweaks {
    public static final String MODID = "dpvptweaks";
    public static final String MOD_NAME = "Dousiyo Client";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final String DOUSEIYO_SERVER_MODID = "dousiyoserver";

    public static final Set<String> ALLOWED_MODS = Set.of(
            "appleskin", "architectury", "armourers_workshop", "ashvehicle", "chat_heads", "chloride", "cloth_config", "collective", "commongroovylibrary", "configured", "controlling", "curios", "damage_indicator", "dpvptweaks", "dragonrise_reforge", "dummmmmmy", "eatinganimation", "embeddium", "embeddium_extra", "endlessammo", "entityculling", "extremesoundmuffler", "ferritecore", "forge", "geckolib", "gml", "guccivuitton", "immediatelyfast", "javd", "journeymap", "journeymapteams", "jpy", "kotlinforforge", "kubejs", "lightmanscurrency", "lrarmor", "lrtactical", "maxstuff", "meatwo310", "minecraft", "mixinsquared", "modernfix", "modernui", "moonlight", "mousetweaks", "notenoughanimations", "notenoughcrashes", "oculus", "packetfixer", "parcool", "parcool_compat_addon", "playeranimator", "presencefootsteps", "puzzlesaccessapi", "puzzleslib", "rhino", "roughtweaks", "rubidium", "seamless_loading_screen", "searchables", "shouldersurfing", "softdeepslate", "sound_physics_remastered", "spotmod", "starterkit", "stylisheffects", "superbwarfare", "tacz", "tacz_presence", "taczadditions", "taczlabs", "tacztweaks", "takkit", "timestamp_chat", "tp_shooting", "transition", "trender", "trenzalore", "tsukichat", "untranslateditems", "waterdripsound", "xlpackets", "yet_another_config_lib_v3"

    );

    public static final Set<String> DEVS = Set.of(

    );

    public DpvpTweaks(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(com.dousiyo.dpvptweaks.client.ClientBootstrap::onFMLClientSetup);
        } else {
            registerFallbackDousiyoServerMainReceiver();
        }

        ModItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModEntities.register(modEventBus);
        ModEffects.register(modEventBus);
        CaptureNetwork.register();
        LoadoutGuiNetwork.register();
        FunctionPaletteNetwork.register();
        PvpStatsNetwork.register();
        ModNetwork.register();

        context.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        context.registerConfig(ModConfig.Type.SERVER, FunctionPaletteServerConfig.SPEC, MODID + "-function_palette-server.toml");
        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, TimerClientConfig.SPEC, MODID + "-timer-client.toml");
    }

    private static void registerFallbackDousiyoServerMainReceiver() {
        if (ModList.get().isLoaded(DOUSEIYO_SERVER_MODID)) {
            LOGGER.info("[{}] Skipping fallback receiver for dousiyoserver:main because mod '{}' is loaded", MOD_NAME, DOUSEIYO_SERVER_MODID);
            return;
        }
        DousiyoServerMainReceiverNetwork.register();
    }

    public static void runClientStartupChecks(String username) {
        boolean bypass = DEVS.contains(username);
        checkKJS(bypass);
        checkMods(bypass);
    }

    private static void checkKJS(boolean bypass) {
        File scriptsDir = new File("./kubejs/client_scripts");
        if (!scriptsDir.exists()) return;

        File[] files = scriptsDir.listFiles();
        if (files == null) return;
        if (files.length == 0) return;

        LOGGER.error("Following scripts are not allowed: {}", (Object) files);
        if (bypass) return;

        LOGGER.warn("[{}] Unauthorized client scripts were detected, but crash enforcement is disabled", MOD_NAME);
    }

    private static void checkMods(boolean bypass) {
        var mods = ModList.get().getMods().stream()
                .map(IModInfo::getModId)
                .collect(Collectors.toCollection(HashSet::new));

        LOGGER.info("Current mods: {}", modsToString(mods));

        mods.removeAll(ALLOWED_MODS);
        if (mods.isEmpty()) return;

        LOGGER.error("Following mods are not allowed: {}", modsToString(mods));
        if (bypass) return;

        LOGGER.warn("[{}] Unauthorized mods were detected, but crash enforcement is disabled", MOD_NAME);
    }

    private static String modsToString(Set<String> mods) {
        return mods.stream()
                .sorted()
                .map(mod -> "\"" + mod + "\"")
                .toList()
                .toString();
    }
}
