package com.dousiyo.dpvptweaks.datagen;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.command.DpvpTweaksGiveCoinCommand;
import com.dousiyo.dpvptweaks.item.ModCreativeModeTabs;
import com.dousiyo.dpvptweaks.item.ModItems;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

public class LanguageGen {
    protected static void register(boolean run, DataGenerator generator) {
        generator.addProvider(run, (DataProvider.Factory<EnUs>) EnUs::new);
        generator.addProvider(run, (DataProvider.Factory<JaJp>) JaJp::new);
    }

    public static class EnUs extends LanguageProvider {
        public EnUs(PackOutput output) {
            super(output, DpvpTweaks.MODID, "en_us");
        }

        @Override
        protected void addTranslations() {
            add(ModCreativeModeTabs.GENERAL_TAB_ID, "Dousiyo PvP Tweaks");
            add(ModItems.VALINE1G.get(), "Valine1g");
            add(ModItems.VALINE2G.get(), "Valine2g");
            add(ModItems.VALINE3G.get(), "Valine3g");
            add(DpvpTweaksGiveCoinCommand.KEY_INVALID_COIN, "The item is not a coin");
        }
    }

    public static class JaJp extends LanguageProvider {
        public JaJp(PackOutput output) {
            super(output, DpvpTweaks.MODID, "ja_jp");
        }

        @Override
        protected void addTranslations() {
            add(ModItems.VALINE1G.get(), "バリン1g");
            add(ModItems.VALINE2G.get(), "バリン2g");
            add(ModItems.VALINE3G.get(), "バリン3g");
            add(DpvpTweaksGiveCoinCommand.KEY_INVALID_COIN, "指定されたアイテムは硬貨ではありません");
        }
    }
}
