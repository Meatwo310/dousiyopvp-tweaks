package com.dousiyo.dpvptweaks.secretoperations;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import com.dousiyo.dpvptweaks.network.DamageFeedbackPacket;
import com.dousiyo.dpvptweaks.network.SecretOperationsNetwork;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.GunDamageSourcePart;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
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
    private static final Map<DamageSource, Boolean> TACZ_HEADSHOT_SOURCES = new IdentityHashMap<>();

    private SecretOperationsDamageFeedbackEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void rememberTaczHeadshot(EntityHurtByGunEvent.Pre event) {
        if (!(event.getAttacker() instanceof ServerPlayer) || !event.isHeadShot()) return;
        TACZ_HEADSHOT_SOURCES.put(event.getDamageSource(GunDamageSourcePart.NON_ARMOR_PIERCING), true);
        TACZ_HEADSHOT_SOURCES.put(event.getDamageSource(GunDamageSourcePart.ARMOR_PIERCING), true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void clearTaczHeadshot(EntityHurtByGunEvent.Post event) {
        TACZ_HEADSHOT_SOURCES.remove(event.getDamageSource(GunDamageSourcePart.NON_ARMOR_PIERCING));
        TACZ_HEADSHOT_SOURCES.remove(event.getDamageSource(GunDamageSourcePart.ARMOR_PIERCING));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void rememberAbsorption(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (event.isCanceled() || !isEnemySecretOperationsHit(event.getSource().getEntity(), victim)) return;
        ABSORPTION_BEFORE_DAMAGE.put(victim.getUUID(), victim.getAbsorptionAmount());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void sendDamageFeedback(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();

        Float absorptionBefore = ABSORPTION_BEFORE_DAMAGE.remove(victim.getUUID());
        if (event.isCanceled() || !(event.getSource().getEntity() instanceof ServerPlayer attacker)
                || !isEnemySecretOperationsHit(attacker, victim)) return;

        float healthDamage = Math.max(0.0F, event.getAmount());
        float shieldDamage = absorptionBefore == null ? 0.0F
                : Math.max(0.0F, absorptionBefore - victim.getAbsorptionAmount());
        if (healthDamage <= 0.0F && shieldDamage <= 0.0F) return;
        boolean headshot = TACZ_HEADSHOT_SOURCES.containsKey(event.getSource());

        SecretOperationsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> attacker),
                new DamageFeedbackPacket(victim.getId(), healthDamage, shieldDamage, headshot));
    }

    private static boolean isEnemySecretOperationsHit(Object sourceEntity, LivingEntity victim) {
        if (!(sourceEntity instanceof ServerPlayer attacker) || attacker == victim) return false;
        boolean secretOperationsActive = victim instanceof ServerPlayer victimPlayer
                && SecretOperationsManager.isActive(attacker)
                && SecretOperationsManager.isActive(victimPlayer);
        if (!DamageFeedbackManager.isEnabled(attacker) && !secretOperationsActive) return false;
        return !attacker.isAlliedTo(victim);
    }
}
