package io.github.meatwo310.dpvptweaks;

import com.mojang.logging.LogUtils;
import io.github.meatwo310.dpvptweaks.config.ServerConfig;
import io.github.meatwo310.dpvptweaks.entity.ModEntities;
import io.github.meatwo310.dpvptweaks.item.ModCreativeModeTabs;
import io.github.meatwo310.dpvptweaks.item.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.io.File;
import java.util.Set;

@Mod(DpvpTweaks.MODID)
public class DpvpTweaks {
    public static final String MODID = "dpvptweaks";
    private static final Logger LOGGER = LogUtils.getLogger();

    public DpvpTweaks() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onFMLClientSetup);

        ModItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModEntities.register(modEventBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    }

    private void onFMLClientSetup(FMLClientSetupEvent event) {
        File scriptsDir = new File("./kubejs/client_scripts");
        if (scriptsDir.exists()) {
            File[] files = scriptsDir.listFiles();
            if (files != null && files.length != 0) Runtime.getRuntime().halt(1);
        }
        ModList.get().forEachModContainer((s, modContainer) -> {
            if (!Set.of("notenoughcrashes", "supermartijn642configlib", "parcool_compat_addon", "geckolib", "playeranimator", "suggestionproviderfix", "javd", "timestamp_chat", "controlling", "modernfix", "dpvptweaks", "rei_plugin_compatibilities", "jei", "mixinextras", "superbwarfare", "pickupnotifier", "melody", "chat_heads", "cloth_config", "sound_physics_remastered", "fzzy_config", "dummmmmmy", "trenzalore", "elitex", "konkrete", "embeddium", "rubidium", "athena", "gml", "lrarmor", "teampinmod", "supermartijn642corelib", "untranslateditems", "curios", "oculus", "collective", "commongroovylibrary", "searchables", "seamless_loading_screen", "nirvana_lib", "resourcefullib", "starterkit", "eatinganimation", "architectury", "mru", "framework", "maxstuff", "smartbrainlib", "waterdripsound", "stylisheffects", "rhino", "xlpackets", "tacz", "tacztweaks", "caelus", "chloride", "ea", "journeymap", "configured", "badpackets", "lightmanscurrency", "catalogue", "fusion", "puzzlesaccessapi", "forge", "toofast", "minecraft", "moonlight", "roughtweaks", "mousetweaks", "mixinsquared", "creativecore", "guccivuitton", "sounds", "roughlyenoughitems", "kubejs", "kotlinforforge", "notenoughanimations", "parcool", "entityculling", "canary", "tsukichat", "damageindicator", "screenshotclipboard", "coroutil", "appleskin", "night_vision_curios", "ferritecore", "yet_another_config_lib_v3", "embeddium_extra", "modernui", "puzzleslib", "betterf3", "jpy", "packetfixer", "extremesoundmuffler", "journeymapteams", "presencefootsteps").contains(s)) Runtime.getRuntime().halt(-1);
        });
    }
}
