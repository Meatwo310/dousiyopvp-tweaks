package com.dousiyo.dpvptweaks.network.loadout;

import com.dousiyo.dpvptweaks.loadout.LoadoutDefinition;
import com.dousiyo.dpvptweaks.loadout.LoadoutDefinitionLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class OpenMiniLoadoutGuiPacket {
    private final List<LoadoutDefinition> loadouts;
    private final long sessionId;

    public OpenMiniLoadoutGuiPacket() {
        this(LoadoutDefinitionLoader.load("mini_loadout_gui.json", "OpenMiniLoadoutGuiPacket"), 0L);
    }

    public OpenMiniLoadoutGuiPacket(List<LoadoutDefinition> loadouts) {
        this(loadouts, 0L);
    }

    public OpenMiniLoadoutGuiPacket(List<LoadoutDefinition> loadouts, long sessionId) {
        this.loadouts = List.copyOf(loadouts);
        this.sessionId = sessionId;
    }

    public List<LoadoutDefinition> getLoadouts() {
        return loadouts;
    }

    public static void encode(OpenMiniLoadoutGuiPacket msg, FriendlyByteBuf buf) {
        OpenLoadoutGuiPacket.writeLoadouts(buf, msg.loadouts);
        buf.writeLong(msg.sessionId);
    }

    public static OpenMiniLoadoutGuiPacket decode(FriendlyByteBuf buf) {
        if (buf.readableBytes() == 0) {
            return new OpenMiniLoadoutGuiPacket();
        }
        List<LoadoutDefinition> loadouts = OpenLoadoutGuiPacket.readLoadouts(buf);
        long sessionId = buf.readableBytes() >= Long.BYTES ? buf.readLong() : 0L;
        return new OpenMiniLoadoutGuiPacket(loadouts, sessionId);
    }

    public static void handle(OpenMiniLoadoutGuiPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!context.getDirection().getReceptionSide().isClient()) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.dousiyo.dpvptweaks.client.loadout.ClientLoadoutGuiHandler.openMiniLoadoutScreen(msg.loadouts, msg.sessionId)));
        context.setPacketHandled(true);
    }
}
