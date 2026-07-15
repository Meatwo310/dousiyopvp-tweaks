package com.dousiyo.dpvptweaks.network;

import com.dousiyo.dpvptweaks.arsenal.ArsenalConfig;
import com.dousiyo.dpvptweaks.arsenal.ArsenalMatchManager;
import com.dousiyo.dpvptweaks.arsenal.ArsenalWeaponSetManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ArsenalAdminActionPacket(Action action, String weaponSet, int stage, int reserveMagazines) {
    public enum Action { REGISTER, REGISTER_ALL, VALIDATE, START, STOP, RESET, RELOAD, REFRESH }

    public static void encode(ArsenalAdminActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action); buffer.writeUtf(packet.weaponSet, 64);
        buffer.writeVarInt(packet.stage); buffer.writeVarInt(packet.reserveMagazines);
    }

    public static ArsenalAdminActionPacket decode(FriendlyByteBuf buffer) {
        return new ArsenalAdminActionPacket(buffer.readEnum(Action.class), buffer.readUtf(64),
                buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(ArsenalAdminActionPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> {
            if (sender == null || !sender.hasPermissions(2)) return;
            ArsenalMatchManager.ActionResult result;
            try {
                result = switch (packet.action) {
                    case REGISTER -> {
                        ArsenalWeaponSetManager.setHeldWeapon(packet.weaponSet, packet.stage,
                                packet.reserveMagazines, sender.getMainHandItem());
                        yield ArsenalMatchManager.ActionResult.ok("第" + packet.stage + "段階へ手持ち銃を登録しました");
                    }
                    case REGISTER_ALL -> {
                        ArsenalWeaponSetManager.setHeldWeaponAll(packet.weaponSet,
                                packet.reserveMagazines, sender.getMainHandItem());
                        yield ArsenalMatchManager.ActionResult.ok("手持ち銃を全30段階へ登録しました（デバッグ用）");
                    }
                    case VALIDATE -> {
                        String error = ArsenalWeaponSetManager.validate(packet.weaponSet);
                        yield error == null ? ArsenalMatchManager.ActionResult.ok("有効な武器セットです")
                                : ArsenalMatchManager.ActionResult.error(error);
                    }
                    case START -> ArsenalMatchManager.start(sender.server, packet.weaponSet);
                    case STOP -> ArsenalMatchManager.stop(sender.server);
                    case RESET -> ArsenalMatchManager.reset(sender.server);
                    case RELOAD -> {
                        ArsenalConfig.reload(); ArsenalWeaponSetManager.reload();
                        String error = ArsenalConfig.validate(sender.server).error();
                        yield error == null ? ArsenalMatchManager.ActionResult.ok("設定を再読み込みしました")
                                : ArsenalMatchManager.ActionResult.error(error);
                    }
                    case REFRESH -> ArsenalMatchManager.ActionResult.ok("");
                };
            } catch (Exception exception) {
                result = ArsenalMatchManager.ActionResult.error("操作失敗: " + exception.getMessage());
            }
            ArsenalMatchManager.openAdmin(sender, result.message(), packet.weaponSet);
        });
        context.setPacketHandled(true);
    }
}
