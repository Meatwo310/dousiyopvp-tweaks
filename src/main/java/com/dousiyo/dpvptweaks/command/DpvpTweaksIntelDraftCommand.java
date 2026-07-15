package com.dousiyo.dpvptweaks.command;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.inteldraft.IntelDraftDefinitionLoader;
import com.dousiyo.dpvptweaks.inteldraft.IntelDraftManager;
import com.tacz.guns.api.item.IGun;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.io.IOException;
import java.util.Collection;

public class DpvpTweaksIntelDraftCommand {
    static void register(LiteralArgumentBuilder<CommandSourceStack> root, RegisterCommandsEvent ignored) {
        var draft = Commands.literal("inteldraft").requires(s -> s.hasPermission(2));
        draft.then(targeted("open", IntelDraftManager::openDebug));
        draft.then(addGunCommand());
        root.then(draft);
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildDirectCommand() {
        return Commands.literal("inteldraft")
                .requires(source -> source.hasPermission(2))
                .then(addGunCommand())
                .executes(context -> IntelDraftManager.openDebug(context.getSource().getPlayerOrException()) ? 1 : 0);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> addGunCommand() {
        return Commands.literal("addgun").executes(DpvpTweaksIntelDraftCommand::addHeldGun);
    }

    private static int addHeldGun(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        var stack = player.getMainHandItem();
        var gun = IGun.getIGunOrNull(stack);
        if (gun == null || gun.getGunId(stack) == null) {
            context.getSource().sendFailure(Component.literal("メインハンドにTACZの銃を持ってください"));
            return 0;
        }
        var gunId = gun.getGunId(stack);
        try {
            if (!IntelDraftDefinitionLoader.addGun(stack)) {
                context.getSource().sendFailure(Component.literal("その銃は既にIntel Draft候補です: " + gunId));
                return 0;
            }
        } catch (IOException | RuntimeException e) {
            DpvpTweaks.LOGGER.error("[{}] Could not save Intel Draft gun {}", DpvpTweaks.MOD_NAME, gunId, e);
            context.getSource().sendFailure(Component.literal("Intel Draft設定への保存に失敗しました: " + gunId));
            return 0;
        }
        context.getSource().sendSuccess(
                () -> Component.literal("Intel Draft銃候補に追加しました: " + gunId), true);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> targeted(String name,
            java.util.function.Predicate<ServerPlayer> action) {
        return Commands.literal(name).then(Commands.argument("players", EntityArgument.players())
                .executes(ctx -> forPlayersBoolean(ctx, action)));
    }
    private static int forPlayersBoolean(CommandContext<CommandSourceStack> ctx,
            java.util.function.Predicate<ServerPlayer> action) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
        int count = 0; for (ServerPlayer player : players) if (action.test(player)) count++;
        int result = count; ctx.getSource().sendSuccess(() -> Component.literal("Intel Draft対象: " + result), true);
        return count == 0 ? 0 : count;
    }
}
