package io.github.meatwo310.dpvptweaks.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

public class DpvpTweaksClearCommand {
    static void register(LiteralArgumentBuilder<CommandSourceStack> builder, RegisterCommandsEvent event) {
        builder.then(Commands.literal("clearinventory")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(DpvpTweaksClearCommand::clearInventory))
        );
    }

    private static int clearInventory(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var players = EntityArgument.getPlayers(ctx, "players");
        int playerSize = players.size();
        for (ServerPlayer player : players) {
            // Clear the player's inventory but keep the armor
            player.getInventory().items.clear();
            player.getInventory().offhand.clear();
        }
        ctx.getSource().sendSuccess(() -> Component.literal(playerSize + "人のプレイヤーのインベントリを空にしました"), true);
        return playerSize;
    }
}
