package com.dousiyo.dpvptweaks.secretoperations;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.arsenal.ArsenalMatchManager;
import com.dousiyo.dpvptweaks.config.ServerConfig;
import com.dousiyo.dpvptweaks.network.secretoperations.DamageFeedbackPacket;
import com.dousiyo.dpvptweaks.network.secretoperations.SecretOperationsNetwork;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.tacz.guns.api.event.common.GunDamageSourcePart;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/** Sends authoritative final damage only to an enabled player who dealt it. */
@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SecretOperationsDamageFeedbackEvents {
    private static final Map<UUID, Float> ABSORPTION_BEFORE_DAMAGE = new HashMap<>();
    private static final Map<Entity, PendingTaczHit> TACZ_HITS = new IdentityHashMap<>();
    private static final Map<DamageSource, PendingTaczHit> TACZ_HITS_BY_SOURCE = new IdentityHashMap<>();

    private SecretOperationsDamageFeedbackEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void beginTaczHit(EntityHurtByGunEvent.Pre event) {
        if (event.isCanceled() || !(event.getAttacker() instanceof ServerPlayer attacker)
                || !DamageFeedbackManager.isEnabled()) return;
        PendingTaczHit hit = new PendingTaczHit(attacker, event.isHeadShot());
        TACZ_HITS.put(event.getBullet(), hit);
        TACZ_HITS_BY_SOURCE.put(event.getDamageSource(GunDamageSourcePart.NON_ARMOR_PIERCING), hit);
        TACZ_HITS_BY_SOURCE.put(event.getDamageSource(GunDamageSourcePart.ARMOR_PIERCING), hit);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void finishTaczHit(EntityHurtByGunEvent.Post event) {
        finishTaczHit(event.getBullet(),
                event.getDamageSource(GunDamageSourcePart.NON_ARMOR_PIERCING),
                event.getDamageSource(GunDamageSourcePart.ARMOR_PIERCING));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void finishFatalTaczHit(EntityKillByGunEvent event) {
        finishTaczHit(event.getBullet(),
                event.getDamageSource(GunDamageSourcePart.NON_ARMOR_PIERCING),
                event.getDamageSource(GunDamageSourcePart.ARMOR_PIERCING));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void rememberAbsorption(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (event.isCanceled() || !isEligibleHit(event.getSource().getEntity(), victim)) return;
        ABSORPTION_BEFORE_DAMAGE.put(victim.getUUID(), victim.getAbsorptionAmount());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void sendDamageFeedback(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();

        Float absorptionBefore = ABSORPTION_BEFORE_DAMAGE.remove(victim.getUUID());
        if (event.isCanceled() || !(event.getSource().getEntity() instanceof ServerPlayer attacker)
                || !isEligibleHit(attacker, victim)) return;

        float healthDamage = Math.max(0.0F, event.getAmount());
        float shieldDamage = absorptionBefore == null ? 0.0F
                : Math.max(0.0F, absorptionBefore - victim.getAbsorptionAmount());
        if (healthDamage <= 0.0F && shieldDamage <= 0.0F) return;

        PendingTaczHit taczHit = TACZ_HITS_BY_SOURCE.get(event.getSource());
        if (taczHit != null) {
            taczHit.add(victim, healthDamage, shieldDamage);
            return;
        }

        SecretOperationsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> attacker),
                new DamageFeedbackPacket(victim.getId(), healthDamage, shieldDamage, false));
    }

    private static void finishTaczHit(Entity bullet, DamageSource nonArmorPiercing, DamageSource armorPiercing) {
        PendingTaczHit hit = TACZ_HITS.remove(bullet);
        TACZ_HITS_BY_SOURCE.remove(nonArmorPiercing);
        TACZ_HITS_BY_SOURCE.remove(armorPiercing);
        if (hit == null || hit.victim == null
                || (hit.healthDamage <= 0.0F && hit.shieldDamage <= 0.0F)) return;
        SecretOperationsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> hit.attacker),
                new DamageFeedbackPacket(hit.victim.getId(), hit.healthDamage, hit.shieldDamage, hit.headshot));
    }

    private static boolean isEligibleHit(Object sourceEntity, LivingEntity victim) {
        if (!(sourceEntity instanceof ServerPlayer attacker) || attacker == victim) return false;
        if (!DamageFeedbackManager.isEnabled()) return false;
        if (victim instanceof ServerPlayer victimPlayer
                && ArsenalMatchManager.activeMatch(attacker.server)
                && ArsenalMatchManager.isParticipant(attacker)
                && ArsenalMatchManager.isParticipant(victimPlayer)) return true;
        if (victim instanceof ServerPlayer victimPlayer) {
            boolean showdownPlayers = SecretShowdownManager.isParticipant(attacker)
                    && SecretShowdownManager.isParticipant(victimPlayer);
            boolean convoyPlayers = SecretConvoyManager.isParticipant(attacker)
                    && SecretConvoyManager.isParticipant(victimPlayer);
            if (showdownPlayers || convoyPlayers) return !attacker.isAlliedTo(victimPlayer);
        }
        return ServerConfig.DAMAGE_FEEDBACK_ENABLED.get() && !attacker.isAlliedTo(victim);
    }

    private static final class PendingTaczHit {
        private final ServerPlayer attacker;
        private final boolean headshot;
        private LivingEntity victim;
        private float healthDamage;
        private float shieldDamage;

        private PendingTaczHit(ServerPlayer attacker, boolean headshot) {
            this.attacker = attacker;
            this.headshot = headshot;
        }

        private void add(LivingEntity victim, float healthDamage, float shieldDamage) {
            this.victim = victim;
            this.healthDamage += healthDamage;
            this.shieldDamage += shieldDamage;
        }
    }
}
