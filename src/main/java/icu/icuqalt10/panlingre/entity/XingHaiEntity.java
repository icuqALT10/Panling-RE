package icu.icuqalt10.panlingre.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class XingHaiEntity extends Entity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float summonerArrowValue = 0f;
    private int lifeTime = 0;
    private static final int MAX_LIFE = 200;
    Player player;

    public XingHaiEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setSummonerArrow(float arrow) {
        this.summonerArrowValue = arrow * 2f;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (this.tickCount % 20 == 0) {
                applyAreaDamage();
            }

            lifeTime++;
            if (lifeTime >= MAX_LIFE) {
                this.discard();
            }
        }
    }

    private void applyAreaDamage() {
        if (this.level().isClientSide) return;

        float damageValue = this.summonerArrowValue;

        AABB area = this.getBoundingBox().inflate(10.0D);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, area);

        Entity owner = this.getOwner();

        for (LivingEntity target : targets) {
            if (!target.isAttackable()) continue;

            if (owner != null && target.is(owner)) continue;

            if (owner instanceof LivingEntity livingOwner) {
                if (livingOwner.isAlliedTo(target)) continue;
            }

            target.hurt(this.damageSources().arrow(null,owner), damageValue);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                        target.getX(), target.getY(0.5D), target.getZ(),
                        5, 0.2D, 0.2D, 0.2D, 0.1D);
            }
        }
    }

    public void setOwner(Player player) {
        this.player = player;
    }

    private Entity getOwner() {
        return this.player;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("SummonerArrow", this.summonerArrowValue);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.summonerArrowValue = tag.getFloat("SummonerArrow");
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
}