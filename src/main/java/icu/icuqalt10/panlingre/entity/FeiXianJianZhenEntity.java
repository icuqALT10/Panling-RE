package icu.icuqalt10.panlingre.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import icu.icuqalt10.panlingre.animation.SyncedEntityAnimation;
import icu.icuqalt10.panlingre.animation.WorldTimeAnimationController;
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
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class FeiXianJianZhenEntity extends Entity implements GeoEntity {
    private static final EntityDataAccessor<CompoundTag> ANIMATION =
            SynchedEntityData.defineId(FeiXianJianZhenEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private final SyncedEntityAnimation animation = new SyncedEntityAnimation(this, ANIMATION);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float summonerArmorValue = 0f;
    private int lifeTime = 0;
    private static final int MAX_LIFE = 200;
    Player player;

    public FeiXianJianZhenEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        animation.loop("animation", 1);
    }

    public void setSummonerArmor(float damage) {
        this.summonerArmorValue = damage;
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

        float damageValue = this.summonerArmorValue;

        AABB area = this.getBoundingBox().inflate(10.0D);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, area);

        Entity owner = this.getOwner();

        for (LivingEntity target : targets) {
            if (!target.isAttackable()) continue;

            if (owner == null || target.is(owner)) continue;

            target.hurt(this.damageSources().playerAttack((Player) owner), damageValue);

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
        controllers.add(new WorldTimeAnimationController<>(this, "controller", animation::playback,
                state -> level().getGameTime() + state.getPartialTick()));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        animation.save(tag, "AnimationPlayback");
        tag.putFloat("SummonerArmor", this.summonerArmorValue);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        animation.load(tag, "AnimationPlayback");
        this.summonerArmorValue = tag.getFloat("SummonerArmor");
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(ANIMATION, new CompoundTag()); }
}