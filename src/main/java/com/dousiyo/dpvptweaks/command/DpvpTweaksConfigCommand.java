package com.dousiyo.dpvptweaks.command;

import com.dousiyo.dpvptweaks.config.ServerConfig;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DpvpTweaksConfigCommand {
    static void register(LiteralArgumentBuilder<CommandSourceStack> builder, RegisterCommandsEvent event) {
        builder.then(Commands.literal("config")
                .requires(s -> s.hasPermission(2))
                .then(valineCommand("valine1g", ServerConfig.VALINE1G_DAMAGE, ServerConfig.VALINE1G_COOLDOWN))
                .then(valineCommand("valine2g", ServerConfig.VALINE2G_DAMAGE, ServerConfig.VALINE2G_COOLDOWN))
                .then(valineCommand("valine3g", ServerConfig.VALINE3G_DAMAGE, ServerConfig.VALINE3G_COOLDOWN))
                .then(Commands.literal("death")
                        .then(configStringList("clearInventoryTeams", ServerConfig.CLEAR_INVENTORY_ON_DEATH_TEAMS))
                        .then(configStringList("spectatorTeams", ServerConfig.SET_SPECTATOR_ON_DEATH_TEAMS))
                        .then(configStringList("loadoutTeams", ServerConfig.OPEN_LOADOUT_ON_RESPAWN_TEAMS))
                        .then(configStringList("miniLoadoutTeams", ServerConfig.OPEN_MINI_LOADOUT_ON_RESPAWN_TEAMS)))
                .executes(ctx -> {
                    String message = """
                            Current config:
                            valine1g damage: %s
                            valine1g cooldown: %d
                            valine2g damage: %s
                            valine2g cooldown: %d
                            valine3g damage: %s
                            valine3g cooldown: %d
                            death clear inventory teams: %s
                            death spectator teams: %s
                            death loadout teams: %s
                            death mini loadout teams: %s""".formatted(
                            ServerConfig.VALINE1G_DAMAGE.get(),
                            ServerConfig.VALINE1G_COOLDOWN.get(),
                            ServerConfig.VALINE2G_DAMAGE.get(),
                            ServerConfig.VALINE2G_COOLDOWN.get(),
                            ServerConfig.VALINE3G_DAMAGE.get(),
                            ServerConfig.VALINE3G_COOLDOWN.get(),
                            listToText(ServerConfig.CLEAR_INVENTORY_ON_DEATH_TEAMS.get()),
                            listToText(ServerConfig.SET_SPECTATOR_ON_DEATH_TEAMS.get()),
                            listToText(ServerConfig.OPEN_LOADOUT_ON_RESPAWN_TEAMS.get()),
                            listToText(ServerConfig.OPEN_MINI_LOADOUT_ON_RESPAWN_TEAMS.get())
                    );
                    ctx.getSource().sendSuccess(() -> Component.literal(message), false);
                    return Command.SINGLE_SUCCESS;
                })
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> valineCommand(String name, ForgeConfigSpec.ConfigValue<Double> damageConfig, ForgeConfigSpec.IntValue cooldownConfig) {
        return Commands.literal(name)
                .requires(commandSourceStack -> commandSourceStack.hasPermission(2))
                .then(config("damage", damageConfig))
                .then(config("cooldown", cooldownConfig));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> config(String name, ForgeConfigSpec.ConfigValue<Double> configValue) {
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

    private static LiteralArgumentBuilder<CommandSourceStack> config(String name, ForgeConfigSpec.IntValue configValue) {
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

    private static LiteralArgumentBuilder<CommandSourceStack> config(String name, ForgeConfigSpec.BooleanValue configValue) {
        return Commands.literal(name)
                .then(Commands.literal("reset")
                        .executes(context -> {
                            boolean previousValue = configValue.get();
                            configValue.set(configValue.getDefault());
                            context.getSource().sendSuccess(() -> Component.literal(name + " with value: " + previousValue + " reset to default: " + configValue.get()), false);
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> {
                            boolean value = BoolArgumentType.getBool(context, "value");
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

    private static LiteralArgumentBuilder<CommandSourceStack> configStringList(String name, ForgeConfigSpec.ConfigValue<List<? extends String>> configValue) {
        return Commands.literal(name)
                .then(Commands.literal("add")
                        .then(Commands.argument("value", StringArgumentType.word())
                                .executes(context -> {
                                    String value = StringArgumentType.getString(context, "value").trim();
                                    if (value.isEmpty()) {
                                        context.getSource().sendSuccess(() -> Component.literal("team name cannot be empty"), false);
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    ArrayList<String> values = new ArrayList<>(configValue.get().stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
                                    if (!values.contains(value)) {
                                        values.add(value);
                                        configValue.set(values);
                                    }
                                    context.getSource().sendSuccess(() -> Component.literal(name + ": " + listToText(configValue.get())), false);
                                    return Command.SINGLE_SUCCESS;
                                }))
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("value", StringArgumentType.word())
                                .executes(context -> {
                                    String value = StringArgumentType.getString(context, "value").trim();
                                    ArrayList<String> values = new ArrayList<>(configValue.get().stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
                                    values.removeIf(s -> s.equals(value));
                                    configValue.set(values);
                                    context.getSource().sendSuccess(() -> Component.literal(name + ": " + listToText(configValue.get())), false);
                                    return Command.SINGLE_SUCCESS;
                                }))
                )
                .then(Commands.literal("reset")
                        .executes(context -> {
                            String previousValue = listToText(configValue.get());
                            configValue.set(List.copyOf(configValue.getDefault()));
                            context.getSource().sendSuccess(() -> Component.literal(name + " with value: " + previousValue + " reset to default: " + listToText(configValue.get())), false);
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal(name + ": " + listToText(configValue.get())), false);
                    return Command.SINGLE_SUCCESS;
                });
    }

    private static String listToText(List<? extends String> values) {
        if (values.isEmpty()) {
            return "(none)";
        }
        return values.stream().collect(Collectors.joining(", "));
    }
}
