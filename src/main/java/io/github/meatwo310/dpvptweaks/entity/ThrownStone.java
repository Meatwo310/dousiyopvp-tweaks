package io.github.meatwo310.dpvptweaks.entity;

import io.github.meatwo310.dpvptweaks.config.ServerConfig;
import io.github.meatwo310.dpvptweaks.item.ModItems;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class ThrownStone extends ThrowableItemProjectile {
    public ThrownStone(EntityType<? extends ThrownStone> type, Level level) {
        super(type, level);
    }

    public ThrownStone(Level level, LivingEntity owner) {
        super(null, owner, level);
    }

    public ThrownStone(EntityType<? extends ThrowableItemProjectile> type, LivingEntity owner, Level level) {
        super(type, owner, level);
    }

    @Override
    protected abstract Item getDefaultItem();

    private ParticleOptions getParticle() {
        ItemStack stack = getItemRaw();
        return new ItemParticleOption(ParticleTypes.ITEM, stack.isEmpty() ? getDefaultItem().getDefaultInstance() : stack);
    }

    @Override
    public void handleEntityEvent(byte eventId) {
        if (eventId == 3) {
            ParticleOptions particleOption = this.getParticle();
            for (int i = 0; i < 8; ++i) {
                this.level().addParticle(particleOption, this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F, 0.0F);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        Entity targetEntity = hitResult.getEntity();

        DamageSource source = this.damageSources().thrown(this, this.getOwner());
        targetEntity.hurt(source, this.getDamage());
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }

    protected abstract float getDamage();

    public static class ThrownValine1g extends ThrownStone {
        public ThrownValine1g(EntityType<? extends ThrownStone> type, Level level) {
            super(type, level);
        }

        public ThrownValine1g(Level level, LivingEntity owner) {
            super(ModEntities.THROWN_VALINE1G.get(), owner, level);
        }

        @Override
        protected Item getDefaultItem() {
            return ModItems.VALINE1G.get();
        }

        @Override
        protected float getDamage() {
            return ServerConfig.VALINE1G_DAMAGE.get().floatValue();
        }


    }

    public static class ThrownValine2g extends ThrownStone {
        public ThrownValine2g(EntityType<? extends ThrownStone> type, Level level) {
            super(type, level);
        }
        
        public ThrownValine2g(Level level, LivingEntity owner) {
            super(ModEntities.THROWN_VALINE2G.get(), owner, level);
        }

        @Override
        protected Item getDefaultItem() {
            return ModItems.VALINE2G.get();
        }

        @Override
        protected float getDamage() {
            return ServerConfig.VALINE2G_DAMAGE.get().floatValue();
        }
    }

    public static class ThrownValine3g extends ThrownStone {
        public ThrownValine3g(EntityType<? extends ThrownStone> type, Level level) {
            super(type, level);
        }
        
        public ThrownValine3g(Level level, LivingEntity owner) {
            super(ModEntities.THROWN_VALINE3G.get(), owner, level);
        }

        @Override
        protected Item getDefaultItem() {
            return ModItems.VALINE3G.get();
        }

        @Override
        protected float getDamage() {
            return ServerConfig.VALINE3G_DAMAGE.get().floatValue();
        }
    }
}