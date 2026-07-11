package com.dousiyo.dpvptweaks.pvpstats.network.s2c;

import com.dousiyo.dpvptweaks.pvpstats.model.AggregateStats;
import com.dousiyo.dpvptweaks.pvpstats.model.MatchRecord;
import com.dousiyo.dpvptweaks.pvpstats.model.PlayerPrivacySettings;
import com.dousiyo.dpvptweaks.pvpstats.model.RankingEntry;
import com.dousiyo.dpvptweaks.pvpstats.model.StatsGuiPayload;
import com.dousiyo.dpvptweaks.pvpstats.mode.PvpModeDefinition;
import com.dousiyo.dpvptweaks.pvpstats.rank.RankState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.function.Supplier;

public final class OpenStatsGuiPacket {
    private final StatsGuiPayload payload;

    public OpenStatsGuiPacket(StatsGuiPayload payload) {
        this.payload = payload;
    }

    public static void encode(OpenStatsGuiPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.payload.targetId());
        buf.writeUtf(packet.payload.targetName(), 64);
        buf.writeBoolean(packet.payload.editableSettings());
        buf.writeBoolean(packet.payload.statsVisible());
        buf.writeBoolean(packet.payload.historyVisible());
        writePrivacy(buf, packet.payload.privacySettings());
        writeAggregate(buf, packet.payload.global());

        buf.writeVarInt(packet.payload.modeStats().size());
        for (Map.Entry<String, AggregateStats> entry : packet.payload.modeStats().entrySet()) {
            buf.writeUtf(entry.getKey(), 128);
            writeAggregate(buf, entry.getValue());
        }

        buf.writeVarInt(packet.payload.recentMatches().size());
        for (MatchRecord record : packet.payload.recentMatches()) {
            buf.writeUtf(record.matchId(), 64);
            buf.writeUtf(record.modeId(), 128);
            buf.writeUtf(record.result(), 16);
            buf.writeVarInt(record.kills());
            buf.writeVarInt(record.deaths());
            buf.writeLong(record.timestamp());
        }

        buf.writeVarInt(packet.payload.modeDefinitions().size());
        for (PvpModeDefinition definition : packet.payload.modeDefinitions()) {
            buf.writeUtf(definition.modeId(), 128);
            buf.writeUtf(definition.displayName(), 128);
            buf.writeUtf(definition.translationKey(), 192);
            buf.writeUtf(definition.description(), 1024);
            buf.writeUtf(definition.descriptionTranslationKey(), 192);
            buf.writeBoolean(definition.icon() != null);
            if (definition.icon() != null) {
                buf.writeResourceLocation(definition.icon());
            }
            buf.writeVarInt(definition.sortOrder());
            buf.writeVarInt(definition.tags().size());
            for (String tag : definition.tags()) {
                buf.writeUtf(tag, 64);
            }
            buf.writeBoolean(definition.visible());
            buf.writeBoolean(definition.rankingEnabled());
            buf.writeLong(definition.rankingMinMatches());
            buf.writeLong(definition.rankingMinKills());
        }

        buf.writeVarInt(packet.payload.rankingEntries().size());
        for (RankingEntry entry : packet.payload.rankingEntries()) {
            buf.writeUUID(entry.playerId() == null ? new UUID(0L, 0L) : entry.playerId());
            buf.writeUtf(entry.mcid(), 64);
            buf.writeUtf(entry.modeId(), 128);
            buf.writeVarInt(entry.rank());
            buf.writeLong(entry.kills());
            buf.writeLong(entry.deaths());
            buf.writeLong(entry.matches());
            buf.writeLong(entry.wins());
            buf.writeLong(entry.losses());
        }

