package io.github.meatwo310.dpvptweaks.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.lightman314.lightmanscurrency.api.money.coins.CoinAPI;
import io.github.lightman314.lightmanscurrency.common.items.WalletItem;
import io.github.meatwo310.dpvptweaks.config.ServerConfig;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber
public class DpvpTweaksCommand {
    public static final String KEY_INVALID_COIN = "commands.dpvptweaks.invalid_coin";
    private static final SimpleCommandExceptionType INVALID_COIN = new SimpleCommandExceptionType(Component.translatable(KEY_INVALID_COIN));

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
                ).then(Commands.literal("config")
                        .requires(s -> s.hasPermission(2))
                        .then(createConfigCommand("valine1g", ServerConfig.VALINE1G_DAMAGE, ServerConfig.VALINE1G_COOLDOWN))
                        .then(createConfigCommand("valine2g", ServerConfig.VALINE2G_DAMAGE, ServerConfig.VALINE2G_COOLDOWN))
                        .then(createConfigCommand("valine3g", ServerConfig.VALINE3G_DAMAGE, ServerConfig.VALINE3G_COOLDOWN))
                        .executes(ctx -> {
                            String message = """
                                    Current config:
                                    valine1g damage: %s
                                    valine1g cooldown: %d
                                    valine2g damage: %s
                                    valine2g cooldown: %d
                                    valine3g damage: %s
                                    valine3g cooldown: %d""".formatted(
                                    ServerConfig.VALINE1G_DAMAGE.get(),
                                    ServerConfig.VALINE1G_COOLDOWN.get(),
                                    ServerConfig.VALINE2G_DAMAGE.get(),
                                    ServerConfig.VALINE2G_COOLDOWN.get(),
                                    ServerConfig.VALINE3G_DAMAGE.get(),
                                    ServerConfig.VALINE3G_COOLDOWN.get()
                            );
                            ctx.getSource().sendSuccess(() -> Component.literal(message), false);
                            return Command.SINGLE_SUCCESS;
                        })
                ).then(Commands.literal("mute")
                        .requires(s -> s.hasPermission(2))
                        .executes(DpvpTweaksCommand::listMuted)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(DpvpTweaksCommand::mutePlayer)))
                .then(Commands.literal("unmute")
                        .requires(s -> s.hasPermission(2))
                        .executes(DpvpTweaksCommand::listMuted)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(DpvpTweaksCommand::unmutePlayer)))
                .then(Commands.literal("clearinventory")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(DpvpTweaksCommand::clearInventory)))
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

    public static int unmutePlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
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

    public static int listMuted(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
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

    private static int givecoin(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var players = EntityArgument.getPlayers(ctx, "players");
        var coin = ItemArgument.getItem(ctx, "coin");

        int amount;
        try {
            amount = IntegerArgumentType.getInteger(ctx, "amount");
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

    private static LiteralArgumentBuilder<CommandSourceStack> createConfigCommand(String name, ForgeConfigSpec.ConfigValue<Double> damageConfig, ForgeConfigSpec.IntValue cooldownConfig) {
        return Commands.literal(name)
                .requires(commandSourceStack -> commandSourceStack.hasPermission(2))
                .then(registerCommand("damage", damageConfig))
                .then(registerCommand("cooldown", cooldownConfig));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerCommand(String name, ForgeConfigSpec.ConfigValue<Double> configValue) {
        return Commands.literal(name)
                .then(Commands.literal("reset")
                        .executes(context -> {
                            double previousValue = configValue.get();
                            configValue.set(configValue.getDefault());
                            context.getSource().sendSuccess(() -> Component.literal(name + " with value: " + previousValue + " reset to default: " + configValue.get()), false);
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(Commands.argument("value", FloatArgumentType.floatArg(0.0F))
                        .executes(context -> {
                            float value = FloatArgumentType.getFloat(context, "value");
                            configValue.set((double) value);
                            context.getSource().sendSuccess(() -> Component.literal(name + " set to: " + value), false);
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal(name + ": " + configValue.get()), false);
                    return Command.SINGLE_SUCCESS;
                });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerCommand(String name, ForgeConfigSpec.IntValue configValue) {
        return Commands.literal(name)
                .then(Commands.literal("reset")
                        .executes(context -> {
                            int previousValue = configValue.get();
                            configValue.set(configValue.getDefault());
                            context.getSource().sendSuccess(() -> Component.literal(name + " with value: " + previousValue + " reset to default: " + configValue.get()), false);
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                        .executes(context -> {
                            int value = IntegerArgumentType.getInteger(context, "value");
                            configValue.set(value);
                            context.getSource().sendSuccess(() -> Component.literal(name + " set to: " + value), false);
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal(name + ": " + configValue.get()), false);
                    return Command.SINGLE_SUCCESS;
                });
    }
}
