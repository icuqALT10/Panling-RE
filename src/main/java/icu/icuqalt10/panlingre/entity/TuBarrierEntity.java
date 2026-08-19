package icu.icuqalt10.panlingre.entity;

import icu.icuqalt10.panlingre.init.ModEntities;
import icu.icuqalt10.panlingre.util.SkillHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

public class TuBarrierEntity extends Entity {
    private static final int ASCENT_TICKS = 3;
    private static final double SEARCH_RANGE = 128.0;

    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(TuBarrierEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<ItemStack> STACK =
            SynchedEntityData.defineId(TuBarrierEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> HEALTH = floatData();
    private static final EntityDataAccessor<Float> MAX_HEALTH = floatData();
    private static final EntityDataAccessor<Float> DIAMETER = floatData();
    private static final EntityDataAccessor<Integer> DURATION =
            SynchedEntityData.defineId(TuBarrierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> START_Y = floatData();
    private static final EntityDataAccessor<Float> BASE_Y = floatData();
    private static final EntityDataAccessor<Float> TARGET_Y = floatData();
    private static final EntityDataAccessor<Boolean> GROUND_STYLE =
            SynchedEntityData.defineId(TuBarrierEntity.class, EntityDataSerializers.BOOLEAN);

    private int age;
    private int activeAge;

    private static EntityDataAccessor<Float> floatData() {
        return SynchedEntityData.defineId(TuBarrierEntity.class, EntityDataSerializers.FLOAT);
    }

    public TuBarrierEntity(EntityType<? extends TuBarrierEntity> type, Level level) {
        super(type, level);
    }

    public TuBarrierEntity(Level level, Player owner, ItemStack stack,
                           float barrierHealth, int durationTicks, float diameter) {
        this(level, owner, stack, barrierHealth, durationTicks, diameter, false);
    }

    public TuBarrierEntity(Level level, Player owner, ItemStack stack,
                           float barrierHealth, int durationTicks, float diameter,
                           boolean groundStyle) {
        super(ModEntities.TU_BARRIER.get(), level);
        entityData.set(OWNER, Optional.of(owner.getUUID()));
        entityData.set(STACK, stack.copyWithCount(1));
        entityData.set(HEALTH, barrierHealth);
        entityData.set(MAX_HEALTH, barrierHealth);
        entityData.set(DURATION, durationTicks);
        entityData.set(DIAMETER, diameter);
        entityData.set(GROUND_STYLE, groundStyle);
        double baseY = findGroundY(level, owner);
        entityData.set(START_Y, (float) (owner.getY() + 0.5));
        entityData.set(BASE_Y, (float) baseY);
        entityData.set(TARGET_Y, (float) (baseY + (groundStyle ? 0.0D : 5.0D)));
        setPos(owner.getX(), owner.getY() + 0.5, owner.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, Optional.empty());
        builder.define(STACK, ItemStack.EMPTY);
        builder.define(HEALTH, 0.0F);
        builder.define(MAX_HEALTH, 0.0F);
        builder.define(DIAMETER, 0.0F);
        builder.define(DURATION, 0);
        builder.define(START_Y, 0.0F);
        builder.define(BASE_Y, 0.0F);
        builder.define(TARGET_Y, 0.0F);
        builder.define(GROUND_STYLE, false);
    }

    @Override
    public void tick() {
        super.tick();
        age++;
        int movementTicks = isGroundStyle() ? 5 : ASCENT_TICKS;
        if (age <= movementTicks) {
            float progress = age / (float) movementTicks;
            setPos(getX(), Mth.lerp(progress, entityData.get(START_Y), entityData.get(TARGET_Y)), getZ());
            return;
        }

        setPos(getX(), entityData.get(TARGET_Y), getZ());
        if (age <= getWarmupTicks()) return;
        if (!level().isClientSide) {
            activeAge++;
            if (activeAge > getDurationTicks() || getBarrierHealth() <= 0.0F) {
                discard();
            }
        }
    }

    public boolean protects(LivingEntity target) {
        if (!isAlive() || !isActive() || target.level() != level() || !target.isAlive()) return false;
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double radius = getDiameter() * 0.5;
        if (dx * dx + dz * dz > radius * radius) return false;

        Player owner = getOwner();
        return owner != null
                && SkillHelper.friendlyTargetFilter(owner).test(target);
    }

    public void absorbDamage(float amount) {
        if (amount <= 0.0F || level().isClientSide) return;
        float remainingHealth = Math.max(0.0F, getBarrierHealth() - amount);
        entityData.set(HEALTH, remainingHealth);
        if (getBarrierHealth() <= 0.0F) discard();
    }

    public static TuBarrierEntity findProtecting(ServerLevel level, LivingEntity target) {
        AABB search = target.getBoundingBox().inflate(SEARCH_RANGE);
        return level.getEntitiesOfClass(TuBarrierEntity.class, search, barrier -> barrier.protects(target))
                .stream()
                .min(Comparator.comparingDouble(target::distanceToSqr))
                .orElse(null);
    }

    public Player getOwner() {
        return entityData.get(OWNER).map(level()::getPlayerByUUID).orElse(null);
    }

    public ItemStack getItem() { return entityData.get(STACK); }
    public float getBarrierHealth() { return entityData.get(HEALTH); }
    public float getMaxBarrierHealth() { return entityData.get(MAX_HEALTH); }
    public float getBarrierHealthPercentage() {
        float maximum = getMaxBarrierHealth();
        return maximum <= 0.0F ? 0.0F : Mth.clamp(getBarrierHealth() / maximum, 0.0F, 1.0F);
    }
    public float getDiameter() { return entityData.get(DIAMETER); }
    public double getBarrierBaseY() { return entityData.get(BASE_Y); }
    public boolean isActive() { return age > getWarmupTicks(); }
    public boolean isGroundStyle() { return entityData.get(GROUND_STYLE); }
    public int getWarmupTicks() { return isGroundStyle() ? 15 : ASCENT_TICKS; }
    public float getFlattenProgress(float partialTick) {
        return Mth.clamp((age + partialTick) / 5.0F, 0.0F, 1.0F);
    }
    public float getExpandProgress(float partialTick) {
        return Mth.clamp((age + partialTick - 5.0F) / 10.0F, 0.0F, 1.0F);
    }
    public int getDurationTicks() { return entityData.get(DURATION); }

    private static double findGroundY(Level level, Player owner) {
        int x = Mth.floor(owner.getX());
        int z = Mth.floor(owner.getZ());
        int startY = Math.min(Mth.floor(owner.getY()), level.getMaxBuildHeight() - 1);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, startY, z);
        for (int y = startY; y >= level.getMinBuildHeight(); y--) {
            cursor.setY(y);
            VoxelShape collision = level.getBlockState(cursor).getCollisionShape(level, cursor);
            if (!collision.isEmpty()) {
                return y + collision.max(Direction.Axis.Y);
            }
        }
        return owner.getY() - 5.0;
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
}
