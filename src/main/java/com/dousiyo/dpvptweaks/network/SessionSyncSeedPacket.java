package com.dousiyo.dpvptweaks.network;

import com.dousiyo.dpvptweaks.client.sync.ClientSessionSyncManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.function.Supplier;

public class SessionSyncSeedPacket {
    private final String sessionId;
    private final String nonce;
    private final long issuedAtEpochMs;
    private final long expiresAtEpochMs;

    public SessionSyncSeedPacket(String sessionId, String nonce, long issuedAtEpochMs, long expiresAtEpochMs) {
        this.sessionId = sessionId;
        this.nonce = nonce;
        this.issuedAtEpochMs = issuedAtEpochMs;
        this.expiresAtEpochMs = expiresAtEpochMs;
    }

    public String sessionId() {
        return sessionId;
    }

    public String nonce() {
        return nonce;
    }

    public long issuedAtEpochMs() {
        return issuedAtEpochMs;
    }

    public long expiresAtEpochMs() {
        return expiresAtEpochMs;
    }

    public static void encode(SessionSyncSeedPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.sessionId, 128);
        buf.writeUtf(msg.nonce, 256);
        buf.writeLong(msg.issuedAtEpochMs);
        buf.writeLong(msg.expiresAtEpochMs);
    }

    public static SessionSyncSeedPacket decode(FriendlyByteBuf buf) {
        return new SessionSyncSeedPacket(
                buf.readUtf(128),
                buf.readUtf(256),
                buf.readLong(),
                buf.readLong()
        );
    }

    public static void handle(SessionSyncSeedPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!context.getDirection().getReceptionSide().isClient() || !FMLEnvironment.dist.isClient()) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> ClientSessionSyncManager.onSessionSeed(msg));
        context.setPacketHandled(true);
    }
}
