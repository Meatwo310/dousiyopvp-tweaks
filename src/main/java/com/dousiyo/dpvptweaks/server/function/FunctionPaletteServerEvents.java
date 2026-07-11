package com.dousiyo.dpvptweaks.server.function;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID)
public final class FunctionPaletteServerEvents {
    private FunctionPaletteServerEvents() {}
    @SubscribeEvent public static void serverStarted(ServerStartedEvent event) { FunctionPaletteManager.reload(event.getServer()); }
    @SubscribeEvent public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dousiyo").requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload").executes(context -> {
                    var result = FunctionPaletteManager.reload(context.getSource().getServer());
                    if (result.success()) {
                        context.getSource().sendSuccess(() -> Component.translatable("commands.dousiyo.reload.success", result.count()), true);
                        return result.count();
                    }
                    context.getSource().sendFailure(Component.translatable("commands.dousiyo.reload.failed"));
                    return 0;
                })));
    }
}
