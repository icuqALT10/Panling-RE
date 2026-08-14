package icu.icuqalt10.panlingre.entity;

import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class Ys3ShuiDomainEntity extends Ys3DomainEntity {
    private static final int DURATION = 60 * 20;

    public Ys3ShuiDomainEntity(EntityType<? extends Ys3ShuiDomainEntity> type, Level level) {
        super(type, level);
    }

    public Ys3ShuiDomainEntity(Level level, LivingEntity owner, Vec3 center,
                               ItemStack stack, float restoreValue) {
        super(ModEntities.YS3_SHUI_DOMAIN.get(), level, owner, center, stack, restoreValue, DURATION);
    }

    @Override
    protected void applyEffect(LivingEntity target, float restoreValue) {
        if (!(target instanceof Player player)) return;
        LingQiData data = player.getData(ModAttachments.LINGQI);
        data.setCurrent(data.getCurrent() + restoreValue, player);
        data.sync(player);
    }

    @Override
    public float getItemScale() {
        return 17.0F;
    }

    @Override
    protected void spawnClientParticles(int activeAge) {
        if (activeAge % 20 != 0) return;
        for (int i = 0; i < 160; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = Math.sqrt(random.nextDouble()) * getDomainRadius();
            level().addParticle(ParticleTypes.SPLASH, true,
                    getX() + Math.cos(angle) * radius,
                    getY() + 0.2D + random.nextDouble() * 4.6D,
                    getZ() + Math.sin(angle) * radius,
                    0.0D, 0.03D, 0.0D);
        }
    }

}
