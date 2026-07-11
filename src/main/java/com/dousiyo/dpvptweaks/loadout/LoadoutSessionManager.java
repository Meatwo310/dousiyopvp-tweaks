package com.dousiyo.dpvptweaks.loadout;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.network.LoadoutGuiNetwork;
import com.dousiyo.dpvptweaks.network.OpenLoadoutGuiPacket;
import com.dousiyo.dpvptweaks.network.OpenMiniLoadoutGuiPacket;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class LoadoutSessionManager {
    private static final long SESSION_TTL_MS = 10 * 60 * 1000L;
    private static final AtomicLong NEXT_SESSION_ID = new AtomicLong(1L);
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private LoadoutSessionManager() {
    }

    public static boolean open(ServerPlayer player, ResourceLocation setId) {
        LoadoutSetDefinition set = LoadoutDataManager.getSet(setId);
        boolean mini = set != null && set.isMiniLayout();
        return open(player, setId, mini);
    }

    public static boolean open(ServerPlayer player, ResourceLocation setId, boolean mini) {
        if (player == null || setId == null) {
            return false;
        }

        cleanupExpiredSessions();

        List<LoadoutDefinition> loadouts = LoadoutDataManager.getLoadoutsForSet(setId, player);
        if (loadouts.isEmpty()) {
            player.displayClientMessage(Component.literal("No loadouts are available for set " + setId + "."), false);
            return false;
        }

        long sessionId = NEXT_SESSION_ID.getAndIncrement();
        SESSIONS.put(player.getUUID(), new Session(sessionId, setId, byId(loadouts), System.currentTimeMillis()));
        if (mini) {
            LoadoutGuiNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenMiniLoadoutGuiPacket(loadouts, sessionId));
        } else {
            LoadoutGuiNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenLoadoutGuiPacket(loadouts, sessionId));
        }
        return true;
    }

    public static void handleSelection(ServerPlayer player, long sessionId, String loadoutId) {
        if (player == null) {
            return;
        }

        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.sessionId != sessionId || session.isExpired()) {
            SESSIONS.remove(player.getUUID());
            player.displayClientMessage(Component.literal("Loadout selection expired. Open the loadout GUI again."), false);
            return;
        }

        LoadoutDefinition loadout = session.loadouts.get(loadoutId == null ? "" : loadoutId.trim());
        if (loadout == null) {
            DpvpTweaks.LOGGER.warn("[{}] Player {} tried to select unauthorized loadout '{}' in session {}", DpvpTweaks.MOD_NAME, player.getGameProfile().getName(), loadoutId, sessionId);
            player.displayClientMessage(Component.literal("That loadout is not available in this session."), false);
            return;
        }

        SESSIONS.remove(player.getUUID());
        apply(player, loadout);
    }

    private static void apply(ServerPlayer player, LoadoutDefinition loadout) {
        ResourceLocation functionId = loadout.applyFunction();
        if (functionId == null) {
            player.displayClientMessage(Component.literal("Loadout " + loadout.id() + " has no apply.function."), false);
            return;
        }

        Optional<CommandFunction> function = player.server.getFunctions().get(functionId);
        if (function.isEmpty()) {
            player.displayClientMessage(Component.literal("Loadout function not found: " + functionId), false);
            return;
        }

        try {
            CommandSourceStack source = player.createCommandSourceStack()
                    .withPermission(2)
                    .withSuppressedOutput();
            int commandsExecuted = player.server.getFunctions().execute(function.get(), source);
            DpvpTweaks.LOGGER.info("[{}] Applied loadout '{}' to {} via {} ({} command(s))",
                    DpvpTweaks.MOD_NAME,
                    loadout.id(),
                    player.getGameProfile().getName(),
                    functionId,
                    commandsExecuted);
            player.displayClientMessage(Component.literal("Applied loadout: " + loadout.name()), false);
        } catch (Exception e) {
            DpvpTweaks.LOGGER.error("[{}] Failed to apply loadout '{}' via {}", DpvpTweaks.MOD_NAME, loadout.id(), functionId, e);
            player.displayClientMessage(Component.literal("Failed to apply loadout: " + loadout.name()), false);
        }
    }

    private static Map<String, LoadoutDefinition> byId(List<LoadoutDefinition> loadouts) {
        Map<String, LoadoutDefinition> result = new LinkedHashMap<>();
        for (LoadoutDefinition loadout : loadouts) {
            result.put(loadout.id(), loadout);
        }
        return Map.copyOf(result);
    }

    private static void cleanupExpiredSessions() {
        SESSIONS.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private record Session(long sessionId, ResourceLocation setId, Map<String, LoadoutDefinition> loadouts, long createdAtMillis) {
        private boolean isExpired() {
            return System.currentTimeMillis() - createdAtMillis > SESSION_TTL_MS;
        }
    }
}
