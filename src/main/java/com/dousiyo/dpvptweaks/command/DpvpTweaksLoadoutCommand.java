package com.dousiyo.dpvptweaks.command;

import com.dousiyo.dpvptweaks.loadout.LoadoutDataManager;
import com.dousiyo.dpvptweaks.loadout.LoadoutSessionManager;
import com.dousiyo.dpvptweaks.network.CloseLoadoutGuiPacket;
import com.dousiyo.dpvptweaks.network.LoadoutGuiNetwork;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.stream.Collectors;

public class DpvpTweaksLoadoutCommand {
    static void register(LiteralArgumentBuilder<CommandSourceStack> builder, RegisterCommandsEvent event) {
        builder.then(Commands.literal("loadout")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("open")
                        .then(Commands.argument("set", StringArgumentType.word())
                                .suggests((ctx, suggestions) -> SharedSuggestionProvider.suggest(
                                        LoadoutDataManager.setIds().stream().map(ResourceLocation::toString),
                                        suggestions
                                ))
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(DpvpTweaksLoadoutCommand::openNamedLoadoutSet))))
                .then(Commands.literal("list")
                        .executes(DpvpTweaksLoadoutCommand::listLoadoutDefinitions))
                .then(Commands.literal("validate")
                        .executes(DpvpTweaksLoadoutCommand::validateLoadoutDefinitions))
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(DpvpTweaksLoadoutCommand::openTbLoadoutGui))
                .then(Commands.literal("tb")
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(DpvpTweaksLoadoutCommand::openTbLoadoutGui)))
                .then(Commands.literal("tb_mini")
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(DpvpTweaksLoadoutCommand::openTbMiniLoadoutGui)))
                .then(Commands.literal("close")
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(DpvpTweaksLoadoutCommand::closeLoadoutGui)))
        );
    }

    private static int openTbLoadoutGui(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return openLoadoutSet(ctx, LoadoutDataManager.DEFAULT_LOADOUT_SET, false);
    }

    private static int openTbMiniLoadoutGui(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return openLoadoutSet(ctx, LoadoutDataManager.DEFAULT_MINI_LOADOUT_SET, true);
    }

    private static int openNamedLoadoutSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String rawSetId = StringArgumentType.getString(ctx, "set");
        ResourceLocation setId = LoadoutDataManager.parseSetId(rawSetId);
        if (setId == null) {
            ctx.getSource().sendFailure(Component.literal("Invalid loadout set id: " + rawSetId));
            return 0;
        }

        return openLoadoutSet(ctx, setId, LoadoutDataManager.getSet(setId) != null && LoadoutDataManager.getSet(setId).isMiniLayout());
    }

    private static int openLoadoutSet(CommandContext<CommandSourceStack> ctx, ResourceLocation setId, boolean mini) throws CommandSyntaxException {
        var players = EntityArgument.getPlayers(ctx, "players");
        int count = 0;
        for (ServerPlayer player : players) {
            if (LoadoutSessionManager.open(player, setId, mini)) {
                count++;
            }
        }

        int finalCount = count;
        ctx.getSource().sendSuccess(() -> Component.literal(finalCount + " player(s) were asked to open loadout set " + setId + "."), true);
        return count > 0 ? Command.SINGLE_SUCCESS : 0;
    }

    private static int closeLoadoutGui(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var players = EntityArgument.getPlayers(ctx, "players");
        int count = 0;
        for (ServerPlayer player : players) {
            LoadoutGuiNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CloseLoadoutGuiPacket());
            count++;
        }

        int finalCount = count;
        ctx.getSource().sendSuccess(() -> Component.literal(finalCount + " player(s) were asked to close loadout GUI."), true);
        return count > 0 ? Command.SINGLE_SUCCESS : 0;
    }

    private static int listLoadoutDefinitions(CommandContext<CommandSourceStack> ctx) {
        String setText = LoadoutDataManager.setIds().isEmpty()
                ? "(none)"
                : LoadoutDataManager.setIds().stream().map(ResourceLocation::toString).collect(Collectors.joining(", "));
        String loadoutText = LoadoutDataManager.loadoutIds().isEmpty()
                ? "(none)"
                : LoadoutDataManager.loadoutIds().stream().map(ResourceLocation::toString).collect(Collectors.joining(", "));

        ctx.getSource().sendSuccess(() -> Component.literal("Loadout sets: " + setText), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Loadouts: " + loadoutText), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int validateLoadoutDefinitions(CommandContext<CommandSourceStack> ctx) {
        List<String> issues = LoadoutDataManager.validate(ctx.getSource().getServer());
        if (issues.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("Loadout validation passed."), false);
            return Command.SINGLE_SUCCESS;
        }

        ctx.getSource().sendFailure(Component.literal("Loadout validation found " + issues.size() + " issue(s)."));
        issues.stream()
                .limit(20)
                .forEach(issue -> ctx.getSource().sendFailure(Component.literal("- " + issue)));
        if (issues.size() > 20) {
            ctx.getSource().sendFailure(Component.literal("...and " + (issues.size() - 20) + " more issue(s)."));
        }
        return 0;
    }
}
