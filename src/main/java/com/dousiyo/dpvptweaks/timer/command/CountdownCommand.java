package com.dousiyo.dpvptweaks.timer.command;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.timer.core.CountdownTitleManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CountdownCommand {
    private CountdownCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(buildRoot());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildRoot() {
        return Commands.literal("countdown")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("start")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(CountdownCommand::startTargets)
                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                        .executes(CountdownCommand::startTargetsWithCommand))))
                .then(Commands.literal("stop")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(CountdownCommand::stopTargets)));
    }

    private static int startTargets(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return startCountdown(ctx, EntityArgument.getPlayers(ctx, "targets"), null);
    }

    private static int startTargetsWithCommand(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        String command = normalizeCommand(StringArgumentType.getString(ctx, "command"));
        return startCountdown(ctx, EntityArgument.getPlayers(ctx, "targets"), command);
    }

    private static int stopTargets(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return stopCountdown(ctx, EntityArgument.getPlayers(ctx, "targets"));
    }

    private static int startCountdown(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players, String command) {
        ServerLevel level = ctx.getSource().getServer().overworld();
        if (level == null) {
            level = ctx.getSource().getLevel();
        }

        CountdownTitleManager manager = CountdownTitleManager.get(level);
        for (ServerPlayer player : players) {
            manager.start(player, level, command);
        }

        int affected = players.size();
        if (command == null || command.isBlank()) {
            ctx.getSource().sendSuccess(() -> Component.literal("10秒カウントダウンを開始しました: 対象人数=" + affected), true);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal("10秒カウントダウンを開始しました: 対象人数=" + affected + ", 終了時コマンド=" + command), true);
        }
        return affected;
    }

    private static int stopCountdown(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        ServerLevel level = ctx.getSource().getServer().overworld();
        if (level == null) {
            level = ctx.getSource().getLevel();
        }

        CountdownTitleManager manager = CountdownTitleManager.get(level);
        for (ServerPlayer player : players) {
            manager.stop(player);
        }

        int affected = players.size();
        ctx.getSource().sendSuccess(() -> Component.literal("カウントダウンを停止しました: 対象人数=" + affected), true);
        return affected;
    }

    private static String normalizeCommand(String command) {
        String trimmed = command.trim();
        if (trimmed.startsWith("/")) {
            return trimmed.substring(1);
        }
        return trimmed;
    }
}
