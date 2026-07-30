package icu.icuqalt10.panlingre.entity;

import icu.icuqalt10.panlingre.init.ModEntities;
import icu.icuqalt10.panlingre.network.particle.HuoQiuExplosionParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class HuoQiuFuEntity extends ThrowableItemProjectile {
    private static final double EXPLOSION_RADIUS = 4.0;
    private static final double PARTICLE_TRACKING_RADIUS_SQR = 64.0 * 64.0;
    private static final double TNT_GRAVITY = 0.04;
    private static final double TNT_DRAG = 0.98;
    private static final double THROWABLE_DRAG = 0.99;
    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(HuoQiuFuEntity.class, EntityDataSerializers.FLOAT);

    public HuoQiuFuEntity(EntityType<? extends HuoQiuFuEntity> type, Level level) {
        super(type, level);
    }

    public HuoQiuFuEntity(Level level, LivingEntity shooter, float damage) {
        super(ModEntities.HUO_QIU_FU.get(), shooter, level);
        this.setDamage(damage);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DAMAGE, 0.0F);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.MAGMA_BLOCK;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -TNT_GRAVITY, 0.0));
        }

        super.tick();

        if (!this.isRemoved() && !this.isInWater()) {
            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(movement.scale(TNT_DRAG / THROWABLE_DRAG));
        }

        if (!this.isRemoved() && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.FLAME,
                    this.getX(), this.getY(), this.getZ(),
                    2,
                    0.2, 0.2, 0.2,
                    0.01
            );
        }
    }

    public void setDamage(float damage) {
        this.entityData.set(DATA_DAMAGE, damage);
    }

    public float getDamage() {
        return this.entityData.get(DATA_DAMAGE);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel serverLevel) {
            this.explode(serverLevel);
            this.discard();
        }
    }

    private void explode(ServerLevel level) {
        Entity owner = this.getOwner();
        double radiusSqr = EXPLOSION_RADIUS * EXPLOSION_RADIUS;

        level.getEntitiesOfClass(
                        LivingEntity.class,
                        this.getBoundingBox().inflate(EXPLOSION_RADIUS),
                        target -> target.isAlive()
                                && target != owner
                                && target.distanceToSqr(this) <= radiusSqr
                )
                .forEach(target -> target.hurt(
                        this.damageSources().explosion(this, owner),
                        this.getDamage()
                ));

        HuoQiuExplosionParticles particles = new HuoQiuExplosionParticles(this.position());
        level.players().stream()
                .filter(player -> player.distanceToSqr(this) <= PARTICLE_TRACKING_RADIUS_SQR)
                .forEach(player -> PacketDistributor.sendToPlayer(player, particles));

        level.playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS,
                1.0F, 1.0F
        );
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", this.getDamage());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Damage")) {
            this.setDamage(tag.getFloat("Damage"));
        }
    }
}
