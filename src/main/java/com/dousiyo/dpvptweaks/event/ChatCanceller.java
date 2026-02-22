package com.dousiyo.dpvptweaks.event;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.config.ServerConfig;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChatCanceller {
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        if (ServerConfig.mutedPlayersSet.contains(event.getUsername())) {
            event.setCanceled(true);
        }
    }
}
