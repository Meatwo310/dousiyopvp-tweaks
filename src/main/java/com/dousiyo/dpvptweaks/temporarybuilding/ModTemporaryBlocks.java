package com.dousiyo.dpvptweaks.temporarybuilding;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModTemporaryBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, DpvpTweaks.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DpvpTweaks.MODID);

    public static final RegistryObject<Block> WOOD = block("wood",
            () -> new TemporaryBuildingBlock(Block.Properties.copy(Blocks.OAK_PLANKS)
                    .strength(2.0F, 3.0F).pushReaction(PushReaction.BLOCK)));
    public static final RegistryObject<Block> STONE = block("stone",
            () -> new TemporaryBuildingBlock(Block.Properties.copy(Blocks.STONE_BRICKS)
                    .strength(4.0F, 6.0F).pushReaction(PushReaction.BLOCK)));

    private ModTemporaryBlocks() {}

    private static RegistryObject<Block> block(String name, java.util.function.Supplier<Block> supplier) {
        RegistryObject<Block> block = BLOCKS.register(name, supplier);
        ITEMS.register(name, () -> new TemporaryBuildingBlockItem(block.get(), new Item.Properties()));
        return block;
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}
