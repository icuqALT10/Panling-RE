package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

import icu.icuqalt10.panlingre.init.ModEntities;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/** A dirt boulder rolls through players, stopping at walls or after eighty blocks. */
public final class GraveDragonRockEntity extends Projectile {
    private static final EntityDataAccessor<Float> TRAVEL = SynchedEntityData.defineId(GraveDragonRockEntity.class, EntityDataSerializers.FLOAT);
    private float damage = 20;

    public GraveDragonRockEntity(EntityType<? extends GraveDragonRockEntity> type, Level level) {
        super(type, level);
    }

    public GraveDragonRockEntity(Level level, GraveDragonEntity owner, Vec3 direction, float damage) {
        this(ModEntities.GRAVE_DRAGON_ROCK.get(), level);
        setOwner(owner);
        this.damage = damage;
        setDeltaMovement(direction.multiply(1, 0, 1).normalize().scale(GraveDragonMythic.RIFT_SPEED / 20));
        setYRot((float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TRAVEL, 0F);
    }

    public float travel() { return entityData.get(TRAVEL); }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            Vec3 motion = getDeltaMovement();
            Vec3 delta = motion.multiply(1, 0, 1);
            double remaining = GraveDragonMythic.RIFT_RANGE - travel();
            if (remaining <= 0) return;
            if (delta.length() > remaining) delta = delta.normalize().scale(remaining);
            int steps = Math.max(1, (int) Math.ceil(delta.length() / .25));
            double fall = (motion.y - .08) * .98;
            boolean grounded = false;
            Vec3 step = delta.scale(1.0 / steps).add(0, fall / steps, 0);
            for (int i = 0; i < steps; i++) {
                Step next = nextPosition(level(), this, position(), step);
                if (next == null) break;
                setPos(next.position());
                grounded = next.grounded();
            }
            setDeltaMovement(motion.x, grounded ? 0 : fall, motion.z);
            if (tickCount % 3 == 0) for (int i = 0; i < 24; i++)
                level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                        getX() + (random.nextDouble() - .5) * 4, getY() + random.nextDouble(),
                        getZ() + (random.nextDouble() - .5) * 4,
                        -getDeltaMovement().x * .2 + (random.nextDouble() - .5) * .15, .1 + random.nextDouble() * .2,
                        -getDeltaMovement().z * .2 + (random.nextDouble() - .5) * .15);
            return;
        }
        Vec3 velocity = getDeltaMovement();
        Vec3 delta = velocity.multiply(1, 0, 1);
        double remaining = GraveDragonMythic.RIFT_RANGE - travel();
        if (remaining <= 0) { finish(); return; }
        if (delta.length() > remaining) delta = delta.normalize().scale(remaining);
        int steps = Math.max(1, (int) Math.ceil(delta.length() / .25));
        double fall = (velocity.y - .08) * .98;
        Vec3 step = delta.scale(1.0 / steps).add(0, fall / steps, 0);
        Set<Integer> contacted = new HashSet<>();
        boolean grounded = false;
        for (int i = 0; i < steps; i++) {
            Step next = nextPosition(level(), this, position(), step);
            if (next == null) { finish(); return; }
            var nextBox = bounds(next.position());
            var swept = getBoundingBox().minmax(nextBox);
            for (var player : ((ServerLevel) level()).players()) {
                if (player.isAlive() && !player.isCreative() && !player.isSpectator()
                        && !contacted.contains(player.getId()) && swept.intersects(player.getBoundingBox())) {
                    contacted.add(player.getId());
                    if (player.hurt(damageSources().thrown(this, getOwner()), damage))
                        player.push(getDeltaMovement().x * .6, .35, getDeltaMovement().z * .6);
                }
            }
            setPos(next.position());
            grounded = next.grounded();
            entityData.set(TRAVEL, travel() + (float) delta.length() / steps);
        }
        setDeltaMovement(delta.x, grounded ? 0 : fall, delta.z);
        if (travel() >= GraveDragonMythic.RIFT_RANGE) finish();
    }

    private static AABB bounds(Vec3 position) {
        return new AABB(position.x - 2, position.y, position.z - 2, position.x + 2, position.y + 4, position.z + 2);
    }

    private record Step(Vec3 position, boolean grounded) { }

    private static Step nextPosition(Level level, Entity source, Vec3 position, Vec3 step) {
        Vec3 next = position.add(step);
        double floorY = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 5; i++) {
            double x = i == 0 ? 0 : i <= 2 ? -1.9 : 1.9;
            double z = i == 0 ? 0 : i % 2 == 1 ? -1.9 : 1.9;
            Vec3 probe = next.add(x, 0, z);
            var floor = level.clip(new ClipContext(probe.add(0, 1.1, 0), probe.add(0, -2.5, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
            if (floor.getType() == HitResult.Type.BLOCK && floor.getLocation().y >= next.y - .2
                    && floor.getLocation().y <= position.y + 1)
                floorY = Math.max(floorY, floor.getLocation().y);
        }
        boolean grounded = floorY != Double.NEGATIVE_INFINITY;
        if (grounded) next = new Vec3(next.x, floorY + .01, next.z);
        return level.getBlockCollisions(source, bounds(next)).iterator().hasNext() ? null : new Step(next, grounded);
    }

    static boolean canReach(GraveDragonEntity dragon, Vec3 origin, Vec3 direction, Player target) {
        double along = target.position().subtract(origin).dot(direction);
        if (along < -2 || along > GraveDragonMythic.RIFT_RANGE + 2) return false;
        Vec3 point = origin.add(0, .01, 0);
        double until = Math.min(GraveDragonMythic.RIFT_RANGE, Math.max(0, along) + 3);
        double fall = 0;
        double distance = 0;
        while (distance <= until && distance < GraveDragonMythic.RIFT_RANGE) {
            double travel = Math.min(GraveDragonMythic.RIFT_SPEED / 20, GraveDragonMythic.RIFT_RANGE - distance);
            int steps = Math.max(1, (int) Math.ceil(travel / .25));
            fall = (fall - .08) * .98;
            Vec3 step = direction.scale(travel / steps).add(0, fall / steps, 0);
            boolean grounded = false;
            for (int i = 0; i < steps; i++) {
                Step next = nextPosition(dragon.level(), dragon, point, step);
                if (next == null) return false;
                if (bounds(point).minmax(bounds(next.position())).intersects(target.getBoundingBox())) return true;
                point = next.position();
                grounded = next.grounded();
                distance += travel / steps;
            }
            if (grounded) fall = 0;
        }
        return false;
    }

    private void finish() {
        ((ServerLevel) level()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                getX(), getY() + 2, getZ(), 96, 2, 2, 2, .3);
        level().playSound(null, getX(), getY() + 2, getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 5F, .7F);
        discard();
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Travel", travel());
        tag.putFloat("Damage", damage);
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(TRAVEL, tag.getFloat("Travel"));
        damage = tag.getFloat("Damage");
    }
}
