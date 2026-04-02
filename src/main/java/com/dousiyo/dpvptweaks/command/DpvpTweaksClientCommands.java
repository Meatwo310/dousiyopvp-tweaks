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
                            sendClientMessage("loadout", "Current theme: " + ClientConfig.LOADOUT_THEME_MODE.get().name().toLowerCase(Locale.ROOT));
                            sendClientMessage("loadout", "Available: " + String.join(", ", THEMES));
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("theme", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(THEMES, builder))
                                .executes(ctx -> {
                                    String raw = StringArgumentType.getString(ctx, "theme");
                                    ClientConfig.LoadoutThemeMode mode = parseTheme(raw);
                                    if (mode == null) {
                                        sendClientMessage("loadout", "Invalid theme: " + raw);
                                        sendClientMessage("loadout", "Available: " + String.join(", ", THEMES));
                                        return 0;
                                    }
                                    ClientConfig.LOADOUT_THEME_MODE.set(mode);
                                    sendClientMessage("loadout", "Loadout theme set to: " + mode.name().toLowerCase(Locale.ROOT));
                                    return Command.SINGLE_SUCCESS;
                                }))
        );

        event.getDispatcher().register(
                Commands.literal("capturehud")
                        .executes(ctx -> {
                            sendCaptureHudStatus();
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.literal("on")
                                .executes(ctx -> setCaptureHudEnabled(true)))
                        .then(Commands.literal("off")
                                .executes(ctx -> setCaptureHudEnabled(false)))
                        .then(Commands.literal("toggle")
                                .executes(ctx -> setCaptureHudEnabled(!isCaptureHudEnabled())))
        );
    }

    private static ClientConfig.LoadoutThemeMode parseTheme(String value) {
        try {
            return ClientConfig.LoadoutThemeMode.valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean isCaptureHudEnabled() {
        return ClientConfig.CAPTURE_SHOW_OVERVIEW_HUD.get() || ClientConfig.CAPTURE_SHOW_FOCUS_HUD.get();
    }

    private static int setCaptureHudEnabled(boolean enabled) {
        ClientConfig.CAPTURE_SHOW_OVERVIEW_HUD.set(enabled);
        ClientConfig.CAPTURE_SHOW_FOCUS_HUD.set(enabled);
        sendClientMessage("capturehud", "Capture HUD: " + (enabled ? "on" : "off"));
        return Command.SINGLE_SUCCESS;
    }

    private static void sendCaptureHudStatus() {
        sendClientMessage("capturehud", "Overview HUD: " + onOff(ClientConfig.CAPTURE_SHOW_OVERVIEW_HUD.get()));
        sendClientMessage("capturehud", "Focus HUD: " + onOff(ClientConfig.CAPTURE_SHOW_FOCUS_HUD.get()));
    }

    private static String onOff(boolean value) {
        return value ? "on" : "off";
    }

    private static void sendClientMessage(String tag, String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("[" + tag + "] " + text), false);
        }
    }
}