package com.dousiyo.dpvptweaks.command;

import com.dousiyo.dpvptweaks.secretoperations.SecretOperationsManager;
import com.dousiyo.dpvptweaks.secretoperations.SecretShowdownManager;
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

public final class DpvpTweaksSecretOperationsCommand {
    private DpvpTweaksSecretOperationsCommand() {}

    static void register(LiteralArgumentBuilder<CommandSourceStack> root, RegisterCommandsEvent ignored) {
        root.then(build("secretoperations", false));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildDirect(String name) {
        return build(name, true);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> build(String name, boolean opensGui) {
        var secret = Commands.literal(name).requires(source -> source.hasPermission(2));
        if (opensGui) secret.executes(context -> {
            SecretShowdownManager.openAdmin(context.getSource().getPlayerOrException(), "");
            return Command.SINGLE_SUCCESS;
        });
        secret.then(targeted("enable", SecretOperationsManager::enableManually));
        secret.then(targeted("disable", SecretOperationsManager::disableManually));
        secret.then(Commands.literal("disableall").executes(context -> {
            SecretOperationsManager.disableAllManual(context.getSource().getServer());
            context.getSource().sendSuccess(() -> Component.literal(
                    "全プレイヤーのコマンド指定SECRET OPERATIONS状態を無効化しました"), true);
            return Command.SINGLE_SUCCESS;
        }));
        secret.then(Commands.literal("status")
                .then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                    context.getSource().sendSuccess(() -> SecretOperationsManager.status(player), false);
                    return Command.SINGLE_SUCCESS;
                })));
        return secret;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> targeted(String name, Predicate<ServerPlayer> action) {
        return Commands.literal(name).then(Commands.argument("players", EntityArgument.players())
                .executes(context -> apply(context, action, name)));
    }

    private static int apply(CommandContext<CommandSourceStack> context, Predicate<ServerPlayer> action, String actionName)
            throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
        int changed = 0;
        for (ServerPlayer player : players) if (action.test(player)) changed++;
        int changedCount = changed;
        context.getSource().sendSuccess(() -> Component.literal("SECRET OPERATIONS " + actionName
                + ": 対象=" + players.size() + " / 変更=" + changedCount), true);
        return players.isEmpty() ? 0 : players.size();
    }
}
