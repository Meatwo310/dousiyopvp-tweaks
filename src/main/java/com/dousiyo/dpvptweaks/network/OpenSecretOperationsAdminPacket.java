package com.dousiyo.dpvptweaks.network;

import com.dousiyo.dpvptweaks.secretoperations.SecretOperationMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record OpenSecretOperationsAdminPacket(SecretOperationMode mode, String phaseLabel, int round, int durationMinutes,
        int draftIntervalMinutes, int participants, int redScore, int blueScore, String configError,
        String notice, List<String> redPlayers, List<String> bluePlayers) {
    public OpenSecretOperationsAdminPacket { redPlayers = List.copyOf(redPlayers); bluePlayers = List.copyOf(bluePlayers); }

    public static void encode(OpenSecretOperationsAdminPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.mode); buf.writeUtf(msg.phaseLabel, 32); buf.writeVarInt(msg.round);
        buf.writeVarInt(msg.durationMinutes); buf.writeVarInt(msg.draftIntervalMinutes);
        buf.writeVarInt(msg.participants); buf.writeVarInt(msg.redScore); buf.writeVarInt(msg.blueScore);
        buf.writeUtf(msg.configError, 512); buf.writeUtf(msg.notice, 512);
        writeList(buf, msg.redPlayers); writeList(buf, msg.bluePlayers);
    }
    public static OpenSecretOperationsAdminPacket decode(FriendlyByteBuf buf) {
        return new OpenSecretOperationsAdminPacket(buf.readEnum(SecretOperationMode.class), buf.readUtf(32), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(512), buf.readUtf(512), readList(buf), readList(buf));
    }
    private static void writeList(FriendlyByteBuf buf, List<String> values) {
        buf.writeVarInt(values.size()); for (String value : values) buf.writeUtf(value, 64);
    }
    private static List<String> readList(FriendlyByteBuf buf) {
        int size = Math.min(256, buf.readVarInt()); List<String> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) result.add(buf.readUtf(64)); return result;
    }
    public static void handle(OpenSecretOperationsAdminPacket msg, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.dousiyo.dpvptweaks.client.secretoperations.SecretOperationsAdminScreen.open(msg)));
        ctx.setPacketHandled(true);
    }
}
