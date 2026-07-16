package com.dousiyo.dpvptweaks.command;

import com.dousiyo.dpvptweaks.content.ContentService;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;

final class DpvpTweaksContentCommand {
    private DpvpTweaksContentCommand() {}

    static void register(LiteralArgumentBuilder<CommandSourceStack> root, RegisterCommandsEvent ignored) {
        var content = Commands.literal("content").requires(source -> source.hasPermission(2));
        content.then(Commands.literal("reload").executes(ctx -> reload(ctx.getSource())));

        var announcement = Commands.literal("announcement");
        announcement.then(Commands.literal("create").then(Commands.argument("audience", StringArgumentType.word())
                .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(new String[]{"all", "op2", "op3", "op4", "rank"}, builder))
                .then(Commands.argument("importance", StringArgumentType.word())
                        .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(new String[]{"normal", "important", "critical"}, builder))
                        .then(Commands.argument("title", StringArgumentType.string())
                                .then(Commands.argument("markdown", StringArgumentType.greedyString()).executes(ctx -> createAnnouncement(
                                        ctx.getSource(), StringArgumentType.getString(ctx, "audience"), StringArgumentType.getString(ctx, "importance"),
                                        StringArgumentType.getString(ctx, "title"), StringArgumentType.getString(ctx, "markdown"))))))));
        announcement.then(Commands.literal("edit").then(Commands.argument("id", IntegerArgumentType.integer(1))
                .then(Commands.argument("title", StringArgumentType.string())
                        .then(Commands.argument("markdown", StringArgumentType.greedyString()).executes(ctx -> editAnnouncement(
                                ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id"), StringArgumentType.getString(ctx, "title"),
                                StringArgumentType.getString(ctx, "markdown")))))));
        announcement.then(Commands.literal("delete").then(Commands.argument("id", IntegerArgumentType.integer(1))
                .executes(ctx -> deleteAnnouncement(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id")))));
        content.then(announcement);

        var rule = Commands.literal("rule");
        rule.then(Commands.literal("set").then(Commands.argument("mode", StringArgumentType.word())
                .then(Commands.argument("title", StringArgumentType.string())
                        .then(Commands.argument("markdown", StringArgumentType.greedyString()).executes(ctx -> setRule(
                                ctx.getSource(), StringArgumentType.getString(ctx, "mode"), StringArgumentType.getString(ctx, "title"),
                                StringArgumentType.getString(ctx, "markdown")))))));
        rule.then(Commands.literal("delete").then(Commands.argument("mode", StringArgumentType.word())
                .executes(ctx -> deleteRule(ctx.getSource(), StringArgumentType.getString(ctx, "mode")))));
        content.then(rule);
        root.then(content);
    }

    private static int reload(CommandSourceStack source) {
        ContentService.reload();
        return success(source, "お知らせ・ルールを再読み込みしました");
    }

    private static int createAnnouncement(CommandSourceStack source, String audienceText, String importanceText, String title, String markdown) {
        try {
            String normalizedAudience = audienceText.toLowerCase(java.util.Locale.ROOT);
            ContentService.Audience audience = normalizedAudience.startsWith("op")
                    ? ContentService.Audience.OP : ContentService.Audience.valueOf(normalizedAudience.toUpperCase(java.util.Locale.ROOT));
            int minimumOpLevel = audience == ContentService.Audience.OP
                    ? Integer.parseInt(normalizedAudience.substring(2)) : 0;
            if (audience == ContentService.Audience.OP && (minimumOpLevel < 1 || minimumOpLevel > 4))
                throw new IllegalArgumentException("OP条件は op1～op4 で指定してください");
            ContentService.Importance importance = ContentService.Importance.valueOf(importanceText.toUpperCase(java.util.Locale.ROOT));
            int id = ContentService.createAnnouncement(title, audience, minimumOpLevel, importance, source.getTextName(), commandMarkdown(markdown));
            return success(source, "お知らせ #" + id + " を作成しました");
        } catch (Exception e) { return failure(source, e); }
    }

    private static int editAnnouncement(CommandSourceStack source, int id, String title, String markdown) {
        try {
            if (!ContentService.editAnnouncement(id, title, source.getTextName(), commandMarkdown(markdown))) return missing(source);
            return success(source, "お知らせ #" + id + " を更新しました");
        } catch (Exception e) { return failure(source, e); }
    }

    private static int deleteAnnouncement(CommandSourceStack source, int id) {
        try {
            if (!ContentService.deleteAnnouncement(id)) return missing(source);
            return success(source, "お知らせ #" + id + " を削除しました（IDは再利用されません）");
        } catch (Exception e) { return failure(source, e); }
    }

    private static int setRule(CommandSourceStack source, String mode, String title, String markdown) {
        try {
            ContentService.setRule(mode, title, source.getTextName(), commandMarkdown(markdown));
            return success(source, "ルール " + mode + " を保存しました");
        } catch (Exception e) { return failure(source, e); }
    }

    private static int deleteRule(CommandSourceStack source, String mode) {
        try {
            if (!ContentService.deleteRule(mode)) return missing(source);
            return success(source, "ルール " + mode + " を削除しました");
        } catch (Exception e) { return failure(source, e); }
    }

    private static int success(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int missing(CommandSourceStack source) {
        source.sendFailure(Component.literal("対象が見つかりません"));
        return 0;
    }

    private static int failure(CommandSourceStack source, Exception error) {
        source.sendFailure(Component.literal("操作に失敗しました: " + error.getMessage()));
        return 0;
    }

    private static String commandMarkdown(String value) {
        return value == null ? "" : value.replace("\\n", "\n");
    }
}
