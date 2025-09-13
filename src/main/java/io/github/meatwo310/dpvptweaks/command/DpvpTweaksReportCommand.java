package io.github.meatwo310.dpvptweaks.command;

import com.google.gson.Gson;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import io.github.meatwo310.dpvptweaks.config.CommonConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

import java.time.Instant;

public class DpvpTweaksReportCommand {
    public static final Logger LOGGER = LogUtils.getLogger();

    static void register(LiteralArgumentBuilder<CommandSourceStack> builder, RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("report")
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(DpvpTweaksReportCommand::report)
                        )
                )
        );
        dispatcher.register(Commands.literal("reporthistory")
                .requires(s -> s.hasPermission(2))
                .executes(DpvpTweaksReportCommand::reporthistory)
        );
    }

    private static int report(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var source = ctx.getSource();

        var reporter = source.getPlayerOrException();
        var reporterUUID = reporter.getUUID();
        var reported = GameProfileArgument.getGameProfiles(ctx, "player").stream().findFirst().orElseThrow();
        var reportedUUID = reported.getId();
        var reason = StringArgumentType.getString(ctx, "reason");

        var report = new Report(
                Instant.now().getEpochSecond(),
                reporter.getDisplayName().getString(),
                reporterUUID,
                reported.getName(),
                reportedUUID,
                reason
        );
        CommonConfig.addReport(report);

        var msg = Component
                .literal("プレイヤー §e%s§r を通報しました。ご協力ありがとうございます。".formatted(reported.getName()))
                .withStyle(ChatFormatting.GREEN);
        source.sendSuccess(() -> msg, false);

        var broadcastMsg = Component.empty()
                .append(Component.literal("[プレイヤー通報]\n").withStyle(ChatFormatting.RED))
                .append(Component.literal(report.toStringWithoutTimestamp()));

        var playerList = source.getServer().getPlayerList();
        playerList.getPlayers().stream()
                .filter(player -> playerList.isOp(player.getGameProfile()))
                .forEach(player -> player.sendSystemMessage(broadcastMsg));

        return Command.SINGLE_SUCCESS;
    }

    private static int reporthistory(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var reports = CommonConfig.REPORTS.get();

        if (reports.isEmpty()) {
            source.sendSuccess(() -> Component.literal("通報履歴はありません。"), false);
            return Command.SINGLE_SUCCESS;
        }

        var header = Component
                .literal("========== 通報履歴 ==========")
                .withStyle(ChatFormatting.GOLD);
        source.sendSuccess(() -> header, false);

        for (var json : reports) {
            try {
                var report = new Gson().fromJson(json, Report.class);
                var message = report.toString();
                source.sendSuccess(() -> Component.literal(message), false);
            } catch (Exception e) {
                LOGGER.error("Failed to parse report JSON: {}", json, e);
                var failedToParse = Component
                        .literal("不明なJSONデータ: スキップ")
                        .withStyle(ChatFormatting.DARK_RED);
                source.sendSuccess(() -> failedToParse, false);
            }
        }

        return Command.SINGLE_SUCCESS;
    }
}
