package com.dousiyo.dpvptweaks.network.secretoperations;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DamageFeedbackPacket(int targetEntityId, float healthDamage, float shieldDamage, boolean headshot) {
    private static final float MAX_DISPLAY_DAMAGE = 4096.0F;

    public DamageFeedbackPacket {
        healthDamage = sanitize(healthDamage);
        shieldDamage = sanitize(shieldDamage);
    }

    public static void encode(DamageFeedbackPacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.targetEntityId);
        buffer.writeFloat(message.healthDamage);
        buffer.writeFloat(message.shieldDamage);
        buffer.writeBoolean(message.headshot);
    }

    public static DamageFeedbackPacket decode(FriendlyByteBuf buffer) {
        return new DamageFeedbackPacket(buffer.readVarInt(), buffer.readFloat(), buffer.readFloat(), buffer.readBoolean());
    }

    public static void handle(DamageFeedbackPacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.dousiyo.dpvptweaks.client.secretoperations.ClientDamageFeedback.add(
                        message.targetEntityId, message.healthDamage, message.shieldDamage, message.headshot)));
        context.setPacketHandled(true);
    }

    private static float sanitize(float value) {
        if (!Float.isFinite(value) || value <= 0.0F) return 0.0F;
        return Math.min(MAX_DISPLAY_DAMAGE, value);
    }
}
