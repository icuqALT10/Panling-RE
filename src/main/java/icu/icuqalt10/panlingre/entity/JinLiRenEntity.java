package icu.icuqalt10.panlingre.entity;

import icu.icuqalt10.panlingre.init.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/** A pickup-less, gravity-free arrow used by the refined metal skill. */
public class JinLiRenEntity extends AbstractArrow {
    private static final int MAX_LIFETIME_TICKS = 1200;
    private static final EntityDataAccessor<Boolean> CURVED =
            SynchedEntityData.defineId(JinLiRenEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> X0 = floatData(), Y0 = floatData(), Z0 = floatData();
    private static final EntityDataAccessor<Float> X1 = floatData(), Y1 = floatData(), Z1 = floatData();
    private static final EntityDataAccessor<Float> X2 = floatData(), Y2 = floatData(), Z2 = floatData();
    private static final EntityDataAccessor<Float> X3 = floatData(), Y3 = floatData(), Z3 = floatData();
    private static final EntityDataAccessor<Float> PROGRESS = floatData();
    private static final EntityDataAccessor<Float> DECAY = floatData();
    private static final EntityDataAccessor<Integer> CURVE_DURATION =
            SynchedEntityData.defineId(JinLiRenEntity.class, EntityDataSerializers.INT);

    private double skillDamage;
    private final Set<Integer> hitEntityIds = new HashSet<>();
    private int curveTargetId = -1;
    private int curveAge;
    private int decayAge;
    private boolean decaying;
    private float previousProgress;
    private float previousDecay;

    private static EntityDataAccessor<Float> floatData() {
        return SynchedEntityData.defineId(JinLiRenEntity.class, EntityDataSerializers.FLOAT);
    }

    public JinLiRenEntity(EntityType<? extends JinLiRenEntity> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
    }

    public JinLiRenEntity(Level level, LivingEntity owner, double damage) {
        super(ModEntities.JIN_LI_REN.get(), owner, level, ItemStack.EMPTY, null);
        this.skillDamage = Math.max(0.0D, damage);
        this.pickup = Pickup.DISALLOWED;
    }

    public JinLiRenEntity(Level level, LivingEntity owner, double damage,
                          LivingEntity target, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3) {
        this(level, owner, damage);
        this.curveTargetId = target.getId();
        entityData.set(CURVED, true);
        setPoint(X0, Y0, Z0, p0); setPoint(X1, Y1, Z1, p1);
        setPoint(X2, Y2, Z2, p2); setPoint(X3, Y3, Z3, p3);
        entityData.set(PROGRESS, 0.0F);
        entityData.set(DECAY, 0.0F);
        entityData.set(CURVE_DURATION,
                Math.clamp((int) Math.ceil(p0.distanceTo(p3) / 3.0D), 3, 10));
        setPos(p0);
    }

    /** Uses the exact control-point construction used by Ys2HealingSkill. */
    public static JinLiRenEntity createCurved(Level level, LivingEntity owner,
                                               LivingEntity target, double damage, Vec3 p0) {
        Vec3 p3 = target.getEyePosition().add(0.0D, -0.2D, 0.0D);
        Vec3 towardTarget = p3.subtract(p0);
        double distance = towardTarget.length();
        if (distance < 0.01D) return null;

        Vec3 forward = towardTarget.normalize();
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 0.01D) right = forward.cross(new Vec3(1.0D, 0.0D, 0.0D));
        right = right.normalize();

        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        Vec3 sideDirection = right.scale(Math.cos(angle))
                .add(forward.cross(right).scale(Math.sin(angle)));
        if (sideDirection.lengthSqr() < 0.01D) sideDirection = right;
        sideDirection = sideDirection.normalize();

        double verticalSign = level.random.nextDouble() < 0.5D ? 1.0D : -1.0D;
        double verticalMagnitude = distance * (0.15D + level.random.nextDouble() * 0.2D);
        double p1Distance = distance * 0.3D;
        Vec3 p1 = p0.add(forward.scale(p1Distance * 0.6D))
                .add(sideDirection.scale(p1Distance * 0.5D))
                .add(0.0D, verticalSign * verticalMagnitude * 0.6D, 0.0D);

        double horizontalOffset2 = (level.random.nextDouble() - 0.5D)
                * Math.min(distance * 0.3D, 5.0D);
        Vec3 p2 = p3.subtract(forward.scale(distance * 0.25D))
                .add(right.scale(horizontalOffset2))
                .add(0.0D, verticalSign * verticalMagnitude * 0.3D, 0.0D);
        return new JinLiRenEntity(level, owner, damage, target, p0, p1, p2, p3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CURVED, false);
        builder.define(X0, 0.0F); builder.define(Y0, 0.0F); builder.define(Z0, 0.0F);
        builder.define(X1, 0.0F); builder.define(Y1, 0.0F); builder.define(Z1, 0.0F);
        builder.define(X2, 0.0F); builder.define(Y2, 0.0F); builder.define(Z2, 0.0F);
        builder.define(X3, 0.0F); builder.define(Y3, 0.0F); builder.define(Z3, 0.0F);
        builder.define(PROGRESS, 0.0F);
        builder.define(DECAY, 0.0F);
        builder.define(CURVE_DURATION, 10);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0D;
    }

