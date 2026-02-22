package com.dousiyo.dpvptweaks;

import com.mojang.logging.LogUtils;
import com.dousiyo.dpvptweaks.config.ClientConfig;
import com.dousiyo.dpvptweaks.config.CommonConfig;
import com.dousiyo.dpvptweaks.config.ServerConfig;
import com.dousiyo.dpvptweaks.entity.ModEntities;
import com.dousiyo.dpvptweaks.item.ModCreativeModeTabs;
import com.dousiyo.dpvptweaks.item.ModItems;
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

    public static final Set<String> ALLOWED_MODS = Set.of(
            "appleskin", "architectury", "armourers_workshop", "ashvehicle", "chat_heads", "chloride", "cloth_config", "collective", "commongroovylibrary", "configured", "controlling", "curios", "damage_indicator", "dpvptweaks", "dragonrise_reforge", "dummmmmmy", "eatinganimation", "embeddium", "embeddium_extra", "endlessammo", "entityculling", "extremesoundmuffler", "ferritecore", "forge", "geckolib", "gml", "guccivuitton", "immediatelyfast", "javd", "journeymap", "journeymapteams", "jpy", "kotlinforforge", "kubejs", "lightmanscurrency", "lrarmor", "lrtactical", "maxstuff", "meatwo310", "minecraft", "mixinsquared", "modernfix", "modernui", "moonlight", "mousetweaks", "notenoughanimations", "notenoughcrashes", "oculus", "packetfixer", "parcool", "parcool_compat_addon", "playeranimator", "presencefootsteps", "puzzlesaccessapi", "puzzleslib", "rhino", "roughtweaks", "rubidium", "seamless_loading_screen", "searchables", "shouldersurfing", "softdeepslate", "sound_physics_remastered", "spotmod", "starterkit", "stylisheffects", "superbwarfare", "tacz", "tacz_presence", "taczadditions", "taczlabs", "tacztweaks", "takkit", "timestamp_chat", "tp_shooting", "transition", "trender", "trenzalore", "tsukichat", "untranslateditems", "waterdripsound", "xlpackets", "yet_another_config_lib_v3"

    );

    public static final Set<String> DEVS = Set.of(
            "Dev",
            "Meatwo310",
            "Meatwo310offline",
            "uribo_ya",
            "valine_3g"
    );

    public DpvpTweaks(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(com.dousiyo.dpvptweaks.client.ClientBootstrap::onFMLClientSetup);
        }

        ModItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModEntities.register(modEventBus);

        context.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        context.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
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

        boolean deleted = false;
        try {
            if (files.length == 1 && files[0].getName().equals("example.js")) {
                deleted = files[0].delete();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to delete example.js", e);
        }
        if (deleted || files.length == 0) return;

        LOGGER.error("Following scripts are not allowed: {}", (Object) files);
        if (bypass) return;

        Runtime.getRuntime().halt(310);
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

        Runtime.getRuntime().halt(-310);
    }

    private static String modsToString(Set<String> mods) {
        return mods.stream()
                .sorted()
                .map(mod -> "\"" + mod + "\"")
                .toList()
                .toString();
    }
}

