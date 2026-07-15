package com.dousiyo.dpvptweaks.inteldraft;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.network.CloseIntelDraftGuiPacket;
import com.dousiyo.dpvptweaks.network.LoadoutGuiNetwork;
import com.dousiyo.dpvptweaks.network.OpenIntelDraftGuiPacket;
import com.dousiyo.dpvptweaks.network.IntelDraftStatePacket;
import com.dousiyo.dpvptweaks.secretoperations.SecretOperationsManager;
import com.dousiyo.dpvptweaks.secretoperations.SecretShowdownManager;
import com.dousiyo.dpvptweaks.secretoperations.SecretConvoyManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** Server-authoritative, allocation-light match state. No global per-tick player scan. */
public final class IntelDraftManager {
    private static final Map<UUID, PlayerState> STATES = new ConcurrentHashMap<>();

    private IntelDraftManager() {}

    public static boolean hasState(ServerPlayer player) { return STATES.containsKey(player.getUUID()); }
    public static void syncOnLogin(ServerPlayer player) {
        syncState(player, STATES.get(player.getUUID()));
        SecretOperationsManager.sync(player);
    }

    public static boolean hasTech(ServerPlayer player, String effectType) {
        PlayerState state = STATES.get(player.getUUID());
        return state != null && state.effects.containsKey(effectType);
    }

    public static Optional<IntelDraftDefinition.TechDefinition> findTech(ServerPlayer player, String effectType) {
        PlayerState state = STATES.get(player.getUUID());
        return state == null ? Optional.empty() : Optional.ofNullable(state.effects.get(effectType));
    }

    public static Set<ResourceLocation> acquiredIds(ServerPlayer player) {
        PlayerState state = STATES.get(player.getUUID());
        return state == null ? Set.of() : Set.copyOf(state.techs.keySet());
    }

    public static boolean open(ServerPlayer player) {
        PlayerState state = ensureState(player);
        IntelDraftDefinition.Pool pool = IntelDraftDefinitionLoader.get();
        if (pool.guns().isEmpty() || pool.attachments().isEmpty()) {
            player.sendSystemMessage(Component.literal("Intel Draftの銃またはアタッチメント定義が空です"));
            return false;
        }
        state.session = createSession(state, pool, pool.rerollCount(), null,
                System.currentTimeMillis() + pool.sessionSeconds() * 1000L, false, true);
        send(player, state);
        return true;
    }

    /** Standalone debug entry point. Each invocation starts from a clean draft state. */
    public static boolean openDebug(ServerPlayer player) {
        end(player);
        return open(player);
    }

    /** Opens a match-owned draft. A zero expiration keeps the exact candidates until selection/end. */
    public static boolean openMatch(ServerPlayer player, boolean closeAllowed) {
        return openMatch(player, closeAllowed, 0L);
    }

    public static boolean openMatch(ServerPlayer player, boolean closeAllowed, long expiresAtMillis) {
        PlayerState state = ensureState(player);
        IntelDraftDefinition.Pool pool = IntelDraftDefinitionLoader.get();
        if (pool.guns().isEmpty() || pool.attachments().isEmpty()) return false;
        state.session = createSession(state, pool, pool.rerollCount(), null, Math.max(0L, expiresAtMillis), closeAllowed, false);
        send(player, state);
        return true;
    }

    public static boolean hasSession(ServerPlayer player) {
        PlayerState state = STATES.get(player.getUUID());
        return state != null && state.session != null;
    }

    public static boolean reopenCurrent(ServerPlayer player, boolean closeAllowed) {
        PlayerState state = STATES.get(player.getUUID());
        if (state == null || state.session == null) return false;
        Session old = state.session;
        state.session = new Session(old.id, old.expiresAt, old.remainingRerolls, old.choices, closeAllowed, old.enforceExpiry);
        send(player, state);
        return true;
    }

    public static boolean autoSelectCurrent(ServerPlayer player) {
        PlayerState state = STATES.get(player.getUUID());
        if (state == null || state.session == null || state.session.choices.isEmpty()) return false;
        Session old = state.session;
        // The server deadline owns automatic selection; bypass a simultaneous wall-clock GUI expiry.
        state.session = new Session(old.id, 0L, old.remainingRerolls, old.choices, old.closeAllowed, false);
        select(player, state.session.id, ThreadLocalRandom.current().nextInt(state.session.choices.size()));
        return true;
    }

