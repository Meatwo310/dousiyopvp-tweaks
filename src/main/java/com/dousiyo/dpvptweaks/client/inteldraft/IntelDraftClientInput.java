package com.dousiyo.dpvptweaks.client.inteldraft;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.network.inteldraft.ActivateIntelTechPacket;
import com.dousiyo.dpvptweaks.network.loadout.LoadoutGuiNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only input edge detection; zero packets while a key is held. */
@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IntelDraftClientInput {
    private static boolean jumpWasDown;
    private IntelDraftClientInput() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        boolean down = mc.options.keyJump.isDown();
        if (down && !jumpWasDown && ClientIntelDraftState.has("double_jump")
                && mc.player != null && !mc.player.onGround() && mc.screen == null)
            LoadoutGuiNetwork.CHANNEL.sendToServer(new ActivateIntelTechPacket(ActivateIntelTechPacket.Action.DOUBLE_JUMP));
        jumpWasDown = down;
    }
}
