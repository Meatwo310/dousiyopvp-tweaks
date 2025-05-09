package io.github.meatwo310.dpvptweaks.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.meatwo310.dpvptweaks.config.ServerConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class DpvpTweaksMuteCommand {
    static void register(LiteralArgumentBuilder<CommandSourceStack> builder, RegisterCommandsEvent event) {
        builder.then(Commands.literal("mute")
                        .requires(s -> s.hasPermission(2))
                        .executes(DpvpTweaksMuteCommand::listMuted)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(DpvpTweaksMuteCommand::mutePlayer))
        ).then(Commands.literal("unmute")
                        .requires(s -> s.hasPermission(2))
                        .executes(DpvpTweaksMuteCommand::listMuted)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(DpvpTweaksMuteCommand::unmutePlayer))
        );
    }

    private static int mutePlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String playerName = getPlayerName(ctx);
        var muted = ServerConfig.mutedPlayersSet;
        if (muted.contains(playerName)) {
            ctx.getSource().sendSuccess(() -> Component.literal("プレイヤー " + playerName + " はすでにミュートされています"), true);
            return Command.SINGLE_SUCCESS;
        }
        ArrayList<String> mutedNew = new ArrayList<>(muted);
        mutedNew.add(playerName);
        ServerConfig.MUTED_PLAYERS.set(mutedNew);
        ctx.getSource().sendSuccess(() -> Component.literal("プレイヤー " + playerName + " をミュートしました"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int unmutePlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String playerName = getPlayerName(ctx);
        var muted = ServerConfig.mutedPlayersSet;
        if (!muted.contains(playerName)) {
            ctx.getSource().sendSuccess(() -> Component.literal("プレイヤー " + playerName + " はミュートされていません"), true);
            return Command.SINGLE_SUCCESS;
        }
        ArrayList<String> mutedNew = new ArrayList<>(muted);
        mutedNew.remove(playerName);
        ServerConfig.MUTED_PLAYERS.set(mutedNew);
        ctx.getSource().sendSuccess(() -> Component.literal("プレイヤー " + playerName + " のミュートを解除しました"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int listMuted(CommandContext<CommandSourceStack> ctx) {
        var muted = ServerConfig.mutedPlayersSet;
        if (muted.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("ミュートされたプレイヤーは存在しません"), true);
        } else {
            String message = muted.stream().collect(Collectors.joining(", ", "ミュートされたプレイヤー: ", ""));
            ctx.getSource().sendSuccess(() -> Component.literal(message), true);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static String getPlayerName(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return EntityArgument.getPlayer(ctx, "player").getName().getString();
    }
}
