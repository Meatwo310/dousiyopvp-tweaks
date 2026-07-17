package com.dousiyo.dpvptweaks.network.secretoperations;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.secretoperations.SecretShowdownManager;
import com.dousiyo.dpvptweaks.secretoperations.SecretConvoyManager;
import com.dousiyo.dpvptweaks.secretoperations.SecretOperationMode;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SecretOperationsAdminActionPacket(SecretOperationMode mode, Action action, int durationMinutes, int draftIntervalMinutes) {
    public enum Action { RANDOMIZE, START, STOP, RELOAD, REFRESH }
    public static void encode(SecretOperationsAdminActionPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.mode); buf.writeEnum(msg.action); buf.writeVarInt(msg.durationMinutes); buf.writeVarInt(msg.draftIntervalMinutes);
    }
    public static SecretOperationsAdminActionPacket decode(FriendlyByteBuf buf) {
        return new SecretOperationsAdminActionPacket(buf.readEnum(SecretOperationMode.class), buf.readEnum(Action.class), buf.readVarInt(), buf.readVarInt());
    }
    public static void handle(SecretOperationsAdminActionPacket msg, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get(); ServerPlayer sender = ctx.getSender();
        ctx.enqueueWork(() -> {
            if (sender == null || !sender.hasPermissions(2)) return;
            try {
                if (msg.mode == SecretOperationMode.CONVOY) {
                    SecretConvoyManager.ActionResult result = switch (msg.action) {
                        case RANDOMIZE -> SecretConvoyManager.randomize(sender.server);
                        case START -> SecretConvoyManager.start(sender.server, msg.durationMinutes, msg.draftIntervalMinutes);
                        case STOP -> SecretConvoyManager.stop(sender.server);
                        case RELOAD -> SecretConvoyManager.reload(sender.server);
                        case REFRESH -> SecretConvoyManager.ActionResult.ok("");
                    };
                    feedback(sender, result.success(), result.message());
                    if (msg.action != Action.START || !result.success())
                        SecretConvoyManager.openAdmin(sender, result.message());
                } else {
                    SecretShowdownManager.ActionResult result = switch (msg.action) {
                        case RANDOMIZE -> SecretShowdownManager.randomize(sender.server);
                        case START -> SecretShowdownManager.start(sender.server, msg.durationMinutes, msg.draftIntervalMinutes);
                        case STOP -> SecretShowdownManager.stop(sender.server);
                        case RELOAD -> SecretShowdownManager.reload(sender.server);
                        case REFRESH -> SecretShowdownManager.ActionResult.ok("");
                    };
                    feedback(sender, result.success(), result.message());
                    if (msg.action != Action.START || !result.success())
                        SecretShowdownManager.openAdmin(sender, result.message());
                }
            } catch (RuntimeException exception) {
                DpvpTweaks.LOGGER.error("SECRET OPERATIONS admin action failed: player={}, mode={}, action={}",
                        sender.getGameProfile().getName(), msg.mode, msg.action, exception);
                sender.sendSystemMessage(Component.literal("SECRET OPERATIONSの操作中にサーバーエラーが発生しました。サーバーログを確認してください")
                        .withStyle(ChatFormatting.RED));
            }
        });
        ctx.setPacketHandled(true);
    }

    private static void feedback(ServerPlayer sender, boolean success, String message) {
        if (message == null || message.isBlank()) return;
        sender.sendSystemMessage(Component.literal(message)
                .withStyle(success ? ChatFormatting.GREEN : ChatFormatting.RED));
    }
}
