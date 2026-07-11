package com.dousiyo.dpvptweaks.timer.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class CountdownHudS2CPacket {
    public static Consumer<CountdownHudS2CPacket> CLIENT_HANDLER = ignored -> {};

    public enum State {
        HIDDEN,
        RUNNING,
        FINISHED
    }

    private final State state;
    private final int currentTicks;
    private final int durationTicks;

    public CountdownHudS2CPacket(State state, int currentTicks, int durationTicks) {
        this.state = state;
        this.currentTicks = currentTicks;
        this.durationTicks = durationTicks;
    }

    public static CountdownHudS2CPacket running(int currentTicks, int durationTicks) {
        return new CountdownHudS2CPacket(State.RUNNING, currentTicks, durationTicks);
    }

    public static CountdownHudS2CPacket finished(int durationTicks) {
        return new CountdownHudS2CPacket(State.FINISHED, 0, durationTicks);
    }

    public static CountdownHudS2CPacket hide() {
        return new CountdownHudS2CPacket(State.HIDDEN, 0, 0);
    }

    public State getState() {
        return state;
    }

    public int getCurrentTicks() {
        return currentTicks;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public static void encode(CountdownHudS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.state);
        buf.writeVarInt(msg.currentTicks);
        buf.writeVarInt(msg.durationTicks);
    }

    public static CountdownHudS2CPacket decode(FriendlyByteBuf buf) {
        return new CountdownHudS2CPacket(
                buf.readEnum(State.class),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    public static void handle(CountdownHudS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> CLIENT_HANDLER.accept(msg));
        ctx.get().setPacketHandled(true);
    }
}
