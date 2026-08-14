package icu.icuqalt10.panlingre.entity;

import icu.icuqalt10.panlingre.init.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class Ys3MuDomainEntity extends Ys3DomainEntity {
    private static final int DURATION = 60 * 20;

    public Ys3MuDomainEntity(EntityType<? extends Ys3MuDomainEntity> type, Level level) {
        super(type, level);
    }

    public Ys3MuDomainEntity(Level level, LivingEntity owner, Vec3 center,
                             ItemStack stack, float healValue) {
        super(ModEntities.YS3_MU_DOMAIN.get(), level, owner, center, stack, healValue, DURATION);
    }

    @Override
    protected void applyEffect(LivingEntity target, float healValue) {
        target.heal(healValue);
    }

    @Override
    protected void spawnClientParticles(int activeAge) {
        if (activeAge % 20 != 0) return;
        for (int i = 0; i < 300; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = Math.sqrt(random.nextDouble()) * getDomainRadius();
            level().addParticle(ParticleTypes.HAPPY_VILLAGER, true,
                    getX() + Math.cos(angle) * radius,
                    getY() + 0.2D + random.nextDouble() * 2.8D,
                    getZ() + Math.sin(angle) * radius,
                    0.0D, 0.025D, 0.0D);
        }
    }

}
