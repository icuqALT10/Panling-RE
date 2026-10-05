package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

import icu.icuqalt10.panlingre.init.ModEntities;
import icu.icuqalt10.panlingre.network.particle.HuoQiuExplosionParticles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Large, straight-flying dragon fireball. Its trail is emitted locally by each client. */
public final class GraveDragonFireballEntity extends ThrowableItemProjectile {
    static final double SPEED = 3.75;
    private static final double EXPLOSION_RADIUS = 12;
    private static final EntityDataAccessor<Boolean> BLUE = SynchedEntityData.defineId(GraveDragonFireballEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(GraveDragonFireballEntity.class, EntityDataSerializers.FLOAT);

    public GraveDragonFireballEntity(EntityType<? extends GraveDragonFireballEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public GraveDragonFireballEntity(Level level, GraveDragonEntity owner, Vec3 direction, float damage, boolean blue) {
        this(ModEntities.GRAVE_DRAGON_FIREBALL.get(), level);
        setOwner(owner);
        entityData.set(BLUE, blue);
        entityData.set(DAMAGE, damage);
        setDeltaMovement(direction.normalize().scale(SPEED));
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BLUE, false);
        builder.define(DAMAGE, 15F);
    }

    public boolean blue() { return entityData.get(BLUE); }

    static Vec3 firingDirection(GraveDragonEntity dragon, String action, int shot, float yaw,
                                Vec3 origin, Vec3 mouth) {
        double angle = Math.toRadians(yaw);
        Vec3 front = new Vec3(-Math.sin(angle), 0, Math.cos(angle));
        Vec3 right = new Vec3(-front.z, 0, front.x);
        boolean ground = action.equals("ground_firezone");
        double radians = Math.toRadians((ground ? new int[]{90, 45, 0, -45, -90} : new int[]{90, 0, -90})[shot]);
        Vec3 sector = front.scale(Math.cos(radians)).add(right.scale(Math.sin(radians)));
        double minDot = Math.cos(Math.toRadians(ground ? 22.5 : 45));
        ServerPlayer nearest = null;
        double best = Double.MAX_VALUE;
        if (dragon.level() instanceof ServerLevel server) for (ServerPlayer player : server.players()) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator()
                    || player.position().distanceToSqr(origin) > 10000) continue;
            Vec3 offset = player.position().subtract(origin).multiply(1, 0, 1);
            if (offset.lengthSqr() < .01 || offset.normalize().dot(sector) < minDot) continue;
            if (offset.lengthSqr() < best) { best = offset.lengthSqr(); nearest = player; }
        }
        Vec3 destination = origin.add(sector.scale(40));
        if (nearest != null) destination = nearest.getBoundingBox().getCenter();
        else {
            var floor = dragon.level().clip(new ClipContext(destination.add(0, .25, 0),
                    new Vec3(destination.x, dragon.level().getMinBuildHeight(), destination.z),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dragon));
            if (floor.getType() == HitResult.Type.BLOCK) destination = floor.getLocation();
        }
        return destination.subtract(mouth).normalize();
    }

    /** Use the same sector target, launch pose and collision ray as a real shot. */
    static boolean canReach(GraveDragonEntity dragon, String action, ServerPlayer target, float yaw, Vec3 origin) {
        var windows = GraveDragonActions.get(action).windows();
        var transform = GraveDragonPose.modelToEntity(yaw, dragon.bodyPitch(), dragon.getScale());
        for (int shot = 0; shot < windows.size(); shot++) {
            var frame = GraveDragonPose.sample(action, windows.get(shot).from(), false);
            Vec3 mouth = GraveDragonBreath.geometry(frame, transform, origin, action).mouth();
            Vec3 direction = firingDirection(dragon, action, shot, yaw, origin, mouth);
            double distance = Math.max(0, target.position().subtract(mouth).dot(direction)) + EXPLOSION_RADIUS;
            Vec3 end = mouth.add(direction.scale(distance));
            HitResult impact = dragon.level().clip(new ClipContext(mouth, end,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dragon));
            if (impact.getType() != HitResult.Type.MISS) end = impact.getLocation();
            var entityHit = ProjectileUtil.getEntityHitResult(dragon.level(), dragon, mouth, end,
                    new AABB(mouth, end).inflate(4), entity -> entity.canBeHitByProjectile()
                            && !(entity instanceof GraveDragonPartEntity part && part.getParent() == dragon));
            if (entityHit != null) impact = entityHit;
            if (impact.getType() != HitResult.Type.MISS
                    && target.position().distanceToSqr(impact.getLocation()) <= EXPLOSION_RADIUS * EXPLOSION_RADIUS)
                return true;
        }
        return false;
    }

    @Override protected Item getDefaultItem() { return Items.MAGMA_BLOCK; }
    @Override protected double getDefaultGravity() { return 0; }

    @Override protected boolean canHitEntity(Entity target) {
        return !(target instanceof GraveDragonPartEntity part && part.getParent() == getOwner())
                && target != getOwner() && super.canHitEntity(target);
    }

    @Override public void tick() {
        if (getDeltaMovement().lengthSqr() > .001)
            setDeltaMovement(getDeltaMovement().normalize().scale(SPEED));
        super.tick();
        if (level().isClientSide && !isRemoved()) {
            for (int i = 0; i < 48; i++) {
                double x = getX() + (random.nextDouble() - .5) * 5;
                double y = getY() + (random.nextDouble() - .5) * 5;
                double z = getZ() + (random.nextDouble() - .5) * 5;
                level().addParticle(blue() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, x, y, z, 0, .03, 0);
            }
        }
        if (tickCount > 120) discard();
    }

    @Override protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel server) {
            Vec3 center = result.getLocation();
            Entity owner = getOwner();
            for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class,
                    new AABB(center, center).inflate(EXPLOSION_RADIUS), target -> target.isAlive()
                            && !target.isCreative() && !target.isSpectator()
                            && target.position().distanceToSqr(center) <= EXPLOSION_RADIUS * EXPLOSION_RADIUS)) {
                player.igniteForSeconds(5);
                if (player.hurt(damageSources().explosion(this, owner), entityData.get(DAMAGE))) {
                    Vec3 away = player.position().subtract(center).multiply(1, 0, 1).normalize();
                    player.push(away.x * .6, .25, away.z * .6);
                }
            }
            HuoQiuExplosionParticles particles = new HuoQiuExplosionParticles(center, blue(), true);
            for (ServerPlayer player : server.players())
                if (player.distanceToSqr(center) <= 80 * 80) PacketDistributor.sendToPlayer(player, particles);
            server.playSound(null, center.x, center.y, center.z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 5F, .8F);
            discard();
        }
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Blue", blue());
        tag.putFloat("DragonDamage", entityData.get(DAMAGE));
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(BLUE, tag.getBoolean("Blue"));
        entityData.set(DAMAGE, tag.getFloat("DragonDamage"));
    }
}
