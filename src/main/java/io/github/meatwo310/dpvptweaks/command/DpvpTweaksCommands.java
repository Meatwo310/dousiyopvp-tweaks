package io.github.meatwo310.dpvptweaks.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class DpvpTweaksCommands {
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("dpvptweaks");

        DpvpTweaksGiveCoinCommand.register(builder, event);
        DpvpTweaksConfigCommand.register(builder, event);
        DpvpTweaksMuteCommand.register(builder, event);
        DpvpTweaksClearCommand.register(builder, event);

        event.getDispatcher().register(builder);
    }
}
