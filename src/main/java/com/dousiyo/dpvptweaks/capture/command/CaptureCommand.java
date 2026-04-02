package com.dousiyo.dpvptweaks.capture.command;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.capture.core.CaptureManager;
import com.dousiyo.dpvptweaks.capture.data.CapturePointsDefinition;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CaptureCommand {
    private CaptureCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(buildRoot());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildRoot() {
        return Commands.literal("capture")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("reload").executes(CaptureCommand::reload))
                .then(Commands.literal("point")
                        .then(Commands.literal("list").executes(CaptureCommand::list))
                        .then(Commands.literal("info")
                                .then(Commands.argument("slot", IntegerArgumentType.integer(0, 4))
                                        .executes(CaptureCommand::info)))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("slot", IntegerArgumentType.integer(0, 4))
                                        .executes(CaptureCommand::remove)))
                        .then(Commands.literal("setarea")
                                .then(Commands.argument("slot", IntegerArgumentType.integer(0, 4))
                                        .then(Commands.argument("x1", IntegerArgumentType.integer())
                                                .then(Commands.argument("y1", IntegerArgumentType.integer())
                                                        .then(Commands.argument("z1", IntegerArgumentType.integer())
                                                                .then(Commands.argument("x2", IntegerArgumentType.integer())
                                                                        .then(Commands.argument("y2", IntegerArgumentType.integer())
                                                                                .then(Commands.argument("z2", IntegerArgumentType.integer())
                                                                                        .executes(CaptureCommand::setArea))))))))));
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        int count = CaptureManager.get(level).reloadFromDisk(level, true);
        ctx.getSource().sendSuccess(() -> Component.literal("capture points reloaded: " + count), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        CaptureManager manager = CaptureManager.get(level);
        if (manager.listPoints().isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("no capture points defined"), false);
            return 1;
        }

        for (CapturePointsDefinition.PointDefinition point : manager.listPoints()) {
            String line = "slot=" + point.slot()
                    + " id=" + point.id()
                    + " aabb=(" + point.x1() + "," + point.y1() + "," + point.z1() + ") -> ("
                    + point.x2() + "," + point.y2() + "," + point.z2() + ")";
            ctx.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return manager.listPoints().size();
    }

    private static int info(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        CaptureManager manager = CaptureManager.get(level);
        int slot = IntegerArgumentType.getInteger(ctx, "slot");
        CapturePointsDefinition.PointDefinition point = manager.getPoint(slot).orElse(null);
        if (point == null) {
            ctx.getSource().sendFailure(Component.literal("point not found: slot=" + slot));
            return 0;
        }

        String line = "slot=" + point.slot()
                + " id=" + point.id()
                + " aabb=(" + point.x1() + "," + point.y1() + "," + point.z1() + ") -> ("
                + point.x2() + "," + point.y2() + "," + point.z2() + ")";
        ctx.getSource().sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        int slot = IntegerArgumentType.getInteger(ctx, "slot");
        CaptureManager.get(level).removePoint(level, slot);
        ctx.getSource().sendSuccess(() -> Component.literal("removed capture point: slot=" + slot), true);
        return 1;
    }

    private static int setArea(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        int slot = IntegerArgumentType.getInteger(ctx, "slot");
        int x1 = IntegerArgumentType.getInteger(ctx, "x1");
        int y1 = IntegerArgumentType.getInteger(ctx, "y1");
        int z1 = IntegerArgumentType.getInteger(ctx, "z1");
        int x2 = IntegerArgumentType.getInteger(ctx, "x2");
        int y2 = IntegerArgumentType.getInteger(ctx, "y2");
        int z2 = IntegerArgumentType.getInteger(ctx, "z2");

        CaptureManager.get(level).setPointArea(level, slot, x1, y1, z1, x2, y2, z2);
        ctx.getSource().sendSuccess(() -> Component.literal("updated capture point: slot=" + slot), true);
        return 1;
    }
}