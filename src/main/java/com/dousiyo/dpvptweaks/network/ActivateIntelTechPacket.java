package com.dousiyo.dpvptweaks.network;

import com.dousiyo.dpvptweaks.inteldraft.IntelDraftManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Edge-triggered client intent. Server remains authoritative. */
public final class ActivateIntelTechPacket {
    public enum Action { DOUBLE_JUMP }
    private final Action action;
    public ActivateIntelTechPacket(Action action) { this.action = action; }
    public static void encode(ActivateIntelTechPacket msg, FriendlyByteBuf buf) { buf.writeEnum(msg.action); }
    public static ActivateIntelTechPacket decode(FriendlyByteBuf buf) { return new ActivateIntelTechPacket(buf.readEnum(Action.class)); }
    public static void handle(ActivateIntelTechPacket msg, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        var player = context.getSender();
        if (player != null) context.enqueueWork(() -> {
            if (msg.action == Action.DOUBLE_JUMP && IntelDraftManager.hasTech(player, "double_jump")
                    && !player.onGround() && !player.isPassenger() && !player.isFallFlying()
                    && !player.getPersistentData().getBoolean("dpvptweaksIntelDoubleJumpUsed")) {
                player.getPersistentData().putBoolean("dpvptweaksIntelDoubleJumpUsed", true);
                var movement = player.getDeltaMovement();
                player.setDeltaMovement(movement.x, 0.48D, movement.z);
                player.hurtMarked = true;
            }
        });
        context.setPacketHandled(true);
    }
}
