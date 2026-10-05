package icu.icuqalt10.panlingre.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class TombTrapArrowEntity extends Arrow {
    public TombTrapArrowEntity(EntityType<? extends TombTrapArrowEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        pickup = Pickup.DISALLOWED;
    }

    @Override public void tick() {
        Vec3 velocity = getDeltaMovement();
        super.tick();
        if (!isRemoved()) setDeltaMovement(velocity); // No drag or drop: a straight, constant-speed trap shot.
        if (!level().isClientSide && (inGround || tickCount >= 100)) discard();
    }
    @Override protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!level().isClientSide) discard();
    }
    @Override protected void onHitEntity(EntityHitResult hit) {
        // Vanilla arrows multiply base damage by speed and round upward.
        setBaseDamage((10 - 1e-7) / getDeltaMovement().length());
        super.onHitEntity(hit);
    }
    @Override protected void doKnockback(LivingEntity entity, DamageSource source) {
        double resistance = Math.max(0, 1 - entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        Vec3 impulse = getDeltaMovement().multiply(1, 0, 1).normalize().scale(10 * .6 * resistance);
        entity.push(impulse.x, .1, impulse.z);
    }
}
