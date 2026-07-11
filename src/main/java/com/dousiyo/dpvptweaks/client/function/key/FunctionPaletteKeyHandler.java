package com.dousiyo.dpvptweaks.client.function.key;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.client.function.FunctionPaletteClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=DpvpTweaks.MODID,bus=Mod.EventBusSubscriber.Bus.FORGE,value=Dist.CLIENT)
public final class FunctionPaletteKeyHandler {
    private FunctionPaletteKeyHandler() {}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase==TickEvent.Phase.END) while(FunctionPaletteKeyMappings.OPEN.consumeClick()) FunctionPaletteClient.tryOpenPalette();
    }
}
