package io.github.meatwo310.dpvptweaks.item;

import io.github.meatwo310.dpvptweaks.DpvpTweaks;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DpvpTweaks.MODID);

    public static final RegistryObject<Item> VALINE1G = ITEMS.register("valine1g", () ->
            new ThrowableStoneItem.ThrowableValine1gItem(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> VALINE2G = ITEMS.register("valine2g", () ->
            new ThrowableStoneItem.ThrowableValine2gItem(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> VALINE3G = ITEMS.register("valine3g", () ->
            new ThrowableStoneItem.ThrowableValine3gItem(new Item.Properties().stacksTo(64)));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
