package io.github.meatwo310.dpvptweaks;

import com.mojang.logging.LogUtils;
import io.github.meatwo310.dpvptweaks.config.CommonConfig;
import io.github.meatwo310.dpvptweaks.config.ServerConfig;
import io.github.meatwo310.dpvptweaks.entity.ModEntities;
import io.github.meatwo310.dpvptweaks.item.ModCreativeModeTabs;
import io.github.meatwo310.dpvptweaks.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forgespi.language.IModInfo;
import org.slf4j.Logger;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Mod(DpvpTweaks.MODID)
public class DpvpTweaks {
    public static final String MODID = "dpvptweaks";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Set<String> ALLOWED_MODS = Set.of(
            "appleskin",
            "architectury",
            "armourers_workshop",
            "ashvehicle",
            "athena",
            "badpackets",
            "betterf3",
            "caelus",
            "canary",
            "catalogue",
            "chat_heads",
            "chloride",
            "cloth_config",
            "collective",
            "commongroovylibrary",
            "commonnetworking",
            "configured",
            "controlling",
            "coroutil",
            "creativecore",
            "curios",
            "damageindicator",
            "dpvptweaks",
            "dummmmmmy",
            "ea",
            "eatinganimation",
            "elitex",
            "embeddium",
            "embeddium_extra",
            "endlessammo",
            "entityculling",
            "extremesoundmuffler",
            "ferritecore",
            "forge",
            "framework",
            "fusion",
            "fzzy_config",
            "geckolib",
            "gml",
            "guccivuitton",
            "javd",
            "journeymap",
            "journeymapteams",
            "jpy",
            "konkrete",
            "kotlinforforge",
            "kubejs",
            "lightmanscurrency",
            "lrarmor",
            "lrtactical",
            "maxstuff",
            "mcsp",
            "melody",
            "mildb",
            "minecraft",
            "mixinextras",
            "mixinsquared",
            "modernfix",
            "modernui",
            "moonlight",
            "mousetweaks",
            "mru",
            "mts",
            "night_vision_curios",
            "nirvana_lib",
            "notenoughanimations",
            "notenoughcrashes",
            "oculus",
            "packetfixer",
            "parcool",
            "parcool_compat_addon",
            "pickupnotifier",
            "playeranimator",
            "presencefootsteps",
            "puzzlesaccessapi",
            "puzzleslib",
            "resourcefullib",
            "rhino",
            "roughtweaks",
            "rubidium",
            "screenshotclipboard",
            "seamless_loading_screen",
            "searchables",
            "shouldersurfing",
            "smartbrainlib",
            "sound_physics_remastered",
            "sounds",
            "starterkit",
            "stylisheffects",
            "suggestionproviderfix",
            "superbwarfare",
            "supermartijn642configlib",
            "supermartijn642corelib",
            "tacz",
            "tacztweaks",
            "takkit",
            "teampinmod",
            "timestamp_chat",
            "toofast",
            "tp_shooting",
            "transition",
            "trender",
            "trenzalore",
            "tsukichat",
            "untranslateditems",
            "vvp",
            "waterdripsound",
            "xlpackets",
            "yet_another_config_lib_v3"
    );

    public static final Set<String> DEVS = Set.of(
            "Dev",
            "Meatwo310",
            "namaedosiyo",
            "uribo_ya",
            "valine_3g"
    );

    public DpvpTweaks(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::onFMLClientSetup);

        ModItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModEntities.register(modEventBus);

        context.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        context.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
    }

    private void onFMLClientSetup(FMLClientSetupEvent event) {
        boolean bypass = DEVS.contains(Minecraft.getInstance().getUser().getName());
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

        Runtime.getRuntime().halt(1);
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

        Runtime.getRuntime().halt(-1);
    }

    private static String modsToString(Set<String> mods) {
        return mods.stream()
                .sorted()
                .map(mod -> "\"" + mod + "\"")
                .toList()
                .toString();
    }
}
