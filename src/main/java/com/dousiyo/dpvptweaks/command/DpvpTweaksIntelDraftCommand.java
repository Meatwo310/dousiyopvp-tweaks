package com.dousiyo.dpvptweaks.command;

import com.dousiyo.dpvptweaks.network.CloseIntelDraftGuiPacket;
import com.dousiyo.dpvptweaks.network.LoadoutGuiNetwork;
import com.dousiyo.dpvptweaks.network.OpenIntelDraftGuiPacket;
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
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.PacketDistributor;

public class DpvpTweaksIntelDraftCommand {
    static void register(LiteralArgumentBuilder<CommandSourceStack> builder, RegisterCommandsEvent event) {
        builder.then(Commands.literal("inteldraft")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(DpvpTweaksIntelDraftCommand::openIntelDraftGui))
                .then(Commands.literal("close")
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(DpvpTweaksIntelDraftCommand::closeIntelDraftGui)))
        );
    }

    private static int openIntelDraftGui(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (ModList.get().isLoaded("dousiyoserver")) {
            ctx.getSource().sendFailure(Component.literal("Intel Draft is server-authoritative. Use /dploadout inteldraft instead."));
            return 0;
        }

        var players = EntityArgument.getPlayers(ctx, "players");
        int count = 0;
        for (ServerPlayer player : players) {
            LoadoutGuiNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenIntelDraftGuiPacket());
            count++;
        }

        int finalCount = count;
        ctx.getSource().sendSuccess(() -> Component.literal(finalCount + " player(s) were asked to open Intel Draft GUI."), true);
        return count > 0 ? Command.SINGLE_SUCCESS : 0;
    }

    private static int closeIntelDraftGui(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var players = EntityArgument.getPlayers(ctx, "players");
        int count = 0;
        for (ServerPlayer player : players) {
            LoadoutGuiNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CloseIntelDraftGuiPacket());
            count++;
        }

        int finalCount = count;
        ctx.getSource().sendSuccess(() -> Component.literal(finalCount + " player(s) were asked to close Intel Draft GUI."), true);
        return count > 0 ? Command.SINGLE_SUCCESS : 0;
    }
}
