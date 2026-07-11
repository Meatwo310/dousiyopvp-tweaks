package com.dousiyo.dpvptweaks.network;

import com.dousiyo.dpvptweaks.inteldraft.IntelDraftDefinition;
import com.dousiyo.dpvptweaks.inteldraft.IntelDraftDefinitionLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class OpenIntelDraftGuiPacket {
    private final IntelDraftDefinition definition;

    public OpenIntelDraftGuiPacket() {
        this(IntelDraftDefinitionLoader.load());
    }

    public OpenIntelDraftGuiPacket(IntelDraftDefinition definition) {
        this.definition = definition;
    }

    public IntelDraftDefinition getDefinition() {
        return definition;
    }

    public static void encode(OpenIntelDraftGuiPacket msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.definition.sessionId());
        buf.writeVarInt(msg.definition.remainingRerolls());
        buf.writeVarInt(msg.definition.choices().size());
        for (IntelDraftDefinition.ChoiceDefinition choice : msg.definition.choices()) {
            IntelDraftDefinition.TechDefinition tech = choice.tech();
            IntelDraftDefinition.GunDefinition gun = choice.gun();
            buf.writeVarInt(tech.id());
            buf.writeUtf(tech.name(), 128);
            buf.writeUtf(tech.description(), 512);
            buf.writeItem(tech.iconStack());
            buf.writeVarInt(gun.id());
            buf.writeUtf(gun.name(), 128);
            buf.writeItem(gun.gunStack());
        }
    }

    public static OpenIntelDraftGuiPacket decode(FriendlyByteBuf buf) {
        if (buf.readableBytes() == 0) {
            return new OpenIntelDraftGuiPacket();
        }
        long sessionId = buf.readLong();
        int remainingRerolls = buf.readVarInt();
        int choiceCount = buf.readVarInt();
        List<IntelDraftDefinition.ChoiceDefinition> choices = new ArrayList<>(choiceCount);
        for (int i = 0; i < choiceCount; i++) {
            int techId = buf.readVarInt();
            String name = buf.readUtf(128);
            String description = buf.readUtf(512);
            ItemStack icon = buf.readItem();
            int gunId = buf.readVarInt();
            String gunName = buf.readUtf(128);
            ItemStack stack = buf.readItem();
            choices.add(new IntelDraftDefinition.ChoiceDefinition(
                    new IntelDraftDefinition.TechDefinition(techId, name, description, icon),
                    new IntelDraftDefinition.GunDefinition(gunId, gunName, stack)
            ));
        }
        return new OpenIntelDraftGuiPacket(new IntelDraftDefinition(sessionId, remainingRerolls, choices));
    }

    public static void handle(OpenIntelDraftGuiPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!context.getDirection().getReceptionSide().isClient()) {
            context.setPacketHandled(true);
            return;
        }
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.dousiyo.dpvptweaks.client.ClientLoadoutGuiHandler.openIntelDraftScreen(msg.definition)));
        context.setPacketHandled(true);
    }
}
