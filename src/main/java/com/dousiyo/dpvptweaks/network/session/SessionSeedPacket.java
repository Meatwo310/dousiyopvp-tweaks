package com.dousiyo.dpvptweaks.network.session;

import com.dousiyo.dpvptweaks.client.sync.ClientSessionStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.function.Supplier;

public class SessionSeedPacket {
    private final String sessionId;
    private final String nonce;
    private final long issuedAtEpochMs;
    private final long expiresAtEpochMs;

    public SessionSeedPacket(String sessionId, String nonce, long issuedAtEpochMs, long expiresAtEpochMs) {
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

    public static void encode(SessionSeedPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.sessionId, 128);
        buf.writeUtf(msg.nonce, 256);
        buf.writeLong(msg.issuedAtEpochMs);
        buf.writeLong(msg.expiresAtEpochMs);
    }

    public static SessionSeedPacket decode(FriendlyByteBuf buf) {
        return new SessionSeedPacket(
                buf.readUtf(128),
                buf.readUtf(256),
                buf.readLong(),
                buf.readLong()
        );
    }

    public static void handle(SessionSeedPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!context.getDirection().getReceptionSide().isClient() || !FMLEnvironment.dist.isClient()) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> ClientSessionStateManager.onSessionSeed(msg));
        context.setPacketHandled(true);
    }
}

