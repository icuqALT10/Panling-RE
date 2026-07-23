package icu.icuqalt10.panlingre.effect;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.entity.PanLingEntities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

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

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return;
        }

        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
        }

        if (entity instanceof PanLingEntities panLingEntity) {
            panLingEntity.whenFroozen();
        }
    }

    public static void removeFrom(LivingEntity entity) {
        if (entity.level().isClientSide) {
            return;
        }

        if (entity instanceof Mob mob) {
            mob.setNoAi(false);
        }

        if (entity instanceof PanLingEntities panLingEntity) {
            panLingEntity.whenUnFroozen();
        }

    }
}
