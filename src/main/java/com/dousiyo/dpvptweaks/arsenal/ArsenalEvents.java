package com.dousiyo.dpvptweaks.arsenal;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.event.common.GunMeleeEvent;
import com.tacz.guns.api.event.common.GunShootEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ArsenalEvents {
    private ArsenalEvents() {}

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        ArsenalConfig.reload(); ArsenalWeaponSetManager.reload();
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) ArsenalMatchManager.serverTick(event.getServer());
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ArsenalMatchManager.playerLogin(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ArsenalMatchManager.handleDeath(player);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ArsenalMatchManager.playerRespawn(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void drops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer && ArsenalMatchManager.isParticipant((ServerPlayer) event.getEntity()))
            event.getDrops().removeIf(item -> ArsenalEquipmentService.isMatchEquipment(item.getItem()));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void toss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && runningParticipant(player)) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attackEntity(AttackEntityEvent event) {
        if (isProtected(event.getEntity())) { event.setCanceled(true); return; }
        if (event.getEntity() instanceof ServerPlayer attacker && event.getTarget() instanceof ServerPlayer victim
                && ArsenalMatchManager.finishedParticipant(attacker) && ArsenalMatchManager.finishedParticipant(victim))
            event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(LivingAttackEvent event) {
        if (blockedDamage(event.getEntity() instanceof ServerPlayer player ? player : null, event.getSource().getEntity()))
            event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hurt(LivingHurtEvent event) {
        if (blockedDamage(event.getEntity() instanceof ServerPlayer player ? player : null, event.getSource().getEntity()))
            event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interact(PlayerInteractEvent event) {
        if (isProtected(event.getEntity()) && event.isCancelable()) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void gunShoot(GunShootEvent event) {
        if (event.getLogicalSide() == LogicalSide.CLIENT
                ? com.dousiyo.dpvptweaks.client.arsenal.ClientArsenalState.blocksCombat()
                : event.getShooter() instanceof ServerPlayer player && ArsenalMatchManager.isProtected(player)) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void gunFire(GunFireEvent event) {
        if (event.getLogicalSide() == LogicalSide.CLIENT
                ? com.dousiyo.dpvptweaks.client.arsenal.ClientArsenalState.blocksCombat()
                : event.getShooter() instanceof ServerPlayer player && ArsenalMatchManager.isProtected(player)) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void gunMelee(GunMeleeEvent event) {
        if (event.getLogicalSide() == LogicalSide.CLIENT
                ? com.dousiyo.dpvptweaks.client.arsenal.ClientArsenalState.blocksCombat()
                : event.getShooter() instanceof ServerPlayer player && ArsenalMatchManager.isProtected(player)) event.setCanceled(true);
    }

    private static boolean blockedDamage(ServerPlayer victim, Entity directAttacker) {
        ServerPlayer attacker = playerOwner(directAttacker);
        if (victim != null && ArsenalMatchManager.isProtected(victim)) return true;
        if (attacker != null && ArsenalMatchManager.isProtected(attacker)) return true;
        return victim != null && attacker != null && ArsenalMatchManager.finishedParticipant(victim)
                && ArsenalMatchManager.finishedParticipant(attacker);
    }

    private static ServerPlayer playerOwner(Entity entity) {
        if (entity instanceof ServerPlayer player) return player;
        if (entity instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer owner) return owner;
        return null;
    }

    private static boolean isProtected(Player player) {
        return player instanceof ServerPlayer serverPlayer ? ArsenalMatchManager.isProtected(serverPlayer)
                : com.dousiyo.dpvptweaks.client.arsenal.ClientArsenalState.protectedState();
    }

    private static boolean runningParticipant(ServerPlayer player) {
        ArsenalSavedData data = ArsenalSavedData.get(player.server);
        return data.state == ArsenalMatchState.RUNNING && data.players.containsKey(player.getUUID());
    }
}
