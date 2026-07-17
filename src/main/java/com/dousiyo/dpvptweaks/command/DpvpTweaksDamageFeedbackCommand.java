package com.dousiyo.dpvptweaks.command;

import com.dousiyo.dpvptweaks.secretoperations.DamageFeedbackManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class DpvpTweaksDamageFeedbackCommand {
    private DpvpTweaksDamageFeedbackCommand() {}

    static void register(LiteralArgumentBuilder<CommandSourceStack> root, RegisterCommandsEvent ignored) {
        root.then(build("damagefeedback"));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildDirect(String name) {
        return build(name);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> build(String name) {
        return Commands.literal(name)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("enable").executes(context ->
                        setEnabled(context.getSource(), true)))
                .then(Commands.literal("disable").executes(context ->
                        setEnabled(context.getSource(), false)))
                .then(Commands.literal("toggle").executes(context -> {
                    boolean enabled = DamageFeedbackManager.toggle(context.getSource().getServer());
                    sendStatus(context.getSource(), enabled, true);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("status").executes(context -> {
                    sendStatus(context.getSource(), DamageFeedbackManager.isEnabled(), false);
                    return Command.SINGLE_SUCCESS;
                }));
    }

    private static int setEnabled(CommandSourceStack source, boolean enabled) {
        DamageFeedbackManager.setEnabled(source.getServer(), enabled);
        sendStatus(source, DamageFeedbackManager.isEnabled(), true);
        return Command.SINGLE_SUCCESS;
    }

    private static void sendStatus(CommandSourceStack source, boolean enabled, boolean broadcast) {
        source.sendSuccess(() -> Component.literal("DAMAGE FEEDBACK: "
                + (enabled ? "enabled" : "disabled")), broadcast);
    }
}
