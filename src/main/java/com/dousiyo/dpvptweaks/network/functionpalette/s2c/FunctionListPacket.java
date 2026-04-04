package com.dousiyo.dpvptweaks.network.functionpalette.s2c;

import com.dousiyo.dpvptweaks.functionpalette.FunctionPaletteAction;
import com.dousiyo.dpvptweaks.functionpalette.FunctionPaletteCategory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class FunctionListPacket {
    private static final int MAX_ID_LENGTH = 512;
    private final List<FunctionPaletteCategory> categories;

    public FunctionListPacket(List<FunctionPaletteCategory> categories) {
        this.categories = List.copyOf(categories);
    }

    public static void encode(FunctionListPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.categories.size());
        for (FunctionPaletteCategory category : packet.categories) {
            buf.writeUtf(category.id(), 64);
            buf.writeUtf(category.displayName(), 128);
            buf.writeVarInt(category.actions().size());
            for (FunctionPaletteAction action : category.actions()) {
                buf.writeUtf(action.label(), 64);
                buf.writeUtf(action.functionId(), MAX_ID_LENGTH);
            }
        }
    }

    public static FunctionListPacket decode(FriendlyByteBuf buf) {
        int categoryCount = buf.readVarInt();
        if (categoryCount < 0 || categoryCount > 1024) {
            throw new IllegalArgumentException("Invalid category count: " + categoryCount);
        }

        List<FunctionPaletteCategory> categories = new ArrayList<>(categoryCount);
        for (int i = 0; i < categoryCount; i++) {
            String id = buf.readUtf(64);
            String displayName = buf.readUtf(128);
            int actionCount = buf.readVarInt();
            if (actionCount < 0 || actionCount > 256) {
                throw new IllegalArgumentException("Invalid action count for category " + id + ": " + actionCount);
            }

            List<FunctionPaletteAction> actions = new ArrayList<>(actionCount);
            for (int j = 0; j < actionCount; j++) {
                actions.add(new FunctionPaletteAction(
                        buf.readUtf(64),
                        buf.readUtf(MAX_ID_LENGTH)
                ));
            }
            categories.add(new FunctionPaletteCategory(id, displayName, actions));
        }

        return new FunctionListPacket(categories);
    }

    public static void handle(FunctionListPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!context.getDirection().getReceptionSide().isClient()) {
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.dousiyo.dpvptweaks.client.function.FunctionPaletteClient.applyPaletteData(packet.categories)));
        context.setPacketHandled(true);
    }
}
