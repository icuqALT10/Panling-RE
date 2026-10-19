package icu.icuqalt10.panlingre.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** One persistent display per moving assembly; barriers provide collision. */
public final class TombDisplayEntity extends Entity {
    public static final int BRONZE_LEFT = 3, BRONZE_RIGHT = 4, COFFIN = 5, SEAL = 6;
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(TombDisplayEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> FROM = SynchedEntityData.defineId(TombDisplayEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET = SynchedEntityData.defineId(TombDisplayEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> START = SynchedEntityData.defineId(TombDisplayEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(TombDisplayEntity.class, EntityDataSerializers.INT);

    public TombDisplayEntity(EntityType<? extends TombDisplayEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(KIND, 0); b.define(FROM, 0F); b.define(TARGET, 0F); b.define(START, 0L); b.define(DURATION, 0);
    }

    public int kind() { return entityData.get(KIND); }
    public void setKind(int kind) { entityData.set(KIND, kind); }
    public float target() { return entityData.get(TARGET); }
    public float progress(float partialTick) {
        int duration = entityData.get(DURATION);
        float t = duration == 0 ? 1 : Mth.clamp((level().getGameTime() - entityData.get(START) + partialTick) / duration, 0, 1);
        // Portcullises move linearly at two blocks/second. Hinged doors ease in/out.
        if (kind()==BRONZE_LEFT || kind()==BRONZE_RIGHT) t = t * t * (3 - 2 * t);
        return Mth.lerp(t, entityData.get(FROM), target());
    }

    public void animateTo(float target, int fullDuration) {
        if (target() == target) return;
        float from = progress(0);
        entityData.set(FROM, from); entityData.set(TARGET, target);
        entityData.set(START, level().getGameTime());
        entityData.set(DURATION, Math.round(Math.abs(target - from) * fullDuration));
    }

    public void reset() {
        entityData.set(FROM, 0F); entityData.set(TARGET, 0F); entityData.set(DURATION, 0);
    }

    public float sealAlpha(float partialTick) {
        if (target()!=0 || entityData.get(DURATION)==0) return progress(partialTick);
        float age=level().getGameTime()-entityData.get(START)+partialTick;
        return age>=40 ? 0 : ((int)(age/2)%2==0 ? 1 : .5F);
    }

    /** Original inward opening, hinged on the rear edge of the Z=3808 block
     * (world boundary 3809). Geometry is relative to the assembly at Z=3806. */
    public static org.joml.Matrix4f bronzeTransform(int kind,float progress) {
        return new org.joml.Matrix4f().translate(0,0,3)
                .rotateY((kind==BRONZE_LEFT ? -1 : 1)*(float)(Math.PI/2)*progress).translate(0,0,-3);
    }

    /** Right of the entrance view is west (-X). Lose support after half-width,
     * then rotate about the coffin rim; the outer lip lands on the Y=79 step. */
    public static org.joml.Matrix4f coffinTransform(float progress) {
        float slide=8*Mth.clamp(progress,0,1);
        // The nine-block lid loses support at 4.5 blocks, not at a timer fraction.
        float fallingSeconds=Math.max(0,(slide-4.5F)/2);
        float drop=Math.min(4,.5F*9.8F*fallingSeconds*fallingSeconds);
        float angle=(float)Math.asin(drop/(1+slide));
        return new org.joml.Matrix4f().translate(1,0,0).rotateZ(angle).translate(-1-slide,0,0);
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        setKind(tag.getInt("Kind"));
        entityData.set(FROM, tag.getFloat("From")); entityData.set(TARGET, tag.getFloat("Target"));
        entityData.set(START, tag.getLong("Start")); entityData.set(DURATION, tag.getInt("Duration"));
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Kind", kind()); tag.putFloat("From", entityData.get(FROM)); tag.putFloat("Target", target());
        tag.putLong("Start", entityData.get(START)); tag.putInt("Duration", entityData.get(DURATION));
    }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 256 * 256; }
    @Override public AABB getBoundingBoxForCulling() { return getBoundingBox().inflate(48); }
}
