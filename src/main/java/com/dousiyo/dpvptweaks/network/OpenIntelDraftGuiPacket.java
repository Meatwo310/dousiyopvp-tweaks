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
        this(IntelDraftDefinition.empty());
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
        buf.writeLong(msg.definition.expiresAtMillis() <= 0L ? 0L : Math.max(1L, msg.definition.expiresAtMillis() - System.currentTimeMillis()));
        buf.writeBoolean(msg.definition.closeAllowed());
        buf.writeVarInt(msg.definition.acquiredTechNames().size());
        for (String name : msg.definition.acquiredTechNames()) buf.writeUtf(name, 128);
        buf.writeVarInt(msg.definition.choices().size());
        for (IntelDraftDefinition.ChoiceDefinition choice : msg.definition.choices()) {
            IntelDraftDefinition.TechDefinition tech = choice.tech();
            IntelDraftDefinition.GunDefinition gun = choice.gun();
            buf.writeBoolean(tech.id() != null);
            if (tech.id() != null) buf.writeResourceLocation(tech.id());
            buf.writeUtf(tech.name(), 128);
            buf.writeUtf(tech.description(), 512);
            buf.writeItem(tech.iconStack());
            buf.writeResourceLocation(gun.id());
            buf.writeUtf(gun.name(), 128);
            buf.writeItem(gun.gunStack());
            var attachment = choice.attachment();
            buf.writeResourceLocation(attachment.id());
            buf.writeUtf(attachment.name(), 128);
            buf.writeItem(attachment.attachmentStack());
        }
    }

    public static OpenIntelDraftGuiPacket decode(FriendlyByteBuf buf) {
        if (buf.readableBytes() == 0) {
            return new OpenIntelDraftGuiPacket();
        }
        long sessionId = buf.readLong();
        int remainingRerolls = buf.readVarInt();
        long remaining = Math.max(0L, buf.readLong());
        long expiresAt = remaining == 0L ? 0L : System.currentTimeMillis() + remaining;
        boolean closeAllowed = buf.readBoolean();
        int acquiredCount = buf.readVarInt();
        List<String> acquired = new ArrayList<>(acquiredCount);
        for (int i = 0; i < acquiredCount; i++) acquired.add(buf.readUtf(128));
        int choiceCount = buf.readVarInt();
        List<IntelDraftDefinition.ChoiceDefinition> choices = new ArrayList<>(choiceCount);
        for (int i = 0; i < choiceCount; i++) {
            var techId = buf.readBoolean() ? buf.readResourceLocation() : null;
            String name = buf.readUtf(128);
            String description = buf.readUtf(512);
            ItemStack icon = buf.readItem();
            var gunId = buf.readResourceLocation();
            String gunName = buf.readUtf(128);
            ItemStack stack = buf.readItem();
            var attachmentId = buf.readResourceLocation();
            String attachmentName = buf.readUtf(128);
            ItemStack attachmentStack = buf.readItem();
            choices.add(new IntelDraftDefinition.ChoiceDefinition(
                    new IntelDraftDefinition.TechDefinition(techId, name, description, icon,
                            IntelDraftDefinition.EffectDefinition.NONE, null),
                    new IntelDraftDefinition.GunDefinition(gunId, gunName, stack),
                    new IntelDraftDefinition.AttachmentDefinition(attachmentId, attachmentName, attachmentStack)));
        }
        return new OpenIntelDraftGuiPacket(new IntelDraftDefinition(sessionId, remainingRerolls, expiresAt, closeAllowed, acquired, choices));
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
