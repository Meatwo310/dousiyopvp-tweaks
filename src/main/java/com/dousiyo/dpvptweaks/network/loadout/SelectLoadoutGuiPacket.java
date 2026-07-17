package com.dousiyo.dpvptweaks.network.loadout;

import com.dousiyo.dpvptweaks.loadout.LoadoutSessionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SelectLoadoutGuiPacket {
    private final long sessionId;
    private final String loadoutId;

    public SelectLoadoutGuiPacket(long sessionId, String loadoutId) {
        this.sessionId = sessionId;
        this.loadoutId = loadoutId == null ? "" : loadoutId;
    }

    public static void encode(SelectLoadoutGuiPacket msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.sessionId);
        buf.writeUtf(msg.loadoutId, 128);
    }

    public static SelectLoadoutGuiPacket decode(FriendlyByteBuf buf) {
        return new SelectLoadoutGuiPacket(buf.readLong(), buf.readUtf(128));
    }

    public static void handle(SelectLoadoutGuiPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!context.getDirection().getReceptionSide().isServer()) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                LoadoutSessionManager.handleSelection(sender, msg.sessionId, msg.loadoutId);
            }
        });
        context.setPacketHandled(true);
    }
}
