package com.dousiyo.dpvptweaks.timer.command;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.timer.core.TimerDefinition;
import com.dousiyo.dpvptweaks.timer.core.TimerInstance;
import com.dousiyo.dpvptweaks.timer.core.TimerManager;
import com.dousiyo.dpvptweaks.timer.core.TimerMode;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TimerCommand {
    private TimerCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(buildRoot());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildRoot() {
        return Commands.literal("timer")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("define")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.literal("countdown")
                                        .then(Commands.argument("durationSeconds", IntegerArgumentType.integer(1))
                                                .executes(ctx -> define(ctx, TimerMode.COUNTDOWN, true, false))
                                                .then(Commands.argument("title", StringArgumentType.greedyString())
                                                        .executes(ctx -> define(ctx, TimerMode.COUNTDOWN, true, true)))))
                                .then(Commands.literal("countup")
                                        .executes(ctx -> define(ctx, TimerMode.COUNTUP, false, false))
                                        .then(Commands.argument("durationSeconds", IntegerArgumentType.integer(0))
                                                .executes(ctx -> define(ctx, TimerMode.COUNTUP, true, false))
                                                .then(Commands.argument("title", StringArgumentType.greedyString())
                                                        .executes(ctx -> define(ctx, TimerMode.COUNTUP, true, true))))
                                        .then(Commands.argument("title", StringArgumentType.greedyString())
                                                .executes(ctx -> define(ctx, TimerMode.COUNTUP, false, true))))))
                .then(Commands.literal("show")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(TimerCommand::show))))
                .then(Commands.literal("hide")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(TimerCommand::hide)))
                .then(Commands.literal("start")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(TimerCommand::start))))
                .then(Commands.literal("pause")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(TimerCommand::pause)))
                .then(Commands.literal("resume")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(TimerCommand::resume)))
                .then(Commands.literal("stop")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(TimerCommand::stop)))
                .then(Commands.literal("reset")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(TimerCommand::reset))))
                .then(Commands.literal("set")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(TimerCommand::set))))
                .then(Commands.literal("title")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("title", StringArgumentType.greedyString())
                                        .executes(TimerCommand::title))))
                .then(Commands.literal("info")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(TimerCommand::info)))
                .then(Commands.literal("remove")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(TimerCommand::remove)))
                .then(Commands.literal("onfinish")
                        .then(Commands.literal("set")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.argument("command", StringArgumentType.greedyString())
                                                .executes(TimerCommand::onFinishSet))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.argument("command", StringArgumentType.greedyString())
                                                .executes(TimerCommand::onFinishAdd))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(TimerCommand::onFinishClear)))
                        .then(Commands.literal("list")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(TimerCommand::onFinishList))))
                .then(Commands.literal("finishmessage")
                        .then(Commands.literal("set")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                                .executes(TimerCommand::finishMessageSet))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(TimerCommand::finishMessageClear)))
                        .then(Commands.literal("get")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(TimerCommand::finishMessageGet))));
    }

    private static int define(CommandContext<CommandSourceStack> ctx, TimerMode mode, boolean hasDuration, boolean hasTitle) {
        String id = StringArgumentType.getString(ctx, "id");
        int durationSeconds = hasDuration ? IntegerArgumentType.getInteger(ctx, "durationSeconds") : 0;
        if (mode == TimerMode.COUNTDOWN && !hasDuration) {
            ctx.getSource().sendFailure(Component.literal("countdown では durationSeconds が必須です"));
            return 0;
        }

        String rawTitle = hasTitle ? StringArgumentType.getString(ctx, "title") : "";
        Component title = rawTitle.isBlank() ? null : Component.literal(rawTitle);

        TimerManager manager = TimerManager.get(ctx.getSource().getLevel());
        manager.define(id, mode, durationSeconds * 20, title);
        String titleText = title == null ? "(なし)" : title.getString();
        ctx.getSource().sendSuccess(
                () -> Component.literal("タイマーを定義しました: id=" + id + ", mode=" + mode.name().toLowerCase()
                        + ", duration=" + durationSeconds + "秒, title=" + titleText),
                true
        );
        return 1;
    }

    private static int show(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "id");
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
        TimerManager manager = TimerManager.get(ctx.getSource().getLevel());
        int success = 0;
        for (ServerPlayer player : players) {
            try {
                manager.show(player.getUUID(), id, player.serverLevel());
                success++;
            } catch (RuntimeException e) {
                sendPlayerFailure(ctx.getSource(), player, e.getMessage());
            }
        }
        final int affected = success;
        ctx.getSource().sendSuccess(() -> Component.literal("表示対象を設定しました: id=" + id + ", 対象人数=" + affected), true);
        return success;
    }

    private static int hide(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
        TimerManager manager = TimerManager.get(ctx.getSource().getLevel());
        for (ServerPlayer player : players) {
            manager.hide(player.getUUID(), player.serverLevel());
        }
        ctx.getSource().sendSuccess(() -> Component.literal("HUDを非表示にしました: 対象人数=" + players.size()), true);
        return players.size();
    }

    private static int start(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "id");
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
        TimerManager manager = TimerManager.get(ctx.getSource().getLevel());
        int success = 0;
        for (ServerPlayer player : players) {
            try {
                manager.start(player.getUUID(), id, player.serverLevel());
                success++;
            } catch (RuntimeException e) {
                sendPlayerFailure(ctx.getSource(), player, e.getMessage());
            }
        }
        final int affected = success;
        ctx.getSource().sendSuccess(() -> Component.literal("タイマーを開始しました: id=" + id + ", 対象人数=" + affected), true);
        return success;
    }

    private static int pause(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
        TimerManager manager = TimerManager.get(ctx.getSource().getLevel());
        int success = 0;
        for (ServerPlayer player : players) {
            try {
                manager.pause(player.getUUID(), player.serverLevel());
                success++;
            } catch (RuntimeException e) {
                sendPlayerFailure(ctx.getSource(), player, e.getMessage());
            }
        }
        final int affected = success;
        ctx.getSource().sendSuccess(() -> Component.literal("一時停止しました: 対象人数=" + affected), true);
        return success;
    }

    private static int resume(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
        TimerManager manager = TimerManager.get(ctx.getSource().getLevel());
        int success = 0;
        for (ServerPlayer player : players) {
            try {
                manager.resume(player.getUUID(), player.serverLevel());
                success++;
            } catch (RuntimeException e) {
                sendPlayerFailure(ctx.getSource(), player, e.getMessage());
            }
        }
        final int affected = success;
        ctx.getSource().sendSuccess(() -> Component.literal("再開しました: 対象人数=" + affected), true);
        return success;
    }

    private static int stop(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
        TimerManager manager = TimerManager.get(ctx.getSource().getLevel());
        int success = 0;
        for (ServerPlayer player : players) {
            try {
                manager.stop(player.getUUID(), player.serverLevel());
                success++;
            } catch (RuntimeException e) {
                sendPlayerFailure(ctx.getSource(), player, e.getMessage());
            }
        }
        final int affected = success;
        ctx.getSource().sendSuccess(() -> Component.literal("停止しました: 対象人数=" + affected), true);
        return success;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "id");
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
        TimerManager manager = TimerManager.get(ctx.getSource().getLevel());
        int success = 0;
        for (ServerPlayer player : players) {
            try {
                manager.reset(player.getUUID(), id, player.serverLevel());
                success++;
            } catch (RuntimeException e) {
                sendPlayerFailure(ctx.getSource(), player, e.getMessage());
            }
        }
        final int affected = success;
        ctx.getSource().sendSuccess(() -> Component.literal("リセットしました: id=" + id + ", 対象人数=" + affected), true);
        return success;
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
        TimerManager manager = TimerManager.get(ctx.getSource().getLevel());
        int success = 0;
        for (ServerPlayer player : players) {
            try {
                manager.setTime(player.getUUID(), seconds, player.serverLevel());
                success++;
            } catch (RuntimeException e) {
                sendPlayerFailure(ctx.getSource(), player, e.getMessage());
            }
        }
        final int affected = success;
        ctx.getSource().sendSuccess(() -> Component.literal("時間を設定しました: " + seconds + "秒, 対象人数=" + affected), true);
        return success;
    }

    private static int title(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        String title = StringArgumentType.getString(ctx, "title");
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
        TimerManager manager = TimerManager.get(ctx.getSource().getLevel());
        int success = 0;
        for (ServerPlayer player : players) {
            try {
                manager.setTitle(player.getUUID(), Component.literal(title), player.serverLevel());
                success++;
            } catch (RuntimeException e) {
                sendPlayerFailure(ctx.getSource(), player, e.getMessage());
            }
        }
        final int affected = success;
        ctx.getSource().sendSuccess(() -> Component.literal("タイトルを設定しました: \"" + title + "\", 対象人数=" + affected), true);
        return success;
    }

    private static int info(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        TimerManager manager = TimerManager.get(ctx.getSource().getLevel());
        int count = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "targets")) {
            String activeId = manager.getActiveHudTimer(player.getUUID());
            if (activeId == null) {
                ctx.getSource().sendSuccess(() -> Component.literal(player.getGameProfile().getName() + ": HUDに表示中のタイマーがありません"), false);
                count++;
                continue;
            }

            Optional<TimerInstance> optional = manager.getInstance(player.getUUID(), activeId);
            TimerInstance instance = optional.orElseGet(() -> manager.getOrCreateInstance(player.getUUID(), activeId));
            TimerDefinition definition = manager.getDefinition(activeId).orElse(null);
            if (definition == null) {
                ctx.getSource().sendSuccess(() -> Component.literal(player.getGameProfile().getName() + ": 表示中タイマーの定義が見つかりません"), false);
                count++;
                continue;
            }

            int seconds = instance.getCurrentTicks() / 20;
            String line = player.getGameProfile().getName() + ": id=" + activeId
                    + ", mode=" + definition.getMode()
                    + ", state=" + instance.getState()
                    + ", seconds=" + seconds;
            ctx.getSource().sendSuccess(() -> Component.literal(line), false);
            count++;
        }
        return count;
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        TimerManager manager = TimerManager.get(ctx.getSource().getLevel());
        manager.removeDefinition(id, ctx.getSource().getLevel());
        ctx.getSource().sendSuccess(() -> Component.literal("タイマー定義を削除しました: " + id), true);
        return 1;
    }

    private static int onFinishSet(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        String command = normalizeCommand(StringArgumentType.getString(ctx, "command"));
        TimerManager manager = TimerManager.get(ctx.getSource().getLevel());
        manager.setOnFinish(id, List.of(command));
        ctx.getSource().sendSuccess(() -> Component.literal("onfinish を設定しました: id=" + id + ", command=" + command), true);
        return 1;
    }

    private static int onFinishAdd(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        String command = normalizeCommand(StringArgumentType.getString(ctx, "command"));
        TimerManager manager = TimerManager.get(ctx.getSource().getLevel());
        manager.addOnFinish(id, command);
        ctx.getSource().sendSuccess(() -> Component.literal("onfinish に追加しました: id=" + id + ", command=" + command), true);
        return 1;
    }

    private static int onFinishClear(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        TimerManager manager = TimerManager.get(ctx.getSource().getLevel());
        manager.clearOnFinish(id);
        ctx.getSource().sendSuccess(() -> Component.literal("onfinish をクリアしました: id=" + id), true);
        return 1;
    }

    private static int onFinishList(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        TimerManager manager = TimerManager.get(ctx.getSource().getLevel());
        List<String> commands = manager.listOnFinish(id);
        if (commands.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal(id + " の onfinish は空です"), false);
            return 1;
        }

        ctx.getSource().sendSuccess(() -> Component.literal(id + " の onfinish コマンド一覧:"), false);
        for (int i = 0; i < commands.size(); i++) {
            int index = i;
            ctx.getSource().sendSuccess(() -> Component.literal((index + 1) + ". " + commands.get(index)), false);
        }
        return commands.size();
    }

    private static int finishMessageSet(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        String message = StringArgumentType.getString(ctx, "message");
        TimerManager manager = TimerManager.get(ctx.getSource().getLevel());
        manager.setFinishMessage(id, Component.literal(message));
        ctx.getSource().sendSuccess(() -> Component.literal("終了メッセージを設定しました: id=" + id + ", message=\"" + message + "\""), true);
        return 1;
    }

    private static int finishMessageClear(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        TimerManager manager = TimerManager.get(ctx.getSource().getLevel());
        manager.clearFinishMessage(id);
        ctx.getSource().sendSuccess(() -> Component.literal("終了メッセージを初期値に戻しました: id=" + id), true);
        return 1;
    }

    private static int finishMessageGet(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        TimerManager manager = TimerManager.get(ctx.getSource().getLevel());
        Component message = manager.getFinishMessage(id);
        ctx.getSource().sendSuccess(() -> Component.literal(id + " の終了メッセージ: ").append(message), false);
        return 1;
    }

    private static void sendPlayerFailure(CommandSourceStack source, ServerPlayer player, String reason) {
        source.sendFailure(Component.literal(player.getGameProfile().getName() + ": " + reason));
    }

    private static String normalizeCommand(String command) {
        String trimmed = command.trim();
        if (trimmed.startsWith("/")) {
            return trimmed.substring(1);
        }
        return trimmed;
    }
}
