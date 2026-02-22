package com.dousiyo.dpvptweaks.client;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.network.ClientNetwork;
import com.dousiyo.dpvptweaks.network.RelayNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientBootstrap {
    private static final boolean ENABLE_SESSION_CONSISTENCY = false;

    private ClientBootstrap() {
    }

    public static void onFMLClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ClientNetwork.register();
            if (ENABLE_SESSION_CONSISTENCY) {
                RelayNetwork.register();
            }
        });

        String username = Minecraft.getInstance().getUser().getName();
        DpvpTweaks.runClientStartupChecks(username);
    }
}
