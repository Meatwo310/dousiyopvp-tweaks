package com.dousiyo.dpvptweaks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SessionStatePacket {
    private final String stateType;
    private final String sessionId;
    private final String nonce;
    private final long clientTimestampEpochMs;
    private final boolean checksPassed;
    private final String modListHash;
    private final String scriptStateHash;
    private final String clientMetaHash;
    private final String payloadDigest;

    public SessionStatePacket(
            String stateType,
            String sessionId,
            String nonce,
            long clientTimestampEpochMs,
            boolean checksPassed,
            String modListHash,
            String scriptStateHash,
            String clientMetaHash,
            String payloadDigest
    ) {
        this.stateType = stateType;
        this.sessionId = sessionId;
        this.nonce = nonce;
        this.clientTimestampEpochMs = clientTimestampEpochMs;
        this.checksPassed = checksPassed;
        this.modListHash = modListHash;
        this.scriptStateHash = scriptStateHash;
        this.clientMetaHash = clientMetaHash;
        this.payloadDigest = payloadDigest;
    }

    public String stateType() {
        return stateType;
    }

    public String sessionId() {
        return sessionId;
    }

    public String nonce() {
        return nonce;
    }

    public long clientTimestampEpochMs() {
        return clientTimestampEpochMs;
    }

    public boolean checksPassed() {
        return checksPassed;
    }

    public String modListHash() {
        return modListHash;
    }

    public String scriptStateHash() {
        return scriptStateHash;
    }

    public String clientMetaHash() {
        return clientMetaHash;
    }

    public String payloadDigest() {
        return payloadDigest;
    }

    public static void encode(SessionStatePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.stateType, 32);
        buf.writeUtf(msg.sessionId, 128);
        buf.writeUtf(msg.nonce, 256);
        buf.writeLong(msg.clientTimestampEpochMs);
        buf.writeBoolean(msg.checksPassed);
        buf.writeUtf(msg.modListHash, 128);
        buf.writeUtf(msg.scriptStateHash, 128);
        buf.writeUtf(msg.clientMetaHash, 128);
        buf.writeUtf(msg.payloadDigest, 128);
    }

    public static SessionStatePacket decode(FriendlyByteBuf buf) {
        return new SessionStatePacket(
                buf.readUtf(32),
                buf.readUtf(128),
                buf.readUtf(256),
                buf.readLong(),
                buf.readBoolean(),
                buf.readUtf(128),
                buf.readUtf(128),
                buf.readUtf(128),
                buf.readUtf(128)
        );
    }

    public static void handle(SessionStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
    }
}


