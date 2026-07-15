package com.dousiyo.dpvptweaks.command;

import com.dousiyo.dpvptweaks.secretoperations.DamageFeedbackManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.function.Predicate;

public final class DpvpTweaksDamageFeedbackCommand {
    private DpvpTweaksDamageFeedbackCommand() {}

    static void register(LiteralArgumentBuilder<CommandSourceStack> root, RegisterCommandsEvent ignored) {
        root.then(build("damagefeedback"));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildDirect(String name) {
        return build(name);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> build(String name) {
        var feedback = Commands.literal(name).requires(source -> source.hasPermission(2));
        feedback.then(targeted("enable", DamageFeedbackManager::enable));
        feedback.then(targeted("disable", DamageFeedbackManager::disable));
        feedback.then(targeted("toggle", player -> {
            DamageFeedbackManager.toggle(player);
            return true;
        }));
        feedback.then(status());
        return feedback;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> targeted(
            String name, Predicate<ServerPlayer> action) {
        return Commands.literal(name)
                .executes(context -> applySelf(context, action, name))
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(context -> apply(context, action, name)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> status() {
        return Commands.literal("status")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    sendStatus(context, player);
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            sendStatus(context, EntityArgument.getPlayer(context, "player"));
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    private static int applySelf(CommandContext<CommandSourceStack> context,
                                 Predicate<ServerPlayer> action, String actionName)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        action.test(player);
        context.getSource().sendSuccess(() -> Component.literal(
                "DAMAGE FEEDBACK " + actionName + ": " + player.getGameProfile().getName()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int apply(CommandContext<CommandSourceStack> context,
                             Predicate<ServerPlayer> action, String actionName)
            throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
        int changed = 0;
        for (ServerPlayer player : players) if (action.test(player)) changed++;
        int changedCount = changed;
        context.getSource().sendSuccess(() -> Component.literal("DAMAGE FEEDBACK " + actionName
                + ": 対象=" + players.size() + " / 変更=" + changedCount), true);
        return players.isEmpty() ? 0 : players.size();
    }

    private static void sendStatus(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        context.getSource().sendSuccess(() -> Component.literal("DAMAGE FEEDBACK: "
                + (DamageFeedbackManager.isEnabled(player) ? "enabled" : "disabled")
                + " / player=" + player.getGameProfile().getName()), false);
    }
}
