package io.github.meatwo310.dpvptweaks.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.github.meatwo310.dpvptweaks.config.ServerConfig;
import io.github.meatwo310.dpvptweaks.entity.ThrownStone;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class ThrowableStoneItem extends Item {
    private final Multimap<Attribute, AttributeModifier> defaultModifiers;

    public ThrowableStoneItem(Properties properties, double attackDamage, double attackSpeed) {
        super(properties);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                BASE_ATTACK_DAMAGE_UUID,
                "Weapon modifier",
                attackDamage,
                AttributeModifier.Operation.ADDITION
        ));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                BASE_ATTACK_SPEED_UUID,
                "Weapon modifier",
                attackSpeed,
                AttributeModifier.Operation.ADDITION
        ));
        this.defaultModifiers = builder.build();
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.SNOWBALL_THROW,
                SoundSource.NEUTRAL,
                0.5F,
                0.5F / (level.getRandom().nextFloat() * 0.4F + 0.8F)
        );


        int cooldown = this.getCooldown();
        if (cooldown > 0) {
            player.getCooldowns().addCooldown(this, cooldown);
        }

        if (!level.isClientSide) {
            ThrowableItemProjectile stone = this.getEntity(level, player);
            stone.setItem(itemStack);
            stone.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(stone);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            itemStack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    @SuppressWarnings("deprecation")
    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
    }

    public abstract int getCooldown();
    public abstract ThrowableItemProjectile getEntity(Level level, Player player);

    public static class ThrowableValine1gItem extends ThrowableStoneItem {
        public ThrowableValine1gItem(Properties properties) {
            super(properties, 5.0 - 1.0, -3.0);
        }

        @Override
        public int getCooldown() {
            return ServerConfig.VALINE1G_COOLDOWN.get();
        }

        @Override
        public ThrowableItemProjectile getEntity(Level level, Player player) {
            return new ThrownStone.ThrownValine1g(level, player);
        }
    }

    public static class ThrowableValine2gItem extends ThrowableStoneItem {
        public ThrowableValine2gItem(Properties properties) {
            super(properties, 10.0 - 1.0, -3.0);
        }

        @Override
        public int getCooldown() {
            return ServerConfig.VALINE2G_COOLDOWN.get();
        }

        @Override
        public ThrowableItemProjectile getEntity(Level level, Player player) {
            return new ThrownStone.ThrownValine2g(level, player);
        }
    }

    public static class ThrowableValine3gItem extends ThrowableStoneItem {
        public ThrowableValine3gItem(Properties properties) {
            super(properties, 20.0 - 1.0, -3.0);
        }

        @Override
        public int getCooldown() {
            return ServerConfig.VALINE3G_COOLDOWN.get();
        }

        @Override
        public ThrowableItemProjectile getEntity(Level level, Player player) {
            return new ThrownStone.ThrownValine3g(level, player);
        }
    }
}
