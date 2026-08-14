package icu.icuqalt10.panlingre.entity;

import icu.icuqalt10.panlingre.init.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

/** Server-side ten-second controller and client-side holder for the flattened item. */
public class Ys3JinTornadoEntity extends Entity {
    private static final int DURATION = 200;
    private static final EntityDataAccessor<ItemStack> STACK =
            SynchedEntityData.defineId(Ys3JinTornadoEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> OWNER =
            SynchedEntityData.defineId(Ys3JinTornadoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE =
            SynchedEntityData.defineId(Ys3JinTornadoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> START_X = floatData(), START_Y = floatData(), START_Z = floatData();
    private static final EntityDataAccessor<Float> TARGET_X = floatData(), TARGET_Y = floatData(), TARGET_Z = floatData();
    private int age;
    private static final int LAND_AND_FLATTEN_TICKS = 5;
    private static final int EXPAND_TICKS = 10;
    private static final int ANIMATION_TICKS = LAND_AND_FLATTEN_TICKS + EXPAND_TICKS;

    private static EntityDataAccessor<Float> floatData() {
        return SynchedEntityData.defineId(Ys3JinTornadoEntity.class, EntityDataSerializers.FLOAT);
    }

    public Ys3JinTornadoEntity(EntityType<? extends Ys3JinTornadoEntity> type, Level level) { super(type, level); }

    public Ys3JinTornadoEntity(Level level, LivingEntity owner, Vec3 center, ItemStack stack, float damage) {
        this(ModEntities.YS3_JIN_TORNADO.get(), level);
        entityData.set(START_X, (float) owner.getX());
        entityData.set(START_Y, (float) (owner.getY() + 0.5D));
        entityData.set(START_Z, (float) owner.getZ());
        entityData.set(TARGET_X, (float) center.x);
        entityData.set(TARGET_Y, (float) center.y);
        entityData.set(TARGET_Z, (float) center.z);
        setPos(entityData.get(START_X), entityData.get(START_Y), entityData.get(START_Z));
        entityData.set(OWNER, owner.getId());
        entityData.set(STACK, stack.copyWithCount(1));
        entityData.set(DAMAGE, damage);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STACK, ItemStack.EMPTY).define(OWNER, 0).define(DAMAGE, 0.0F);
        builder.define(START_X, 0.0F).define(START_Y, 0.0F).define(START_Z, 0.0F);
        builder.define(TARGET_X, 0.0F).define(TARGET_Y, 0.0F).define(TARGET_Z, 0.0F);
    }

    @Override public void tick() {
        super.tick();
        age++;
        if (age <= LAND_AND_FLATTEN_TICKS) {
            float progress = age / (float) LAND_AND_FLATTEN_TICKS;
            setPos(Mth.lerp(progress, entityData.get(START_X), entityData.get(TARGET_X)),
                    Mth.lerp(progress, entityData.get(START_Y), entityData.get(TARGET_Y)),
                    Mth.lerp(progress, entityData.get(START_Z), entityData.get(TARGET_Z)));
            return;
        }
        setPos(entityData.get(TARGET_X), entityData.get(TARGET_Y), entityData.get(TARGET_Z));
        if (age <= ANIMATION_TICKS) return;
        if (level().isClientSide) return;
        int activeAge = age - ANIMATION_TICKS;
        if (activeAge > DURATION) { discard(); return; }
        if (activeAge % 10 != 0) return;
        Entity owner = level().getEntity(entityData.get(OWNER));
        if (!(owner instanceof LivingEntity livingOwner) || !owner.isAlive()) return;
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                new AABB(position(), position()).inflate(32.0D),
                target -> JinLiRenEntity.isValidAttackTarget(livingOwner, target))) {
            Vec3 start = position().add(0.0D, 3.0D, 0.0D);
            JinLiRenEntity blade = JinLiRenEntity.createCurved(
                    level(), livingOwner, target, entityData.get(DAMAGE), start);
            if (blade != null) level().addFreshEntity(blade);
        }
    }

    public ItemStack getItem() { return entityData.get(STACK); }
    public float getFlattenProgress(float partialTick) {
        return Math.min(1.0F, (age + partialTick) / LAND_AND_FLATTEN_TICKS);
    }
    public float getExpandProgress(float partialTick) {
        return Mth.clamp((age + partialTick - LAND_AND_FLATTEN_TICKS) / EXPAND_TICKS, 0.0F, 1.0F);
    }
    public boolean isTornadoVisible() { return age > LAND_AND_FLATTEN_TICKS; }
    @Override protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) { }
}