    @Override
    public void tick() {
        if (entityData.get(CURVED)) {
            previousProgress = entityData.get(PROGRESS);
            previousDecay = entityData.get(DECAY);
            this.xOld = getX();
            this.yOld = getY();
            this.zOld = getZ();
            this.yRotO = getYRot();
            this.xRotO = getXRot();
            if (decaying || entityData.get(DECAY) > 0.0F) {
                decaying = true;
                float decay = Math.min(1.0F, ++decayAge / 10.0F);
                entityData.set(DECAY, decay);
                setPos(p3());
                if (!level().isClientSide && decay >= 1.0F) discard();
                return;
            }
            // Advance on both logical sides, exactly like ZhuRiArrowEntity.
            float t = Math.min(1.0F, ++curveAge / (float) entityData.get(CURVE_DURATION));
            entityData.set(PROGRESS, t);
            setPos(cubicBezier(t, p0(), p1(), p2(), p3()));
            Vec3 tangent = cubicTangent(t, p0(), p1(), p2(), p3());
            if (tangent.lengthSqr() > 1.0E-7D) {
                Vec3 direction = tangent.normalize();
                setYRot((float) Math.toDegrees(Math.atan2(direction.z, direction.x)));
                setXRot((float) Math.toDegrees(Math.asin(direction.y)));
            }
            if (t >= 1.0F) {
                if (!this.level().isClientSide) {
                    Entity owner = getOwner();
                    Entity target = level().getEntity(curveTargetId);
                    if (owner != null && isValidAttackTarget(owner, target)) {
                        if (skillDamage > 0.0D) {
                            target.hurt(damageSources().arrow(this, owner), (float) skillDamage);
                            if (owner instanceof LivingEntity livingOwner && target instanceof LivingEntity livingTarget) {
                                livingOwner.setLastHurtMob(livingTarget);
                            }
                        }
                    }
                }
                decaying = true;
                decayAge = 0;
                return;
            }
            return;
        } else {
            super.tick();
        }
        // A gravity-free arrow may never reach a block, so also enforce the
        // vanilla arrow despawn duration while it is still in flight.
        if (!this.level().isClientSide && this.tickCount >= MAX_LIFETIME_TICKS) {
            this.discard();
        }
    }

    public static Vec3 cubicBezier(float t, Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
        double mt = 1.0 - t;
        return a.scale(mt * mt * mt).add(b.scale(3.0 * mt * mt * t))
                .add(c.scale(3.0 * mt * t * t)).add(d.scale(t * t * t));
    }

    public static Vec3 cubicTangent(float t, Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
        double mt = 1.0 - t;
        return b.subtract(a).scale(3.0 * mt * mt)
                .add(c.subtract(b).scale(6.0 * mt * t))
                .add(d.subtract(c).scale(3.0 * t * t));
    }

    public boolean isCurved() { return entityData.get(CURVED); }
    public float progress() { return entityData.get(PROGRESS); }
    public float progress(float partialTick) {
        return previousProgress + (progress() - previousProgress) * partialTick;
    }
    public float decay() { return entityData.get(DECAY); }
    public float decay(float partialTick) {
        return previousDecay + (decay() - previousDecay) * partialTick;
    }
    public boolean decaying() { return decaying || decay() > 0.0F; }
    public Vec3 p0() { return point(X0, Y0, Z0); }
    public Vec3 p1() { return point(X1, Y1, Z1); }
    public Vec3 p2() { return point(X2, Y2, Z2); }
    public Vec3 p3() { return point(X3, Y3, Z3); }

    private void setPoint(EntityDataAccessor<Float> x, EntityDataAccessor<Float> y,
                          EntityDataAccessor<Float> z, Vec3 point) {
        entityData.set(x, (float) point.x); entityData.set(y, (float) point.y); entityData.set(z, (float) point.z);
    }

    private Vec3 point(EntityDataAccessor<Float> x, EntityDataAccessor<Float> y,
                       EntityDataAccessor<Float> z) {
        return new Vec3(entityData.get(x), entityData.get(y), entityData.get(z));
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        this.hitEntityIds.add(target.getId());

        if (!this.level().isClientSide) {
            Entity owner = this.getOwner();
            target.hurt(this.damageSources().arrow(this, owner != null ? owner : this), (float)this.skillDamage);
            if (owner instanceof LivingEntity livingOwner) {
                livingOwner.setLastHurtMob(target);
            }

            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (!super.canHitEntity(entity) || this.hitEntityIds.contains(entity.getId())) {
            return false;
        }

        Entity owner = this.getOwner();
        return isValidAttackTarget(owner, entity);
    }

    /** The target predicate shared by the blade collision and auto-targeting skills. */
    public static boolean isValidAttackTarget(Entity owner, Entity entity) {
        if (!(entity instanceof LivingEntity livingTarget)
                || !livingTarget.isAlive()
                || !livingTarget.isAttackable()
                || livingTarget.isInvulnerable()
                || entity == owner) {
            return false;
        }
        if (livingTarget instanceof Player targetPlayer
                && (targetPlayer.isCreative() || targetPlayer.isSpectator())) {
            return false;
        }
        return owner == null || owner.getTeam() == null
                || !owner.getTeam().isAlliedTo(livingTarget.getTeam());
    }

    @Override
    public void setOwner(Entity owner) {
        super.setOwner(owner);
        this.pickup = Pickup.DISALLOWED;
    }

    @Override
    public void playerTouch(Player player) {
        // This projectile deliberately has no collectible item form.
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("SkillDamage", this.skillDamage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.skillDamage = Math.max(0.0D, tag.getDouble("SkillDamage"));
        this.pickup = Pickup.DISALLOWED;
    }
}
