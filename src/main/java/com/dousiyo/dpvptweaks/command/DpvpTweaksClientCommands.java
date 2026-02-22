package com.dousiyo.dpvptweaks.command;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.config.ClientConfig;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class DpvpTweaksClientCommands {
    private static final List<String> THEMES = Arrays.stream(ClientConfig.LoadoutThemeMode.values())
            .map(mode -> mode.name().toLowerCase(Locale.ROOT))
            .toList();

    @SubscribeEvent
    public static void registerClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("loadout")
                        .executes(ctx -> {
                            sendClientMessage("Current theme: " + ClientConfig.LOADOUT_THEME_MODE.get().name().toLowerCase(Locale.ROOT));
                            sendClientMessage("Available: " + String.join(", ", THEMES));
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("theme", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(THEMES, builder))
                                .executes(ctx -> {
                                    String raw = StringArgumentType.getString(ctx, "theme");
                                    ClientConfig.LoadoutThemeMode mode = parseTheme(raw);
                                    if (mode == null) {
                                        sendClientMessage("Invalid theme: " + raw);
                                        sendClientMessage("Available: " + String.join(", ", THEMES));
                                        return 0;
                                    }
                                    ClientConfig.LOADOUT_THEME_MODE.set(mode);
                                    sendClientMessage("Loadout theme set to: " + mode.name().toLowerCase(Locale.ROOT));
                                    return Command.SINGLE_SUCCESS;
                                }))
        );
    }

    private static ClientConfig.LoadoutThemeMode parseTheme(String value) {
        try {
            return ClientConfig.LoadoutThemeMode.valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void sendClientMessage(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("[loadout] " + text), false);
        }
    }
}

