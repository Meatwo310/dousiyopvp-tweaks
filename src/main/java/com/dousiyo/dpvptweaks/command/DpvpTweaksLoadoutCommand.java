package com.dousiyo.dpvptweaks.command;

import com.dousiyo.dpvptweaks.network.ClientNetwork;
import com.dousiyo.dpvptweaks.network.OpenLoadoutGuiPacket;
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
                        .executes(DpvpTweaksLoadoutCommand::openLoadoutGui))
        );
    }

    private static int openLoadoutGui(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var players = EntityArgument.getPlayers(ctx, "players");
        int count = 0;
        for (ServerPlayer player : players) {
            ClientNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenLoadoutGuiPacket());
            count++;
        }

        int finalCount = count;
        ctx.getSource().sendSuccess(() -> Component.literal(finalCount + " player(s) were asked to open loadout GUI."), true);
        return count > 0 ? Command.SINGLE_SUCCESS : 0;
    }
}

