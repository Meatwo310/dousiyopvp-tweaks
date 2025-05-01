package io.github.meatwo310.dpvptweaks.item;

import io.github.meatwo310.dpvptweaks.DpvpTweaks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DpvpTweaks.MODID);
    public static final String GENERAL_TAB_ID = "item_group.%s.general".formatted(DpvpTweaks.MODID);

    public static final RegistryObject<CreativeModeTab> GENERAL_TAB = CREATIVE_MODE_TABS.register(GENERAL_TAB_ID, () -> CreativeModeTab.builder()
            .title(Component.translatable(GENERAL_TAB_ID))
            .icon(() -> ModItems.VALINE2G.get().getDefaultInstance())
            .displayItems(ModCreativeModeTabs::accept)
            .build()
    );

    private static void accept(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (RegistryObject<Item> registryItem : ModItems.ITEMS.getEntries()) {
            output.accept(registryItem.get());
        }
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