    public static void reroll(ServerPlayer player, long sessionId) {
        PlayerState state = STATES.get(player.getUUID());
        Session old = validSession(player, state, sessionId);
        if (old == null || old.remainingRerolls <= 0) return;
        state.session = createSession(state, IntelDraftDefinitionLoader.get(), old.remainingRerolls - 1, old.choices,
                old.expiresAt, old.closeAllowed, old.enforceExpiry);
        send(player, state);
    }

    public static void select(ServerPlayer player, long sessionId, int index) {
        PlayerState state = STATES.get(player.getUUID());
        Session session = validSession(player, state, sessionId);
        if (session == null || index < 0 || index >= session.choices.size()) {
            if (session != null) { reject(player, "不正な選択番号です"); send(player, state); }
            return;
        }
        // Consume before side effects to make duplicate packets harmless.
        state.session = null;
        IntelDraftDefinition.ChoiceDefinition choice = session.choices.get(index);
        if (!choice.tech().isSupplyOnly() && !state.techs.containsKey(choice.tech().id())) {
            state.techs.put(choice.tech().id(), choice.tech());
            state.effects.put(choice.tech().effect().type(), choice.tech());
            IntelDraftTechEffects.acquire(player, choice.tech());
            syncState(player, state);
        }
        giveOrDrop(player, choice.gun().gunStack().copy());
        giveOrDrop(player, choice.attachment().attachmentStack().copy());
        grantAmmo(player, AmmoGrant.SELECT);
        runFunction(player, choice.tech().onSelectFunction());
        LoadoutGuiNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CloseIntelDraftGuiPacket());
        SecretShowdownManager.onDraftSelected(player);
        SecretConvoyManager.onDraftSelected(player);
    }

    public static void grantRespawnAmmo(ServerPlayer player) { if (hasState(player)) grantAmmo(player, AmmoGrant.RESPAWN); }
    public static void grantEliminationAmmo(ServerPlayer player) {
        if (hasTech(player, "resupply")) grantAmmo(player, AmmoGrant.ELIMINATION);
    }

    public static void grantAttachment(ServerPlayer player, ResourceLocation id, int count) {
        giveOrDrop(player, IntelDraftDefinitionLoader.attachmentStack(id, count));
    }

    public static void end(ServerPlayer player) {
        PlayerState state = STATES.remove(player.getUUID());
        if (state != null) IntelDraftTechEffects.clear(player, state.techs.values());
        syncState(player, null);
        SecretOperationsManager.sync(player);
        LoadoutGuiNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CloseIntelDraftGuiPacket());
    }

    public static void clearAll() { STATES.clear(); }
    public static void endAll(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) end(player);
        STATES.clear(); // Also removes retained states for players who logged out mid-match.
    }
    public static void invalidateSessions(net.minecraft.server.MinecraftServer server) {
        STATES.values().forEach(s -> s.session = null);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) close(player);
    }

    public static Component status(ServerPlayer player) {
        PlayerState state = STATES.get(player.getUUID());
        if (state == null) return Component.literal("Intel Draft: inactive");
        String names = state.techs.values().stream().map(IntelDraftDefinition.TechDefinition::name)
                .reduce((a, b) -> a + ", " + b).orElse("なし");
        return Component.literal("Intel Draft: active / 技術: " + names + (state.session == null ? "" : " / 選択待ち"));
    }

    private static Session createSession(PlayerState state, IntelDraftDefinition.Pool pool, int rerolls,
                                         List<IntelDraftDefinition.ChoiceDefinition> previous,
                                         long expiresAt, boolean closeAllowed, boolean enforceExpiry) {
        List<IntelDraftDefinition.ChoiceDefinition> choices = IntelDraftSampler.sample(pool, state.techs.keySet(), previous);
        long id;
        do id = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE); while (id == 0);
        return new Session(id, expiresAt, rerolls, List.copyOf(choices), closeAllowed, enforceExpiry);
    }

    private static PlayerState ensureState(ServerPlayer player) {
        PlayerState existing = STATES.get(player.getUUID());
        if (existing != null) return existing;
        PlayerState created = new PlayerState();
        PlayerState raced = STATES.putIfAbsent(player.getUUID(), created);
        PlayerState result = raced == null ? created : raced;
        if (raced == null) syncState(player, result);
        return result;
    }

    private static Session validSession(ServerPlayer player, PlayerState state, long id) {
        if (state == null || state.session == null || state.session.id != id) {
            reject(player, "無効なIntel Draftセッションです"); close(player); return null;
        }
        if (state.session.enforceExpiry && state.session.expiresAt > 0L && System.currentTimeMillis() > state.session.expiresAt) {
            state.session = null; reject(player, "Intel Draftの制限時間が切れました"); close(player); return null;
        }
        return state.session;
    }
    private static void reject(ServerPlayer player, String message) { player.sendSystemMessage(Component.literal(message)); }
    private static void close(ServerPlayer player) {
        LoadoutGuiNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CloseIntelDraftGuiPacket());
    }
    private static void send(ServerPlayer player, PlayerState state) {
        Session s = state.session;
        List<String> names = state.techs.values().stream().map(IntelDraftDefinition.TechDefinition::name).toList();
        LoadoutGuiNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenIntelDraftGuiPacket(
                new IntelDraftDefinition(s.id, s.remainingRerolls, s.expiresAt, s.closeAllowed, names, s.choices)));
    }
    private static void syncState(ServerPlayer player, PlayerState state) {
        Set<String> effects = state == null ? Set.of() : Set.copyOf(state.effects.keySet());
        LoadoutGuiNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new IntelDraftStatePacket(state != null, effects));
    }

    private static void grantAmmo(ServerPlayer player, AmmoGrant reason) {
        double multiplier = findTech(player, "overprepared").map(t -> t.effect().value("multiplier", 1.5)).orElse(1.0);
        for (IntelDraftDefinition.AmmoDefinition ammo : IntelDraftDefinitionLoader.get().ammo()) {
            int amount = switch (reason) {
                case SELECT -> ammo.onSelect(); case RESPAWN -> ammo.onRespawn(); case ELIMINATION -> ammo.onElimination();
            };
            if (reason != AmmoGrant.ELIMINATION) amount = amount == 0 ? 0 : Math.max(1, (int)Math.round(amount * multiplier));
            while (amount > 0) {
                int stackSize = Math.min(64, amount);
                giveAmmoToMainInventoryOrDrop(player, IntelDraftDefinitionLoader.ammoStack(ammo.id(), stackSize));
                amount -= stackSize;
            }
        }
    }

    /**
     * Ammunition must never consume or merge into hotbar slots (0-8). Prefer an
     * existing stack in the main inventory, then an empty main-inventory slot.
     */
    private static void giveAmmoToMainInventoryOrDrop(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;

        Inventory inventory = player.getInventory();
        ItemStack remaining = stack.copy();
        final int firstMainInventorySlot = 9;

        for (int slot = firstMainInventorySlot; slot < inventory.items.size() && !remaining.isEmpty(); slot++) {
            ItemStack existing = inventory.items.get(slot);
            if (existing.isEmpty() || !ItemStack.isSameItemSameTags(existing, remaining)) continue;

            int move = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
            if (move > 0) {
                existing.grow(move);
                remaining.shrink(move);
            }
        }

        for (int slot = firstMainInventorySlot; slot < inventory.items.size() && !remaining.isEmpty(); slot++) {
            if (!inventory.items.get(slot).isEmpty()) continue;

            int move = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            ItemStack inserted = remaining.copy();
            inserted.setCount(move);
            inventory.setItem(slot, inserted);
            remaining.shrink(move);
        }

        inventory.setChanged();
        if (!remaining.isEmpty()) dropOwned(player, remaining);
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!player.getInventory().add(stack)) dropOwned(player, stack);
    }

    private static void dropOwned(ServerPlayer player, ItemStack stack) {
        ItemEntity drop = player.drop(stack, false);
        if (drop != null) {
            drop.getPersistentData().putUUID("dpvptweaksIntelOwner", player.getUUID());
            drop.getPersistentData().putLong("dpvptweaksIntelOwnerUntil", player.level().getGameTime() + 100L);
        }
    }

    private static void runFunction(ServerPlayer player, ResourceLocation id) {
        if (id == null) return;
        player.server.getFunctions().get(id).ifPresent(function -> {
            CommandSourceStack source = player.createCommandSourceStack().withSuppressedOutput().withPermission(2);
            player.server.getFunctions().execute(function, source);
        });
    }

    private enum AmmoGrant { SELECT, RESPAWN, ELIMINATION }
    private static final class PlayerState {
        final LinkedHashMap<ResourceLocation, IntelDraftDefinition.TechDefinition> techs = new LinkedHashMap<>();
        final HashMap<String, IntelDraftDefinition.TechDefinition> effects = new HashMap<>();
        volatile Session session;
    }
    private record Session(long id, long expiresAt, int remainingRerolls,
                           List<IntelDraftDefinition.ChoiceDefinition> choices, boolean closeAllowed, boolean enforceExpiry) {}
}
