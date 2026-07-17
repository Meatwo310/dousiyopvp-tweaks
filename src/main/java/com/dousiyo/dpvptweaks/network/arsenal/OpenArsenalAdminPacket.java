package com.dousiyo.dpvptweaks.network.arsenal;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record OpenArsenalAdminPacket(String state, String selectedWeaponSet, String activeWeaponSet,
                                     String configError, String notice, List<String> weaponSets,
                                     List<String> participants) {
    public OpenArsenalAdminPacket {
        weaponSets = List.copyOf(weaponSets); participants = List.copyOf(participants);
    }

    public static void encode(OpenArsenalAdminPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.state, 32); buffer.writeUtf(packet.selectedWeaponSet, 64);
        buffer.writeUtf(packet.activeWeaponSet, 64); buffer.writeUtf(packet.configError, 512);
        buffer.writeUtf(packet.notice, 512); writeList(buffer, packet.weaponSets); writeList(buffer, packet.participants);
    }

    public static OpenArsenalAdminPacket decode(FriendlyByteBuf buffer) {
        return new OpenArsenalAdminPacket(buffer.readUtf(32), buffer.readUtf(64), buffer.readUtf(64),
                buffer.readUtf(512), buffer.readUtf(512), readList(buffer), readList(buffer));
    }

    private static void writeList(FriendlyByteBuf buffer, List<String> values) {
        buffer.writeVarInt(values.size());
        for (String value : values) buffer.writeUtf(value, 256);
    }

    private static List<String> readList(FriendlyByteBuf buffer) {
        int size = Math.min(256, buffer.readVarInt());
        List<String> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) result.add(buffer.readUtf(256));
        return result;
    }

    public static void handle(OpenArsenalAdminPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.dousiyo.dpvptweaks.client.arsenal.ArsenalAdminScreen.open(packet)));
        context.setPacketHandled(true);
    }
}
