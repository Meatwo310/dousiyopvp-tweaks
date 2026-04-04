package com.dousiyo.dpvptweaks.effect;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, DpvpTweaks.MODID);

    public static final RegistryObject<MobEffect> DVD_EFFECT =
            EFFECTS.register("dvd_effect", DvdEffect::new);
    public static final RegistryObject<MobEffect> STATIC_OVERLAY_EFFECT =
            EFFECTS.register("static_overlay_effect", StaticOverlayEffect::new);

    private ModEffects() {
    }

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }
}
