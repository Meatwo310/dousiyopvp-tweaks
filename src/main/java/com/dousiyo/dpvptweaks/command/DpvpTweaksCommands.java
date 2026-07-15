package com.dousiyo.dpvptweaks.command;

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
        DpvpTweaksLoadoutCommand.register(builder, event);
        DpvpTweaksIntelDraftCommand.register(builder, event);
        DpvpTweaksSecretOperationsCommand.register(builder, event);
        DpvpTweaksDamageFeedbackCommand.register(builder, event);
        DpvpTweaksArsenalCommand.register(builder, event);

        event.getDispatcher().register(builder);
        event.getDispatcher().register(DpvpTweaksLoadoutCommand.buildDirectCommand());
        event.getDispatcher().register(DpvpTweaksIntelDraftCommand.buildDirectCommand());
        event.getDispatcher().register(DpvpTweaksSecretOperationsCommand.buildDirect("secretoperations"));
        event.getDispatcher().register(DpvpTweaksSecretOperationsCommand.buildDirect("sp"));
        event.getDispatcher().register(DpvpTweaksDamageFeedbackCommand.buildDirect("damagefeedback"));
        event.getDispatcher().register(DpvpTweaksArsenalCommand.buildDirect());
    }
}

