package icu.icuqalt10.panlingre.entity;

import icu.icuqalt10.panlingre.util.SkillHelper;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Shared timing, animation and teammate selection for tier-three elemental domains. */
public abstract class Ys3DomainEntity extends Entity {
    public static final float DEFAULT_DOMAIN_RADIUS = 7.5F;
    public static final int LAND_AND_FLATTEN_TICKS = 5;
    public static final int EXPAND_TICKS = 10;
    public static final int ANIMATION_TICKS = LAND_AND_FLATTEN_TICKS + EXPAND_TICKS;
    private static final int MAX_TEAMMATES = 4;
    private static final EntityDataAccessor<ItemStack> STACK =
            SynchedEntityData.defineId(Ys3DomainEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> OWNER =
            SynchedEntityData.defineId(Ys3DomainEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> EFFECT_VALUE = floatData();
    private static final EntityDataAccessor<Integer> DURATION =
            SynchedEntityData.defineId(Ys3DomainEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> START_X = floatData(), START_Y = floatData(), START_Z = floatData();
    private static final EntityDataAccessor<Float> TARGET_X = floatData(), TARGET_Y = floatData(), TARGET_Z = floatData();
    private int age;

    private static EntityDataAccessor<Float> floatData() {
        return SynchedEntityData.defineId(Ys3DomainEntity.class, EntityDataSerializers.FLOAT);
    }

    protected Ys3DomainEntity(EntityType<? extends Ys3DomainEntity> type, Level level) {
        super(type, level);
    }

    protected Ys3DomainEntity(EntityType<? extends Ys3DomainEntity> type, Level level,
                              LivingEntity owner, Vec3 center, ItemStack stack,
                              float effectValue, int duration) {
        this(type, level);
        entityData.set(OWNER, owner.getId());
        entityData.set(STACK, stack.copyWithCount(1));
        entityData.set(EFFECT_VALUE, effectValue);
        entityData.set(DURATION, duration);
        entityData.set(START_X, (float) owner.getX());
        entityData.set(START_Y, (float) (owner.getY() + 0.5D));
        entityData.set(START_Z, (float) owner.getZ());
        entityData.set(TARGET_X, (float) center.x);
        entityData.set(TARGET_Y, (float) center.y);
        entityData.set(TARGET_Z, (float) center.z);
        setPos(owner.getX(), owner.getY() + 0.5D, owner.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STACK, ItemStack.EMPTY).define(OWNER, 0)
                .define(EFFECT_VALUE, 0.0F).define(DURATION, 0);
        builder.define(START_X, 0.0F).define(START_Y, 0.0F).define(START_Z, 0.0F);
        builder.define(TARGET_X, 0.0F).define(TARGET_Y, 0.0F).define(TARGET_Z, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        age++;
        if (age <= LAND_AND_FLATTEN_TICKS) {
            float progress = age / (float) LAND_AND_FLATTEN_TICKS;
            setPos(Mth.lerp(progress, entityData.get(START_X), entityData.get(TARGET_X)),
                    Mth.lerp(progress, entityData.get(START_Y), entityData.get(TARGET_Y)),
                    Mth.lerp(progress, entityData.get(START_Z), entityData.get(TARGET_Z)));
        } else {
            setPos(entityData.get(TARGET_X), entityData.get(TARGET_Y), entityData.get(TARGET_Z));
        }

        int activeAge = age - ANIMATION_TICKS;
        if (activeAge <= 0) return;
        if (level().isClientSide) {
            spawnClientParticles(activeAge);
            return;
        }
        if (activeAge > entityData.get(DURATION)) {
            onDomainEnd();
            discard();
        } else if (activeAge >= effectTickOffset()
                && (activeAge - effectTickOffset()) % 20 == 0) {
            applyToDomainTargets();
        }
    }

    private void applyToDomainTargets() {
        beforeApplyingEffects();
        Entity ownerEntity = level().getEntity(entityData.get(OWNER));
        if (!(ownerEntity instanceof LivingEntity owner) || !owner.isAlive()) {
            afterApplyingEffects();
            return;
        }

        float radius = getDomainRadius();
        List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class,
                        new AABB(position(), position()).inflate(radius, 4.0D, radius),
                        SkillHelper.friendlyTargetFilter(owner))
                .stream()
                .filter(target -> horizontalDistanceSqr(target) <= radius * radius)
                .sorted(SkillHelper.friendlyTargetComparator(owner))
                .limit(MAX_TEAMMATES + 1L)
                .toList();
        for (LivingEntity target : targets) applyEffect(target, entityData.get(EFFECT_VALUE));
        afterApplyingEffects();
    }

    private double horizontalDistanceSqr(LivingEntity target) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        return dx * dx + dz * dz;
    }

    protected abstract void applyEffect(LivingEntity target, float effectValue);
    protected abstract void spawnClientParticles(int activeAge);
    protected int effectTickOffset() { return 20; }
    protected void beforeApplyingEffects() { }
    protected void afterApplyingEffects() { }
    protected void onDomainEnd() { }

    public ItemStack getItem() { return entityData.get(STACK); }
    public float getDomainRadius() { return DEFAULT_DOMAIN_RADIUS; }
    public float getItemScale() { return 15.0F; }
    public boolean isActive() { return age > ANIMATION_TICKS; }
    public float flattenProgress(float partialTick) {
        return Math.min(1.0F, (age + partialTick) / LAND_AND_FLATTEN_TICKS);
    }
    public float expandProgress(float partialTick) {
        return Mth.clamp((age + partialTick - LAND_AND_FLATTEN_TICKS) / EXPAND_TICKS, 0.0F, 1.0F);
    }

    @Override protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) { }
}
