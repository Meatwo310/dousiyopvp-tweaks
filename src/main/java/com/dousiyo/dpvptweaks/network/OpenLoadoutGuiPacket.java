package com.dousiyo.dpvptweaks.network;

import com.dousiyo.dpvptweaks.loadout.LoadoutDefinition;
import com.dousiyo.dpvptweaks.loadout.LoadoutDefinitionLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class OpenLoadoutGuiPacket {
    private final List<LoadoutDefinition> loadouts;
    private final long sessionId;

    public OpenLoadoutGuiPacket() {
        this(LoadoutDefinitionLoader.load("loadout_gui.json", "OpenLoadoutGuiPacket"), 0L);
    }

    public OpenLoadoutGuiPacket(List<LoadoutDefinition> loadouts) {
        this(loadouts, 0L);
    }

    public OpenLoadoutGuiPacket(List<LoadoutDefinition> loadouts, long sessionId) {
        this.loadouts = List.copyOf(loadouts);
        this.sessionId = sessionId;
    }

    public List<LoadoutDefinition> getLoadouts() {
        return loadouts;
    }

    public static void encode(OpenLoadoutGuiPacket msg, FriendlyByteBuf buf) {
        writeLoadouts(buf, msg.loadouts);
        buf.writeLong(msg.sessionId);
    }

    public static OpenLoadoutGuiPacket decode(FriendlyByteBuf buf) {
        if (buf.readableBytes() == 0) {
            return new OpenLoadoutGuiPacket();
        }
        List<LoadoutDefinition> loadouts = readLoadouts(buf);
        long sessionId = buf.readableBytes() >= Long.BYTES ? buf.readLong() : 0L;
        return new OpenLoadoutGuiPacket(loadouts, sessionId);
    }

    public static void handle(OpenLoadoutGuiPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!context.getDirection().getReceptionSide().isClient()) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.dousiyo.dpvptweaks.client.ClientLoadoutGuiHandler.openLoadoutScreen(msg.loadouts, msg.sessionId)));
        context.setPacketHandled(true);
    }

    static void writeLoadouts(FriendlyByteBuf buf, List<LoadoutDefinition> loadouts) {
        buf.writeVarInt(loadouts.size());
        for (LoadoutDefinition loadout : loadouts) {
            buf.writeUtf(loadout.id(), 128);
            buf.writeUtf(loadout.name(), 128);
            buf.writeUtf(loadout.weapons(), 128);
            buf.writeUtf(loadout.description(), 512);
            buf.writeVarInt(loadout.gunStacks().size());
            for (ItemStack stack : loadout.gunStacks()) {
                buf.writeItem(stack);
            }
        }
    }

    static List<LoadoutDefinition> readLoadouts(FriendlyByteBuf buf) {
        int loadoutCount = buf.readVarInt();
        List<LoadoutDefinition> loadouts = new ArrayList<>(loadoutCount);
        for (int i = 0; i < loadoutCount; i++) {
            String id = buf.readUtf(128);
            String name = buf.readUtf(128);
            String weapons = buf.readUtf(128);
            String description = buf.readUtf(512);
            int stackCount = buf.readVarInt();
            List<ItemStack> stacks = new ArrayList<>(stackCount);
            for (int stackIndex = 0; stackIndex < stackCount; stackIndex++) {
                stacks.add(buf.readItem());
            }
            loadouts.add(new LoadoutDefinition(id, name, weapons, stacks, description));
        }
        return List.copyOf(loadouts);
    }
}
