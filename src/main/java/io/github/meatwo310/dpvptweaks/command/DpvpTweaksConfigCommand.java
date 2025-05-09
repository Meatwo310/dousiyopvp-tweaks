package io.github.meatwo310.dpvptweaks.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.meatwo310.dpvptweaks.config.ServerConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.RegisterCommandsEvent;

public class DpvpTweaksConfigCommand {
    static void register(LiteralArgumentBuilder<CommandSourceStack> builder, RegisterCommandsEvent event) {
        builder.then(Commands.literal("config")
                .requires(s -> s.hasPermission(2))
                .then(DpvpTweaksConfigCommand.createConfigCommand("valine1g", ServerConfig.VALINE1G_DAMAGE, ServerConfig.VALINE1G_COOLDOWN))
                .then(DpvpTweaksConfigCommand.createConfigCommand("valine2g", ServerConfig.VALINE2G_DAMAGE, ServerConfig.VALINE2G_COOLDOWN))
                .then(DpvpTweaksConfigCommand.createConfigCommand("valine3g", ServerConfig.VALINE3G_DAMAGE, ServerConfig.VALINE3G_COOLDOWN))
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
        );
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
