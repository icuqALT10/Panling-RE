package icu.icuqalt10.panlingre.effect;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.entity.PanLingEntities;
import icu.icuqalt10.panlingre.network.FreezeSyncPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.network.PacketDistributor;

public class FreezeEffect extends MobEffect {
    public FreezeEffect() {
        super(MobEffectCategory.HARMFUL, 0xA8E8FF);
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "freeze_movement"),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                amplifier -> -1.0
        );
        addAttributeModifier(
                Attributes.JUMP_STRENGTH,
                ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "freeze_jump"),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                amplifier -> -1.0
        );
    }

    public static boolean canApplyTo(LivingEntity entity) {
        return !entity.isInvulnerable()
                && (!(entity instanceof Mob mob) || !mob.isNoAi());
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        updateFrozenTicks(entity);

        if (entity.level().isClientSide) return;

        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                entity,
                new FreezeSyncPayload(entity.getUUID(), true)
        );

        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
        }

        if (entity instanceof PanLingEntities panLingEntity) {
            panLingEntity.whenFroozen();
        }
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        updateFrozenTicks(entity);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    private void updateFrozenTicks(LivingEntity entity) {
        for (MobEffectInstance instance : entity.getActiveEffects()) {
            if (instance.getEffect().value() == this) {
                int remaining = instance.isInfiniteDuration()
                        ? 2
                        : Math.max(instance.getDuration(), 0);
                // Vanilla thaws entities by 2 frozen ticks per game tick. Keeping
                // twice the remaining effect duration makes the final effect tick
                // fall from 142 to exactly 140 when the effect expires.
                long target = 140L + 2L * remaining;
                entity.setTicksFrozen((int) Math.min(target, Integer.MAX_VALUE));
                return;
            }
        }
    }

    public static void removeFrom(LivingEntity entity) {
        if (entity.level().isClientSide) {
            return;
        }

        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                entity,
                new FreezeSyncPayload(entity.getUUID(), false)
        );

        if (entity instanceof Mob mob) {
            mob.setNoAi(false);
        }

        if (entity instanceof PanLingEntities panLingEntity) {
            panLingEntity.whenUnFroozen();
        }

    }
}
