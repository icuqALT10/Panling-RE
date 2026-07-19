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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public class ZhuRiArrowEntity extends Entity {

    // cubic Bezier: P0→P1→P2→P3
    private static final EntityDataAccessor<Optional<UUID>> OWN =
            SynchedEntityData.defineId(ZhuRiArrowEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> X0=F("X0"),Y0=F("Y0"),Z0=F("Z0");
    private static final EntityDataAccessor<Float> X1=F("X1"),Y1=F("Y1"),Z1=F("Z1");
    private static final EntityDataAccessor<Float> X2=F("X2"),Y2=F("Y2"),Z2=F("Z2");
    private static final EntityDataAccessor<Float> X3=F("X3"),Y3=F("Y3"),Z3=F("Z3");
    private static final EntityDataAccessor<Float> PROG=F("PROG"), DEC=F("DEC"), DMG=F("DMG");
    private static EntityDataAccessor<Float> F(String s) {
        return SynchedEntityData.defineId(ZhuRiArrowEntity.class, EntityDataSerializers.FLOAT);
    }

    private enum Stage { FLYING, DECAYING, DONE }
    private Stage stage = Stage.FLYING;
    private int ticks;
    private LivingEntity cachedTarget;

    public ZhuRiArrowEntity(EntityType<? extends ZhuRiArrowEntity> t, Level l) { super(t, l); }

    public ZhuRiArrowEntity(Level level, Player owner, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                            double damage, LivingEntity targetEntity) {
        super(ModEntities.ZHU_RI_ARROW.get(), level);
        entityData.set(OWN, Optional.of(owner.getUUID()));
        s(X0,Y0,Z0,p0); s(X1,Y1,Z1,p1); s(X2,Y2,Z2,p2); s(X3,Y3,Z3,p3);
        entityData.set(DMG, (float)damage);
        entityData.set(PROG, 0f); entityData.set(DEC, 0f);
        this.cachedTarget = targetEntity;
        setPos(p0.x, p0.y, p0.z);
    }

    public Vec3 p0() { return v(X0,Y0,Z0); }  public Vec3 p1() { return v(X1,Y1,Z1); }
    public Vec3 p2() { return v(X2,Y2,Z2); }  public Vec3 p3() { return v(X3,Y3,Z3); }
    public float progress() { return entityData.get(PROG); }
    public float decay()    { return entityData.get(DEC); }
    public boolean decaying() { return stage == Stage.DECAYING; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(OWN, Optional.empty());
        b.define(X0,0f);b.define(Y0,0f);b.define(Z0,0f);
        b.define(X1,0f);b.define(Y1,0f);b.define(Z1,0f);
        b.define(X2,0f);b.define(Y2,0f);b.define(Z2,0f);
        b.define(X3,0f);b.define(Y3,0f);b.define(Z3,0f);
        b.define(PROG,0f); b.define(DEC,0f); b.define(DMG,0f);
    }

    @Override
    public void tick() {
        if (stage == Stage.DONE) { discard(); return; }
        ticks++;
        if (stage == Stage.FLYING) {
            float t = Math.min(1f, ticks / 10f);
            entityData.set(PROG, t);
            Vec3 p = cubicBezier(t, p0(), p1(), p2(), p3());
            setPos(p.x, p.y, p.z);

            if (!level().isClientSide) {
                if (!level().getBlockState(blockPosition()).getCollisionShape(level(), blockPosition()).isEmpty()) {
                    hit(); return;
                }
                if (cachedTarget != null && cachedTarget.isAlive() && t > 0.3f
                        && position().distanceTo(cachedTarget.getEyePosition()) < 2.5) {
                    hit(); return;
                }
                if (t >= 1f) hit();
            }
        } else if (stage == Stage.DECAYING) {
            float d = Math.min(1f, ticks / 10f);
            entityData.set(DEC, d);
            if (!level().isClientSide && d >= 1f) { stage = Stage.DONE; discard(); }
        }
    }

    private void hit() {
        if (stage != Stage.FLYING) return;
        if (!level().isClientSide) {
            float dmg = entityData.get(DMG);
            Entity owner = entityData.get(OWN)
                    .flatMap(uuid -> java.util.Optional.ofNullable(level().getPlayerByUUID(uuid)))
                    .orElse(null);
            if (cachedTarget != null && cachedTarget.isAlive()
                    && position().distanceTo(cachedTarget.getEyePosition()) < 3.5) {
                cachedTarget.hurt(damageSources().thrown(this, owner), dmg);
                cachedTarget.invulnerableTime = 10;
            } else {
                AABB area = new AABB(p3(), p3()).inflate(2.5);
                for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, area,
                        en -> en != owner && en.isAttackable())) {
                    e.hurt(damageSources().thrown(this, owner), dmg);
                    e.invulnerableTime = 10;
                }
            }
        }
        stage = Stage.DECAYING; ticks = 0;
    }

    @Override protected void readAdditionalSaveData(CompoundTag t) {}
    @Override protected void addAdditionalSaveData(CompoundTag t) {}

    public static Vec3 cubicBezier(float t, Vec3 A, Vec3 B, Vec3 C, Vec3 D) {
        double mt = 1 - t;
        double mt2 = mt*mt, t2 = t*t;
        return new Vec3(
            mt2*mt*A.x + 3*mt2*t*B.x + 3*mt*t2*C.x + t2*t*D.x,
            mt2*mt*A.y + 3*mt2*t*B.y + 3*mt*t2*C.y + t2*t*D.y,
            mt2*mt*A.z + 3*mt2*t*B.z + 3*mt*t2*C.z + t2*t*D.z);
    }

    public static Vec3 cubicTangent(float t, Vec3 A, Vec3 B, Vec3 C, Vec3 D) {
        double mt = 1 - t;
        return new Vec3(
            3*mt*mt*(B.x-A.x) + 6*mt*t*(C.x-B.x) + 3*t*t*(D.x-C.x),
            3*mt*mt*(B.y-A.y) + 6*mt*t*(C.y-B.y) + 3*t*t*(D.y-C.y),
            3*mt*mt*(B.z-A.z) + 6*mt*t*(C.z-B.z) + 3*t*t*(D.z-C.z));
    }

    private void s(EntityDataAccessor<Float> ax,EntityDataAccessor<Float> ay,EntityDataAccessor<Float> az,Vec3 v) {
        entityData.set(ax,(float)v.x); entityData.set(ay,(float)v.y); entityData.set(az,(float)v.z);
    }
    private Vec3 v(EntityDataAccessor<Float> ax,EntityDataAccessor<Float> ay,EntityDataAccessor<Float> az) {
        return new Vec3(entityData.get(ax), entityData.get(ay), entityData.get(az));
    }
}
