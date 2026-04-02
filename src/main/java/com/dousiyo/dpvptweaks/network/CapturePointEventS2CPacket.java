package com.dousiyo.dpvptweaks.network;

import com.dousiyo.dpvptweaks.capture.core.PointState;
import com.dousiyo.dpvptweaks.capture.core.TeamSide;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class CapturePointEventS2CPacket {
    public static Consumer<CapturePointEventS2CPacket> CLIENT_HANDLER = ignored -> {};

    private final byte slotIndex;
    private final long serverGameTime;
    private final PointState state;
    private final TeamSide owner;
    private final float progress0;
    private final TeamSide captureTeam;
    private final float ratePerTick;

    public CapturePointEventS2CPacket(byte slotIndex,
                                      long serverGameTime,
                                      PointState state,
                                      TeamSide owner,
                                      float progress0,
                                      TeamSide captureTeam,
                                      float ratePerTick) {
        this.slotIndex = slotIndex;
        this.serverGameTime = serverGameTime;
        this.state = state;
        this.owner = owner;
        this.progress0 = progress0;
        this.captureTeam = captureTeam;
        this.ratePerTick = ratePerTick;
    }

    public byte getSlotIndex() {
        return slotIndex;
    }

    public long getServerGameTime() {
        return serverGameTime;
    }

    public PointState getState() {
        return state;
    }

    public TeamSide getOwner() {
        return owner;
    }

    public float getProgress0() {
        return progress0;
    }

    public TeamSide getCaptureTeam() {
        return captureTeam;
    }

    public float getRatePerTick() {
        return ratePerTick;
    }

    public static void encode(CapturePointEventS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.slotIndex);
        buf.writeLong(msg.serverGameTime);
        buf.writeEnum(msg.state);
        buf.writeEnum(msg.owner);
        buf.writeFloat(msg.progress0);
        buf.writeEnum(msg.captureTeam);
        buf.writeFloat(msg.ratePerTick);
    }

    public static CapturePointEventS2CPacket decode(FriendlyByteBuf buf) {
        return new CapturePointEventS2CPacket(
                buf.readByte(),
                buf.readLong(),
                buf.readEnum(PointState.class),
                buf.readEnum(TeamSide.class),
                buf.readFloat(),
                buf.readEnum(TeamSide.class),
                buf.readFloat()
        );
    }

    public static void handle(CapturePointEventS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> CLIENT_HANDLER.accept(msg));
        ctx.get().setPacketHandled(true);
    }
}