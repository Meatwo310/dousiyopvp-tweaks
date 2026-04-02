package com.dousiyo.dpvptweaks.command;

import com.dousiyo.dpvptweaks.network.LoadoutGuiNetwork;
import com.dousiyo.dpvptweaks.network.OpenLoadoutGuiPacket;
import com.dousiyo.dpvptweaks.network.OpenMiniLoadoutGuiPacket;
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
import net.minecraftforge.network.PacketDistributor;

public class DpvpTweaksLoadoutCommand {
    static void register(LiteralArgumentBuilder<CommandSourceStack> builder, RegisterCommandsEvent event) {
        builder.then(Commands.literal("loadout")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(DpvpTweaksLoadoutCommand::openTbLoadoutGui))
                .then(Commands.literal("tb")
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(DpvpTweaksLoadoutCommand::openTbLoadoutGui)))
                .then(Commands.literal("tb_mini")
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(DpvpTweaksLoadoutCommand::openTbMiniLoadoutGui)))
        );
    }

    private static int openTbLoadoutGui(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var players = EntityArgument.getPlayers(ctx, "players");
        int count = 0;
        for (ServerPlayer player : players) {
            LoadoutGuiNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenLoadoutGuiPacket());
            count++;
        }

        int finalCount = count;
        ctx.getSource().sendSuccess(() -> Component.literal(finalCount + " player(s) were asked to open tb loadout GUI."), true);
        return count > 0 ? Command.SINGLE_SUCCESS : 0;
    }

    private static int openTbMiniLoadoutGui(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var players = EntityArgument.getPlayers(ctx, "players");
        int count = 0;
        for (ServerPlayer player : players) {
            LoadoutGuiNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenMiniLoadoutGuiPacket());
            count++;
        }

        int finalCount = count;
        ctx.getSource().sendSuccess(() -> Component.literal(finalCount + " player(s) were asked to open tb_mini loadout GUI."), true);
        return count > 0 ? Command.SINGLE_SUCCESS : 0;
    }
}

