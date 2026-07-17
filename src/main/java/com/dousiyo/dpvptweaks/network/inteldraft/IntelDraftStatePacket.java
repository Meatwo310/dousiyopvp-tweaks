package com.dousiyo.dpvptweaks.network.inteldraft;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/** Small state-change-only sync used by client input and presentation. */
public record IntelDraftStatePacket(boolean active, Set<String> effects) {
    public IntelDraftStatePacket { effects = Set.copyOf(effects); }
    public static void encode(IntelDraftStatePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active); buf.writeVarInt(msg.effects.size());
        msg.effects.forEach(v -> buf.writeUtf(v, 64));
    }
    public static IntelDraftStatePacket decode(FriendlyByteBuf buf) {
        boolean active = buf.readBoolean(); int size = Math.min(64, Math.max(0, buf.readVarInt()));
        Set<String> effects = new HashSet<>(); for (int i = 0; i < size; i++) effects.add(buf.readUtf(64));
        return new IntelDraftStatePacket(active, effects);
    }
    public static void handle(IntelDraftStatePacket msg, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.dousiyo.dpvptweaks.client.inteldraft.ClientIntelDraftState.update(msg.active, msg.effects)));
        context.setPacketHandled(true);
    }
}
