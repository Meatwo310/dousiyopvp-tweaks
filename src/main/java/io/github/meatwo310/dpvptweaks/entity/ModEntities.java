package io.github.meatwo310.dpvptweaks.entity;

import io.github.meatwo310.dpvptweaks.DpvpTweaks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DpvpTweaks.MODID);

    // Helper method to reduce repetition in entity registration
    private static <T extends Entity> RegistryObject<EntityType<T>> registerEntity(String name, EntityFactory<T> factory) {
        return TYPES.register(name, () -> EntityType.Builder
                .of(factory::create, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(4)
                .updateInterval(10)
                .fireImmune()
                .build(DpvpTweaks.MODID + name));
    }

    // Functional interface to allow constructor references
    @FunctionalInterface
    public interface EntityFactory<T extends Entity> {
        T create(EntityType<T> type, Level level);
    }

    public static final RegistryObject<EntityType<ThrownStone.ThrownValine1g>> THROWN_VALINE1G =
            registerEntity("thrown_valine1g", ThrownStone.ThrownValine1g::new);

    public static final RegistryObject<EntityType<ThrownStone.ThrownValine2g>> THROWN_VALINE2G =
            registerEntity("thrown_valine2g", ThrownStone.ThrownValine2g::new);

    public static final RegistryObject<EntityType<ThrownStone.ThrownValine3g>> THROWN_VALINE3G =
            registerEntity("thrown_valine3g", ThrownStone.ThrownValine3g::new);

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
    }
}