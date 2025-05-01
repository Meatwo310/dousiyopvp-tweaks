package io.github.meatwo310.dpvptweaks.datagen;

import io.github.meatwo310.dpvptweaks.DpvpTweaks;
import io.github.meatwo310.dpvptweaks.item.ModItems;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.BlockModelProvider;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModelGen {
    protected static void register(boolean run, DataGenerator generator, PackOutput packOutput, ExistingFileHelper efh) {
        generator.addProvider(run, new ItemModel(packOutput, DpvpTweaks.MODID, efh));
        generator.addProvider(run, new BlockModel(packOutput, DpvpTweaks.MODID, efh));
    }

    private static class ItemModel extends ItemModelProvider {
        public ItemModel(PackOutput output, String modid, ExistingFileHelper existingFileHelper) {
            super(output, modid, existingFileHelper);
        }

        @Override
        protected void registerModels() {
            this.basicItem(ModItems.VALINE1G.get());
            this.basicItem(ModItems.VALINE2G.get());
            this.basicItem(ModItems.VALINE3G.get());
        }
    }

    private static class BlockModel extends BlockModelProvider {
        public BlockModel(PackOutput output, String modid, ExistingFileHelper existingFileHelper) {
            super(output, modid, existingFileHelper);
        }

        @Override
        protected void registerModels() {
        }
    }
}
