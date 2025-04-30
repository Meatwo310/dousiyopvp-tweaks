package io.github.meatwo310.dpvptweaks.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.lightman314.lightmanscurrency.api.money.coins.CoinAPI;
import io.github.lightman314.lightmanscurrency.common.items.WalletItem;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;

@Mod.EventBusSubscriber
public class DpvpTweaksCommand {
    private static final SimpleCommandExceptionType INVALID_COIN = new SimpleCommandExceptionType(Component.translatable("commands.dpvptweaks.invalid_coin"));

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher(), event.getBuildContext());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("dpvptweaks")
                .then(Commands.literal("givecoin")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("coin", ItemArgument.item(context))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(DpvpTweaksCommand::givecoin)
                                        )
                                        .executes(DpvpTweaksCommand::givecoin)
                                )
                        )
                )
        );
    }

    private static int givecoin(CommandContext<CommandSourceStack> commandSourceStackCommandContext) throws CommandSyntaxException {
        var players = EntityArgument.getPlayers(commandSourceStackCommandContext, "players");
        var coin = ItemArgument.getItem(commandSourceStackCommandContext, "coin");

        int amount;
        try {
            amount = IntegerArgumentType.getInteger(commandSourceStackCommandContext, "amount");
        } catch (IllegalArgumentException e) {
            amount = 1;
        }

        var coinStack = coin.createItemStack(amount, true);
        if (!CoinAPI.API.IsAllowedInCoinContainer(coinStack, false)) throw INVALID_COIN.create();

        for (var player : players) {
            ItemStack wallet = CoinAPI.API.getEquippedWallet(player);
            if (!wallet.isEmpty()) {
                WalletItem walletItem = (WalletItem) wallet.getItem();
                coinStack = WalletItem.PickupCoin(wallet, coinStack);
            }
            if (!coinStack.isEmpty()) ItemHandlerHelper.giveItemToPlayer(player, coinStack);
            if (!player.level().isClientSide) WalletItem.playCollectSound(player,wallet);
        }

        return Command.SINGLE_SUCCESS;
    }
}
