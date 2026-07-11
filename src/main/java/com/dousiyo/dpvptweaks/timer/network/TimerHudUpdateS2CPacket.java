package com.dousiyo.dpvptweaks.timer.network;

import com.dousiyo.dpvptweaks.timer.core.TimerMode;
import com.dousiyo.dpvptweaks.timer.core.TimerState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class TimerHudUpdateS2CPacket {
    public static Consumer<TimerHudUpdateS2CPacket> CLIENT_HANDLER = ignored -> {};

    private final boolean visible;
    private final String timerId;
    private final TimerMode mode;
    private final TimerState state;
    private final int currentTicks;
    private final int durationTicks;
    private final Component title;
    private final Component finishMessage;

    public TimerHudUpdateS2CPacket(boolean visible, String timerId, TimerMode mode, TimerState state,
                                   int currentTicks, int durationTicks, Component title, Component finishMessage) {
        this.visible = visible;
        this.timerId = timerId;
        this.mode = mode;
        this.state = state;
        this.currentTicks = currentTicks;
        this.durationTicks = durationTicks;
        this.title = title;
        this.finishMessage = finishMessage;
    }

    public static TimerHudUpdateS2CPacket show(String timerId, TimerMode mode, TimerState state,
                                               int currentTicks, int durationTicks, Component title, Component finishMessage) {
        return new TimerHudUpdateS2CPacket(true, timerId, mode, state, currentTicks, durationTicks, title, finishMessage);
    }

    public static TimerHudUpdateS2CPacket hide() {
        return new TimerHudUpdateS2CPacket(false, "", TimerMode.COUNTDOWN, TimerState.IDLE, 0, 0, Component.empty(), Component.empty());
    }

    public boolean isVisible() {
        return visible;
    }

    public String getTimerId() {
        return timerId;
    }

    public TimerMode getMode() {
        return mode;
    }

    public TimerState getState() {
        return state;
    }

    public int getCurrentTicks() {
        return currentTicks;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public Component getTitle() {
        return title;
    }

    public Component getFinishMessage() {
        return finishMessage;
    }

    public static void encode(TimerHudUpdateS2CPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.visible);
        buf.writeUtf(msg.timerId, 128);
        buf.writeEnum(msg.mode);
        buf.writeEnum(msg.state);
        buf.writeVarInt(msg.currentTicks);
        buf.writeVarInt(msg.durationTicks);
        buf.writeComponent(msg.title);
        buf.writeComponent(msg.finishMessage);
    }

    public static TimerHudUpdateS2CPacket decode(FriendlyByteBuf buf) {
        return new TimerHudUpdateS2CPacket(
                buf.readBoolean(),
                buf.readUtf(128),
                buf.readEnum(TimerMode.class),
                buf.readEnum(TimerState.class),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readComponent(),
                buf.readComponent()
        );
    }

    public static void handle(TimerHudUpdateS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> CLIENT_HANDLER.accept(msg));
        ctx.get().setPacketHandled(true);
    }
}