        buf.writeVarInt(packet.payload.ranks().size());
        for (Map.Entry<String, RankState> entry : packet.payload.ranks().entrySet()) {
            buf.writeUtf(entry.getKey(), 128);
            buf.writeVarInt(entry.getValue().rating());
            buf.writeVarInt(entry.getValue().peakRating());
            buf.writeVarInt(entry.getValue().placementMatches());
        }
    }

    public static OpenStatsGuiPacket decode(FriendlyByteBuf buf) {
        UUID targetId = buf.readUUID();
        String targetName = buf.readUtf(64);
        boolean editableSettings = buf.readBoolean();
        boolean statsVisible = buf.readBoolean();
        boolean historyVisible = buf.readBoolean();
        PlayerPrivacySettings privacySettings = readPrivacy(buf);
        AggregateStats global = readAggregate(buf);

        int modeCount = buf.readVarInt();
        Map<String, AggregateStats> modes = new LinkedHashMap<>();
        for (int i = 0; i < modeCount; i++) {
            modes.put(buf.readUtf(128), readAggregate(buf));
        }

        int recentCount = buf.readVarInt();
        List<MatchRecord> recentMatches = new ArrayList<>(recentCount);
        for (int i = 0; i < recentCount; i++) {
            recentMatches.add(new MatchRecord(
                    buf.readUtf(64),
                    buf.readUtf(128),
                    buf.readUtf(16),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readLong()
            ));
        }

        int definitionCount = buf.readVarInt();
        List<PvpModeDefinition> modeDefinitions = new ArrayList<>(definitionCount);
        for (int i = 0; i < definitionCount; i++) {
            String modeId = buf.readUtf(128);
            String displayName = buf.readUtf(128);
            String translationKey = buf.readUtf(192);
            String description = buf.readUtf(1024);
            String descriptionTranslationKey = buf.readUtf(192);
            ResourceLocation icon = buf.readBoolean() ? buf.readResourceLocation() : null;
            int sortOrder = buf.readVarInt();
            int tagCount = buf.readVarInt();
            LinkedHashSet<String> tags = new LinkedHashSet<>();
            for (int tagIndex = 0; tagIndex < tagCount; tagIndex++) {
                tags.add(buf.readUtf(64));
            }
            modeDefinitions.add(new PvpModeDefinition(
                    modeId, displayName, translationKey, description, descriptionTranslationKey, icon, sortOrder, tags,
                    buf.readBoolean(), buf.readBoolean(), buf.readLong(), buf.readLong()
            ));
        }

        int rankingCount = buf.readVarInt();
        List<RankingEntry> rankingEntries = new ArrayList<>(rankingCount);
        for (int i = 0; i < rankingCount; i++) {
            rankingEntries.add(new RankingEntry(
                    buf.readUUID(),
                    buf.readUtf(64),
                    buf.readUtf(128),
                    buf.readVarInt(),
                    buf.readLong(),
                    buf.readLong(),
                    buf.readLong(),
                    buf.readLong(),
                    buf.readLong()
            ));
        }
        int rankCount = buf.readVarInt();
        Map<String, RankState> ranks = new LinkedHashMap<>();
        for (int i = 0; i < rankCount; i++) {
            ranks.put(buf.readUtf(128), new RankState(buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        }
        return new OpenStatsGuiPacket(new StatsGuiPayload(
                targetId,
                targetName,
                global,
                modes,
                recentMatches,
                modeDefinitions,
                rankingEntries,
                ranks,
                privacySettings,
                editableSettings,
                statsVisible,
                historyVisible
        ));
    }

    public static void handle(OpenStatsGuiPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (!context.getDirection().getReceptionSide().isClient()) {
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.dousiyo.dpvptweaks.client.pvpstats.PvpStatsClient.openStatsScreen(packet.payload)));
        context.setPacketHandled(true);
    }

    private static void writeAggregate(FriendlyByteBuf buf, AggregateStats stats) {
        buf.writeLong(stats.wins());
        buf.writeLong(stats.losses());
        buf.writeLong(stats.draws());
        buf.writeLong(stats.kills());
        buf.writeLong(stats.deaths());
        buf.writeLong(stats.matches());
    }

    private static AggregateStats readAggregate(FriendlyByteBuf buf) {
        return new AggregateStats(
                buf.readLong(),
                buf.readLong(),
                buf.readLong(),
                buf.readLong(),
                buf.readLong(),
                buf.readLong()
        );
    }

    private static void writePrivacy(FriendlyByteBuf buf, PlayerPrivacySettings settings) {
        PlayerPrivacySettings safeSettings = settings == null ? PlayerPrivacySettings.DEFAULT : settings;
        buf.writeBoolean(safeSettings.showRank());
        buf.writeBoolean(safeSettings.showStats());
        buf.writeBoolean(safeSettings.showMatchHistory());
        buf.writeBoolean(safeSettings.joinLeaderboards());
    }

    private static PlayerPrivacySettings readPrivacy(FriendlyByteBuf buf) {
        return new PlayerPrivacySettings(
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }
}
