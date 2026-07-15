package com.dousiyo.dpvptweaks.command;

import com.dousiyo.dpvptweaks.arsenal.ArsenalConfig;
import com.dousiyo.dpvptweaks.arsenal.ArsenalMatchManager;
import com.dousiyo.dpvptweaks.arsenal.ArsenalWeaponSetManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class DpvpTweaksArsenalCommand {
    private DpvpTweaksArsenalCommand() {}

    static void register(LiteralArgumentBuilder<CommandSourceStack> root, RegisterCommandsEvent ignored) {
        root.then(build("arsenal"));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildDirect() { return build("arsenal"); }

    private static LiteralArgumentBuilder<CommandSourceStack> build(String name) {
        var command = Commands.literal(name).requires(source -> source.hasPermission(2)).executes(context -> {
            ArsenalMatchManager.openAdmin(context.getSource().getPlayerOrException(), "", "");
            return 1;
        });
        command.then(Commands.literal("start").then(Commands.argument("weapon_set", StringArgumentType.word())
                .executes(context -> reply(context, ArsenalMatchManager.start(context.getSource().getServer(),
                        StringArgumentType.getString(context, "weapon_set"))))));
        command.then(Commands.literal("stop").executes(context -> reply(context,
                ArsenalMatchManager.stop(context.getSource().getServer()))));
        command.then(Commands.literal("reset").executes(context -> reply(context,
                ArsenalMatchManager.reset(context.getSource().getServer()))));
        command.then(Commands.literal("status").executes(context -> {
            context.getSource().sendSuccess(() -> Component.literal(ArsenalMatchManager.status(context.getSource().getServer())), false);
            return 1;
        }));
        command.then(Commands.literal("reload").executes(context -> {
            ArsenalConfig.reload(); ArsenalWeaponSetManager.reload();
            var validation = ArsenalConfig.validate(context.getSource().getServer());
            if (!validation.valid()) { context.getSource().sendFailure(Component.literal(validation.error())); return 0; }
            context.getSource().sendSuccess(() -> Component.literal("アーセナル設定を再読み込みしました"), true);
            return 1;
        }));
        var weapons = Commands.literal("weapons");
        weapons.then(Commands.literal("list").executes(context -> {
            var entries = ArsenalWeaponSetManager.list();
            context.getSource().sendSuccess(() -> Component.literal(entries.isEmpty() ? "武器セットはありません" : String.join("\n", entries)), false);
            return entries.size();
        }));
        weapons.then(Commands.argument("weapon_set", StringArgumentType.word())
                .then(Commands.literal("validate").executes(context -> {
                    String id = StringArgumentType.getString(context, "weapon_set");
                    String error = ArsenalWeaponSetManager.validate(id);
                    if (error != null) { context.getSource().sendFailure(Component.literal(error)); return 0; }
                    context.getSource().sendSuccess(() -> Component.literal("有効な武器セットです: " + id), false); return 1;
                }))
                .then(Commands.literal("set")
                        .then(Commands.argument("stage", IntegerArgumentType.integer(1, 30))
                                .executes(context -> setHeld(context, 4))
                                .then(Commands.argument("reserve_magazines", IntegerArgumentType.integer(0, 256))
                                        .executes(context -> setHeld(context,
                                                IntegerArgumentType.getInteger(context, "reserve_magazines"))))))
                .then(Commands.literal("setall")
                        .executes(context -> setHeldAll(context, 4))
                        .then(Commands.argument("reserve_magazines", IntegerArgumentType.integer(0, 256))
                                .executes(context -> setHeldAll(context,
                                        IntegerArgumentType.getInteger(context, "reserve_magazines"))))));
        command.then(weapons);
        return command;
    }

    private static int setHeld(CommandContext<CommandSourceStack> context, int reserveMagazines) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String id = StringArgumentType.getString(context, "weapon_set");
            int stage = IntegerArgumentType.getInteger(context, "stage");
            ArsenalWeaponSetManager.setHeldWeapon(id, stage, reserveMagazines, player.getMainHandItem());
            context.getSource().sendSuccess(() -> Component.literal("武器セット" + id + "の第" + stage + "段階を登録しました"), true);
            return 1;
        } catch (Exception exception) {
            context.getSource().sendFailure(Component.literal("登録失敗: " + exception.getMessage()));
            return 0;
        }
    }

    private static int setHeldAll(CommandContext<CommandSourceStack> context, int reserveMagazines) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String id = StringArgumentType.getString(context, "weapon_set");
            ArsenalWeaponSetManager.setHeldWeaponAll(id, reserveMagazines, player.getMainHandItem());
            context.getSource().sendSuccess(() -> Component.literal(
                    "手持ち銃を武器セット" + id + "の全30段階へ登録しました（デバッグ用）"), true);
            return 1;
        } catch (Exception exception) {
            context.getSource().sendFailure(Component.literal("一括登録失敗: " + exception.getMessage()));
            return 0;
        }
    }

    private static int reply(CommandContext<CommandSourceStack> context, ArsenalMatchManager.ActionResult result) {
        if (result.success()) context.getSource().sendSuccess(() -> Component.literal(result.message()), true);
        else context.getSource().sendFailure(Component.literal(result.message()));
        return result.success() ? 1 : 0;
    }
}
