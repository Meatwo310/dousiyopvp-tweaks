package com.dousiyo.dpvptweaks.inteldraft;

import com.dousiyo.dpvptweaks.DpvpTweaks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.dousiyo.dpvptweaks.temporarybuilding.TemporaryBuildingLoadout;

import java.util.Collection;
import java.util.UUID;

/** Event-driven tech effects. The only tick hook exits in O(1) for inactive players and runs active work at 2 Hz. */
@Mod.EventBusSubscriber(modid = DpvpTweaks.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IntelDraftTechEffects {
    private static final UUID CROUCH_SPEED = UUID.fromString("72fd37b9-705c-4b72-9a9d-af36f6324121");
    private static final UUID SWIM_SPEED = UUID.fromString("d9870447-122f-4ab7-b04b-d541b0370dc0");

    private IntelDraftTechEffects() {}

    static void acquire(ServerPlayer player, IntelDraftDefinition.TechDefinition tech) {
        if (tech.effect().type().equals("battle_ready")) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 60 * 60, 0, false, false));
        }
        if (tech.effect().type().equals("incendiary_ammo")) {
            IntelDraftManager.grantAttachment(player,
                    ResourceLocation.fromNamespaceAndPath("tacz", "ammo_mod_i"), 1);
        }
        if (tech.effect().type().equals("building_supplies")) TemporaryBuildingLoadout.grantExtraMaterials(player);
        if (tech.effect().type().equals("building_tool_upgrade")) TemporaryBuildingLoadout.upgradeTool(player);
    }

    static void clear(ServerPlayer player, Collection<IntelDraftDefinition.TechDefinition> techs) {
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(CROUCH_SPEED);
        var swim = player.getAttribute(ForgeMod.SWIM_SPEED.get());
        if (swim != null) swim.removeModifier(SWIM_SPEED);
        // Only remove absorption if this system supplied Battle Ready.
        if (techs.stream().anyMatch(t -> t.effect().type().equals("battle_ready") || t.effect().type().equals("shield_drip")))
            player.removeEffect(MobEffects.ABSORPTION);
        var data = player.getPersistentData();
        data.remove("dpvptweaksIntelDoubleJumpUsed");
        data.remove("dpvptweaksIntelLastDamage");
        data.remove("dpvptweaksIntelEmergencySpeed");
        data.remove("dpvptweaksIntelWeakTarget");
        data.remove("dpvptweaksIntelWeakHits");
        data.remove("dpvptweaksIntelFieldMedic");
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer victim) {
            victim.getPersistentData().putLong("dpvptweaksIntelLastDamage", victim.level().getGameTime());
            if (event.getSource().is(DamageTypes.FALL)) {
                IntelDraftManager.findTech(victim, "soft_landing").ifPresent(t ->
                        event.setAmount((float)(event.getAmount() * (1.0 - t.effect().value("reduction", 0.4)))));
            }
            IntelDraftManager.findTech(victim, "emergency_speed").ifPresent(t -> {
                long now = victim.level().getGameTime();
                long last = victim.getPersistentData().getLong("dpvptweaksIntelEmergencySpeed");
                if (now - last >= (long)t.effect().value("cooldownTicks", 200)) {
                    victim.getPersistentData().putLong("dpvptweaksIntelEmergencySpeed", now);
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                            (int)t.effect().value("durationTicks", 60), 0, false, false));
                }
            });
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && event.getEntity() instanceof Player target) {
            double multiplier = 1.0;
            var brink = IntelDraftManager.findTech(attacker, "from_the_brink");
            if (brink.isPresent() && attacker.getHealth() <= attacker.getMaxHealth() * brink.get().effect().value("healthRatio", 0.3))
                multiplier *= 1.0 + brink.get().effect().value("bonus", 0.2);
            var air = IntelDraftManager.findTech(attacker, "air_attack");
            if (air.isPresent() && !attacker.onGround()) multiplier *= 1.0 + air.get().effect().value("bonus", 0.15);
            event.setAmount((float)(event.getAmount() * multiplier));
            IntelDraftManager.findTech(attacker, "tracer_rounds").ifPresent(t -> target.addEffect(new MobEffectInstance(
                    MobEffects.GLOWING, (int)t.effect().value("durationTicks", 100), 0, false, false)));
            IntelDraftManager.findTech(attacker, "weakness_analysis").ifPresent(t -> {
                var data = attacker.getPersistentData();
                String current = data.getString("dpvptweaksIntelWeakTarget");
                String targetId = target.getUUID().toString();
                int hits = current.equals(targetId) ? data.getInt("dpvptweaksIntelWeakHits") + 1 : 1;
                if (hits >= (int)t.effect().value("hits", 3)) {
                    event.setAmount((float)(event.getAmount() * (1.0 + t.effect().value("bonus", 0.15))));
                    hits = 0;
                }
                data.putString("dpvptweaksIntelWeakTarget", targetId);
                data.putInt("dpvptweaksIntelWeakHits", hits);
            });
        }
    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) IntelDraftManager.findTech(player, "strong_resolve")
                .ifPresent(t -> event.setStrength((float)(event.getStrength() * (1.0 - t.effect().value("reduction", 0.35)))));
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer) || !(event.getEntity() instanceof ServerPlayer dead)) return;
        if (killer == dead || (killer.getTeam() != null && killer.getTeam() == dead.getTeam())) return;
        IntelDraftManager.findTech(killer, "reinvigorated").ifPresent(t ->
                killer.heal((float)t.effect().value("heal", 4.0)));
        IntelDraftManager.grantEliminationAmmo(killer);
        IntelDraftManager.findTech(killer, "informant").ifPresent(t -> {
            double radius = t.effect().value("radius", 20.0);
            int duration = (int)t.effect().value("durationTicks", 100);
            for (ServerPlayer nearby : killer.serverLevel().players()) {
                if (nearby != killer && nearby.isAlive() && nearby.distanceToSqr(killer) <= radius * radius
                        && (killer.getTeam() == null || killer.getTeam() != nearby.getTeam()))
                    nearby.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false));
            }
        });
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !IntelDraftManager.hasState(player)) return;
        IntelDraftManager.grantRespawnAmmo(player);
        IntelDraftManager.findTech(player, "battle_ready").ifPresent(t -> player.addEffect(new MobEffectInstance(
                MobEffects.ABSORPTION, 20 * 60 * 60, (int)t.effect().value("amplifier", 0), false, false)));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) IntelDraftManager.syncOnLogin(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || (player.tickCount % 10) != 0 || !IntelDraftManager.hasState(player)) return;
        if (player.onGround()) player.getPersistentData().putBoolean("dpvptweaksIntelDoubleJumpUsed", false);
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            var tech = IntelDraftManager.findTech(player, "sneaky");
            boolean wanted = tech.isPresent() && player.isCrouching();
            if (wanted && speed.getModifier(CROUCH_SPEED) == null) speed.addTransientModifier(new AttributeModifier(
                    CROUCH_SPEED, "Intel Draft crouch speed", tech.get().effect().value("bonus", 0.2), AttributeModifier.Operation.MULTIPLY_TOTAL));
            else if (!wanted && speed.getModifier(CROUCH_SPEED) != null) speed.removeModifier(CROUCH_SPEED);
        }
        var swim = player.getAttribute(ForgeMod.SWIM_SPEED.get());
        if (swim != null) {
            var tech = IntelDraftManager.findTech(player, "part_fish");
            boolean wanted = tech.isPresent() && player.isInWater();
            if (wanted && swim.getModifier(SWIM_SPEED) == null) swim.addTransientModifier(new AttributeModifier(
                    SWIM_SPEED, "Intel Draft swim speed", tech.get().effect().value("bonus", 0.35), AttributeModifier.Operation.MULTIPLY_TOTAL));
            else if (!wanted && swim.getModifier(SWIM_SPEED) != null) swim.removeModifier(SWIM_SPEED);
        }
        if (IntelDraftManager.hasTech(player, "hasty_harvest") && player.getMainHandItem().getItem() instanceof DiggerItem)
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 15, 0, false, false));
        IntelDraftManager.findTech(player, "shield_drip").ifPresent(t -> {
            long lastDamage = player.getPersistentData().getLong("dpvptweaksIntelLastDamage");
            if (player.level().getGameTime() - lastDamage >= (long)t.effect().value("delayTicks", 100)
                    && player.getAbsorptionAmount() < t.effect().value("max", 8.0))
                player.setAbsorptionAmount((float)Math.min(t.effect().value("max", 8.0), player.getAbsorptionAmount() + t.effect().value("perPulse", 0.25)));
        });
    }

    @SubscribeEvent
    public static void onUseFinished(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.getItem().isEdible()) return;
        IntelDraftManager.findTech(player, "field_medic").ifPresent(t -> {
            long now = player.level().getGameTime();
            long last = player.getPersistentData().getLong("dpvptweaksIntelFieldMedic");
            if (now - last >= (long)t.effect().value("cooldownTicks", 160)) {
                player.getPersistentData().putLong("dpvptweaksIntelFieldMedic", now);
                player.heal((float)t.effect().value("heal", 2));
            }
        });
    }

    @SubscribeEvent
    public static void onProtectedPickup(EntityItemPickupEvent event) {
        var tag = event.getItem().getPersistentData();
        if (!tag.hasUUID("dpvptweaksIntelOwner") || event.getItem().level().getGameTime() >= tag.getLong("dpvptweaksIntelOwnerUntil")) return;
        if (!event.getEntity().getUUID().equals(tag.getUUID("dpvptweaksIntelOwner"))) event.setCanceled(true);
    }
}
