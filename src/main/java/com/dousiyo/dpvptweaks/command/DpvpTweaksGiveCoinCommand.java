package com.dousiyo.dpvptweaks.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.lightman314.lightmanscurrency.api.money.coins.CoinAPI;
import io.github.lightman314.lightmanscurrency.common.items.WalletItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.items.ItemHandlerHelper;

public class DpvpTweaksGiveCoinCommand {
    public static final String KEY_INVALID_COIN = "commands.dpvptweaks.invalid_coin";

    private static final SimpleCommandExceptionType INVALID_COIN = new SimpleCommandExceptionType(Component.translatable(KEY_INVALID_COIN));

    static void register(LiteralArgumentBuilder<CommandSourceStack> builder, RegisterCommandsEvent event) {
        builder.then(Commands.literal("givecoin")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("players", EntityArgument.players())
                        .then(Commands.argument("coin", ItemArgument.item(event.getBuildContext()))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(DpvpTweaksGiveCoinCommand::givecoin)
                                )
                                .executes(DpvpTweaksGiveCoinCommand::givecoin)
                        )
                )
        );
    }

    private static int givecoin(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var players = EntityArgument.getPlayers(ctx, "players");
        var coin = ItemArgument.getItem(ctx, "coin");

        int amount;
        try {
            amount = IntegerArgumentType.getInteger(ctx, "amount");
        } catch (IllegalArgumentException e) {
            amount = 1;
        }

        ItemStack sourceStack = coin.createItemStack(amount, true);
        if (!CoinAPI.getApi().IsAllowedInCoinContainer(sourceStack, false)) {
            throw INVALID_COIN.create();
        }

        for (var player : players) {
            ItemStack coinStack = sourceStack.copy();
            ItemStack wallet = CoinAPI.getApi().getEquippedWallet(player);
            if (!wallet.isEmpty()) {
                coinStack = WalletItem.PickupCoin(wallet, coinStack);
            }
            if (!coinStack.isEmpty()) {
                ItemHandlerHelper.giveItemToPlayer(player, coinStack);
            }
            if (!player.level().isClientSide) {
                WalletItem.playCollectSound(player, wallet);
            }
        }

        return Command.SINGLE_SUCCESS;
    }
}
