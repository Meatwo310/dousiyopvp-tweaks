package com.dousiyo.dpvptweaks.loadout;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.network.LoadoutGuiNetwork;
import com.dousiyo.dpvptweaks.network.OpenLoadoutGuiPacket;
import com.dousiyo.dpvptweaks.network.OpenMiniLoadoutGuiPacket;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSource;
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
        return open(player, setId, false);
    }

    public static boolean open(ServerPlayer player, ResourceLocation setId, boolean mini) {
        if (player == null || setId == null) {
            return false;
        }

        cleanupExpiredSessions();

        List<LoadoutDataManager.AvailableLoadout> available = LoadoutDataManager.getAvailableLoadoutsForSet(setId, player);
        if (available.isEmpty()) {
            player.displayClientMessage(Component.literal("No loadouts are available for set " + setId + "."), false);
            return false;
        }

        long sessionId = NEXT_SESSION_ID.getAndIncrement();
        SESSIONS.put(player.getUUID(), new Session(sessionId, setId, byId(available), System.currentTimeMillis()));
        List<LoadoutDefinition> previews = available.stream().map(LoadoutDataManager.AvailableLoadout::preview).toList();
        Object packet = mini ? new OpenMiniLoadoutGuiPacket(previews, sessionId) : new OpenLoadoutGuiPacket(previews, sessionId);
        LoadoutGuiNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
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

        LoadoutDataManager.AvailableLoadout selected = session.loadouts.get(loadoutId == null ? "" : loadoutId.trim());
        if (selected == null) {
            DpvpTweaks.LOGGER.warn("[{}] Player {} tried to select unauthorized loadout '{}' in session {}", DpvpTweaks.MOD_NAME, player.getGameProfile().getName(), loadoutId, sessionId);
            player.displayClientMessage(Component.literal("That loadout is not available in this session."), false);
            return;
        }

        SESSIONS.remove(player.getUUID());
        if (selected.isRandom()) applyRandom(player, selected);
        else apply(player, selected.preview());
    }

    private static void apply(ServerPlayer player, LoadoutDefinition loadout) {
        try {
            CommandSourceStack source = silentCommandSource(player);
            int applied = player.server.getCommands().performPrefixedCommand(source, "loadout apply " + loadout.id() + " @s");
            if (applied <= 0) {
                player.displayClientMessage(Component.literal("Failed to apply saved loadout: " + loadout.id()), false);
                return;
            }
            ResourceLocation functionId = loadout.applyFunction();
            if (functionId != null) {
                Optional<CommandFunction> function = player.server.getFunctions().get(functionId);
                if (function.isEmpty()) {
                    player.displayClientMessage(Component.literal("After-apply function not found: " + functionId), false);
                    return;
                }
                player.server.getFunctions().execute(function.get(), source);
            }
            DpvpTweaks.LOGGER.info("[{}] Applied saved loadout '{}' to {} (after_apply={})",
                    DpvpTweaks.MOD_NAME, loadout.id(), player.getGameProfile().getName(), functionId);
            player.displayClientMessage(Component.literal("Applied loadout: " + loadout.name()), false);
        } catch (Exception e) {
            DpvpTweaks.LOGGER.error("[{}] Failed to apply saved loadout '{}'", DpvpTweaks.MOD_NAME, loadout.id(), e);
            player.displayClientMessage(Component.literal("Failed to apply loadout: " + loadout.name()), false);
        }
    }

    private static void applyRandom(ServerPlayer player, LoadoutDataManager.AvailableLoadout selected) {
        LoadoutSetDefinition.Entry entry = selected.entry();
        RandomLoadoutProfileManager.DrawResult draw = RandomLoadoutProfileManager.draw(entry.random(), player.getRandom());
        if (!draw.valid()) {
            DpvpTweaks.LOGGER.warn("[{}] Random loadout '{}' could not be drawn for {}: {}", DpvpTweaks.MOD_NAME,
                    entry.id(), player.getGameProfile().getName(), draw.error());
            player.displayClientMessage(Component.literal("ランダム武器を抽選できませんでした: " + draw.error()), false);
            return;
        }

        if (entry.afterApply() != null && player.server.getFunctions().get(entry.afterApply()).isEmpty()) {
            player.displayClientMessage(Component.literal("After-apply function not found: " + entry.afterApply()), false);
            return;
        }

        try {
            CommandSourceStack source = silentCommandSource(player);
            int applied = player.server.getCommands().performPrefixedCommand(source,
                    "loadout apply " + entry.random().template() + " @s");
            if (applied <= 0) {
                player.displayClientMessage(Component.literal("ランダムロードアウトのテンプレートを適用できませんでした: "
                        + entry.random().template()), false);
                return;
            }
            for (int slot = 0; slot < draw.weapons().size(); slot++) {
                player.getInventory().setItem(slot, draw.weapons().get(slot).copy());
            }
            player.containerMenu.broadcastChanges();
            executeAfterApply(player, source, entry.afterApply());
            DpvpTweaks.LOGGER.info("[{}] Applied random loadout '{}' (profile={}, template={}) to {}",
                    DpvpTweaks.MOD_NAME, entry.id(), entry.random().profile(), entry.random().template(),
                    player.getGameProfile().getName());
            player.displayClientMessage(Component.literal("ランダムロードアウトを適用しました: " + selected.preview().name()), false);
        } catch (Exception exception) {
            DpvpTweaks.LOGGER.error("[{}] Failed to apply random loadout '{}'", DpvpTweaks.MOD_NAME, entry.id(), exception);
            player.displayClientMessage(Component.literal("ランダムロードアウトを適用できませんでした: " + selected.preview().name()), false);
        }
    }

    private static void executeAfterApply(ServerPlayer player, CommandSourceStack source, ResourceLocation functionId) {
        if (functionId == null) return;
        Optional<CommandFunction> function = player.server.getFunctions().get(functionId);
        if (function.isEmpty()) throw new IllegalStateException("After-apply function not found: " + functionId);
        player.server.getFunctions().execute(function.get(), source);
    }

    private static CommandSourceStack silentCommandSource(ServerPlayer player) {
        return player.createCommandSourceStack()
                .withSource(CommandSource.NULL)
                .withPermission(2)
                .withSuppressedOutput();
    }

    private static Map<String, LoadoutDataManager.AvailableLoadout> byId(List<LoadoutDataManager.AvailableLoadout> loadouts) {
        Map<String, LoadoutDataManager.AvailableLoadout> result = new LinkedHashMap<>();
        for (LoadoutDataManager.AvailableLoadout loadout : loadouts) {
            result.put(loadout.preview().id(), loadout);
        }
        return Map.copyOf(result);
    }

    private static void cleanupExpiredSessions() {
        SESSIONS.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private record Session(long sessionId, ResourceLocation setId, Map<String, LoadoutDataManager.AvailableLoadout> loadouts, long createdAtMillis) {
        private boolean isExpired() {
            return System.currentTimeMillis() - createdAtMillis > SESSION_TTL_MS;
        }
    }
}
