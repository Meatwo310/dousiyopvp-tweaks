package com.dousiyo.dpvptweaks.pvpstats.command;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.pvpstats.model.PvpStatKey;
import com.dousiyo.dpvptweaks.pvpstats.model.StatsGuiPayload;
import com.dousiyo.dpvptweaks.pvpstats.network.PvpStatsNetwork;
import com.dousiyo.dpvptweaks.pvpstats.network.s2c.OpenStatsGuiPacket;
import com.dousiyo.dpvptweaks.pvpstats.service.ModeIdService;
import com.dousiyo.dpvptweaks.pvpstats.service.MatchIdService;
import com.dousiyo.dpvptweaks.pvpstats.service.PvpStatsImportService;
import com.dousiyo.dpvptweaks.pvpstats.service.PvpStatsQueryService;
import com.dousiyo.dpvptweaks.pvpstats.util.SavedDataAccessor;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

public final class PvpStatsCommand {
    private PvpStatsCommand() {
    }

    @Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Registrar {
        private Registrar() {
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            event.getDispatcher().register(buildRoot());
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildRoot() {
        return Commands.literal("pvpstats")
                .executes(PvpStatsCommand::openOwn)
                .then(Commands.literal("open")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(PvpStatsCommand::openTarget)))
                .then(Commands.literal("import_bundle")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("mode_id", StringArgumentType.word())
                                .then(Commands.argument("wins_obj", ObjectiveArgument.objective())
                                        .then(Commands.argument("losses_obj", ObjectiveArgument.objective())
                                                .then(Commands.argument("kills_obj", ObjectiveArgument.objective())
                                                        .then(Commands.argument("deaths_obj", ObjectiveArgument.objective())
                                                                .executes(PvpStatsCommand::importBundle)))))))
                .then(Commands.literal("import_match")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("mode_id", StringArgumentType.word())
                                .then(Commands.argument("winner_team", TeamArgument.team())
                                        .then(Commands.argument("loser_team", TeamArgument.team())
                                                .then(Commands.argument("kills_obj", ObjectiveArgument.objective())
                                                        .then(Commands.argument("deaths_obj", ObjectiveArgument.objective())
                                                                .then(Commands.argument("match_id", StringArgumentType.word())
                                                                        .executes(PvpStatsCommand::importMatchWithId))))))))
                .then(Commands.literal("import_draw")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("mode_id", StringArgumentType.word())
                                .then(Commands.argument("team_a", TeamArgument.team())
                                        .then(Commands.argument("team_b", TeamArgument.team())
                                                .then(Commands.argument("kills_obj", ObjectiveArgument.objective())
                                                        .then(Commands.argument("deaths_obj", ObjectiveArgument.objective())
                                                                .then(Commands.argument("match_id", StringArgumentType.word())
                                                                        .executes(PvpStatsCommand::importDrawWithId))))))))
                .then(Commands.literal("import_objective")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("objective", ObjectiveArgument.objective())
                                .then(Commands.argument("mode_id", StringArgumentType.word())
                                        .then(Commands.argument("stat_key", StringArgumentType.word())
                                                .executes(PvpStatsCommand::importObjective)))))
                .then(Commands.literal("achievement")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("grant")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.literal("debugger")
                                                .executes(ctx -> updateAchievement(ctx, "debugger", "デバッカー", true)))
                                        .then(Commands.literal("supporter")
                                                .executes(ctx -> updateAchievement(ctx, "supporter", "サポーター", true)))))
                        .then(Commands.literal("revoke")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.literal("debugger")
                                                .executes(ctx -> updateAchievement(ctx, "debugger", "デバッカー", false)))
                                        .then(Commands.literal("supporter")
                                                .executes(ctx -> updateAchievement(ctx, "supporter", "サポーター", false))))))
                .then(Commands.literal("reset")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(PvpStatsCommand::resetPlayer)));
    }

    private static int updateAchievement(CommandContext<CommandSourceStack> ctx, String achievementId,
                                         String achievementName, boolean grant)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        var savedData = SavedDataAccessor.get(ctx.getSource().getLevel());
        boolean changed = grant
                ? savedData.awardBadge(target.getUUID(), achievementId)
                : savedData.revokeBadge(target.getUUID(), achievementId);
        String action = grant ? "付与" : "取り消し";
        if (!changed) {
            ctx.getSource().sendFailure(Component.literal(
                    "実績「" + achievementName + "」は既に" + (grant ? "付与済み" : "未付与") + "です: "
                            + target.getGameProfile().getName()));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                "実績「" + achievementName + "」を" + action + "しました: "
                        + target.getGameProfile().getName()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int openOwn(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        openStatsScreen(player, player.getUUID(), player.getGameProfile().getName());
        return Command.SINGLE_SUCCESS;
    }

    private static int openTarget(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer viewer = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        openStatsScreen(viewer, target.getUUID(), target.getGameProfile().getName());
        return Command.SINGLE_SUCCESS;
    }

    private static int importBundle(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        String modeId = StringArgumentType.getString(ctx, "mode_id");
        if (!ModeIdService.isValid(modeId)) {
            ctx.getSource().sendFailure(Component.literal("mode_id が不正です"));
            return 0;
        }

        Objective winsObjective = ObjectiveArgument.getObjective(ctx, "wins_obj");
        Objective lossesObjective = ObjectiveArgument.getObjective(ctx, "losses_obj");
        Objective killsObjective = ObjectiveArgument.getObjective(ctx, "kills_obj");
        Objective deathsObjective = ObjectiveArgument.getObjective(ctx, "deaths_obj");
        int imported = PvpStatsImportService.importBundle(
                ctx.getSource().getLevel(),
                ModeIdService.normalize(modeId),
                winsObjective,
                lossesObjective,
                killsObjective,
                deathsObjective
        );

        ctx.getSource().sendSuccess(() -> Component.literal("PvP戦績を " + imported + " 件取り込みました"), true);
        return imported > 0 ? Command.SINGLE_SUCCESS : 0;
    }

    private static int importMatch(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return importMatch(ctx, null);
    }

    private static int importMatchWithId(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        String rawMatchId = StringArgumentType.getString(ctx, "match_id");
        if (!MatchIdService.isValid(rawMatchId)) {
            ctx.getSource().sendFailure(Component.literal("match_id が不正です"));
            return 0;
        }
        return importMatch(ctx, MatchIdService.normalize(rawMatchId));
    }

    private static int importMatch(CommandContext<CommandSourceStack> ctx, String matchId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        String modeId = StringArgumentType.getString(ctx, "mode_id");
        if (!ModeIdService.isValid(modeId)) {
            ctx.getSource().sendFailure(Component.literal("mode_id が不正です"));
            return 0;
        }

        PlayerTeam winnerTeam = TeamArgument.getTeam(ctx, "winner_team");
        PlayerTeam loserTeam = TeamArgument.getTeam(ctx, "loser_team");
        Objective killsObjective = ObjectiveArgument.getObjective(ctx, "kills_obj");
        Objective deathsObjective = ObjectiveArgument.getObjective(ctx, "deaths_obj");

        int imported = PvpStatsImportService.importMatchResult(
                ctx.getSource().getLevel(),
                ModeIdService.normalize(modeId),
                winnerTeam,
                loserTeam,
                killsObjective,
                deathsObjective,
                matchId
        );

        if (imported == PvpStatsImportService.DUPLICATE_MATCH) {
            ctx.getSource().sendFailure(Component.literal("登録済みの match_id です: " + matchId));
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.literal(
                "試合結果を " + imported + " 件取り込みました"
                        + " (winner=" + winnerTeam.getName()
                        + ", loser=" + loserTeam.getName()
                        + (matchId == null ? "" : ", match_id=" + matchId) + ")"
        ), true);
        return imported > 0 ? Command.SINGLE_SUCCESS : 0;
    }

    private static int importDraw(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return importDraw(ctx, null);
    }

    private static int importDrawWithId(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        String rawMatchId = StringArgumentType.getString(ctx, "match_id");
        if (!MatchIdService.isValid(rawMatchId)) {
            ctx.getSource().sendFailure(Component.literal("match_id が不正です"));
            return 0;
        }
        return importDraw(ctx, MatchIdService.normalize(rawMatchId));
    }

    private static int importDraw(CommandContext<CommandSourceStack> ctx, String matchId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        String modeId = StringArgumentType.getString(ctx, "mode_id");
        if (!ModeIdService.isValid(modeId)) {
            ctx.getSource().sendFailure(Component.literal("mode_id が不正です"));
            return 0;
        }

        PlayerTeam teamA = TeamArgument.getTeam(ctx, "team_a");
        PlayerTeam teamB = TeamArgument.getTeam(ctx, "team_b");
        Objective killsObjective = ObjectiveArgument.getObjective(ctx, "kills_obj");
        Objective deathsObjective = ObjectiveArgument.getObjective(ctx, "deaths_obj");

        int imported = PvpStatsImportService.importDrawResult(
                ctx.getSource().getLevel(),
                ModeIdService.normalize(modeId),
                teamA,
                teamB,
                killsObjective,
                deathsObjective,
                matchId
        );

        if (imported == PvpStatsImportService.DUPLICATE_MATCH) {
            ctx.getSource().sendFailure(Component.literal("登録済みの match_id です: " + matchId));
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.literal(
                "引き分け結果を " + imported + " 件取り込みました"
                        + " (teamA=" + teamA.getName()
                        + ", teamB=" + teamB.getName()
                        + (matchId == null ? "" : ", match_id=" + matchId) + ")"
        ), true);
        return imported > 0 ? Command.SINGLE_SUCCESS : 0;
    }

    private static int importObjective(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        String modeId = StringArgumentType.getString(ctx, "mode_id");
        if (!ModeIdService.isValid(modeId)) {
            ctx.getSource().sendFailure(Component.literal("mode_id が不正です"));
            return 0;
        }

        PvpStatKey statKey = PvpStatKey.fromString(StringArgumentType.getString(ctx, "stat_key"));
        if (statKey == null) {
            ctx.getSource().sendFailure(Component.literal("stat_key は wins/losses/draws/kills/deaths のいずれかを指定してください"));
            return 0;
        }

        Objective objective = ObjectiveArgument.getObjective(ctx, "objective");
        int imported = PvpStatsImportService.importObjective(ctx.getSource().getLevel(), ModeIdService.normalize(modeId), objective, statKey);
        ctx.getSource().sendSuccess(() -> Component.literal("PvP戦績を " + imported + " 件取り込みました"), true);
        return imported > 0 ? Command.SINGLE_SUCCESS : 0;
    }

    private static int resetPlayer(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        var savedData = SavedDataAccessor.get(ctx.getSource().getLevel());
        boolean removed = savedData.remove(target.getUUID());
        if (removed) {
            savedData.setDirty();
        }
        ctx.getSource().sendSuccess(() -> Component.literal("戦績をリセットしました: " + target.getGameProfile().getName()), true);
        return removed ? Command.SINGLE_SUCCESS : 0;
    }

    private static void openStatsScreen(ServerPlayer viewer, java.util.UUID targetUuid, String fallbackName) {
        StatsGuiPayload payload = PvpStatsQueryService.query(viewer.serverLevel(), viewer.getUUID(), targetUuid, fallbackName);
        PvpStatsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), new OpenStatsGuiPacket(payload));
    }
}
