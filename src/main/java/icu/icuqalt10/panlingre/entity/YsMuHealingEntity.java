package icu.icuqalt10.panlingre.entity;

import icu.icuqalt10.panlingre.init.ModEntities;
import icu.icuqalt10.panlingre.network.ItemActivationPayload;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.BiConsumer;

public class YsMuHealingEntity extends Entity {
    private static final int FLIGHT_TICKS = 5;
    private static final int DECAY_TICKS = 10;
    private static final EntityDataAccessor<Float> X0 = floatData(), Y0 = floatData(), Z0 = floatData();
    private static final EntityDataAccessor<Float> X1 = floatData(), Y1 = floatData(), Z1 = floatData();
    private static final EntityDataAccessor<Float> X2 = floatData(), Y2 = floatData(), Z2 = floatData();
    private static final EntityDataAccessor<Float> X3 = floatData(), Y3 = floatData(), Z3 = floatData();
    private static final EntityDataAccessor<Float> PROGRESS = floatData();
    private static final EntityDataAccessor<Float> DECAY = floatData();
    private static final EntityDataAccessor<Integer> TRAIL_COLOR =
            SynchedEntityData.defineId(YsMuHealingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> STACK =
            SynchedEntityData.defineId(YsMuHealingEntity.class, EntityDataSerializers.ITEM_STACK);

    private UUID targetId;
    private Holder<SoundEvent> impactSound;
    private float effectValue;
    private BiConsumer<LivingEntity, Float> targetEffect;
    private int flightAge;
    private int decayAge;
    private boolean decaying;

    private static EntityDataAccessor<Float> floatData() {
        return SynchedEntityData.defineId(YsMuHealingEntity.class, EntityDataSerializers.FLOAT);
    }

    public YsMuHealingEntity(EntityType<? extends YsMuHealingEntity> type, Level level) {
        super(type, level);
    }

    public YsMuHealingEntity(Level level, LivingEntity target, ItemStack stack,
                             Holder<SoundEvent> impactSound,
                             int trailColor,
                             float effectValue,
                             BiConsumer<LivingEntity, Float> targetEffect,
                             Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3) {
        super(ModEntities.YS_MU_HEALING.get(), level);
        targetId = target.getUUID();
        this.impactSound = impactSound;
        this.effectValue = effectValue;
        this.targetEffect = targetEffect;
        entityData.set(TRAIL_COLOR, trailColor);
        setPoint(X0, Y0, Z0, p0); setPoint(X1, Y1, Z1, p1);
        setPoint(X2, Y2, Z2, p2); setPoint(X3, Y3, Z3, p3);
        entityData.set(STACK, stack.copyWithCount(1));
        setPos(p0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(X0, 0.0F); builder.define(Y0, 0.0F); builder.define(Z0, 0.0F);
        builder.define(X1, 0.0F); builder.define(Y1, 0.0F); builder.define(Z1, 0.0F);
        builder.define(X2, 0.0F); builder.define(Y2, 0.0F); builder.define(Z2, 0.0F);
        builder.define(X3, 0.0F); builder.define(Y3, 0.0F); builder.define(Z3, 0.0F);
        builder.define(PROGRESS, 0.0F);
        builder.define(DECAY, 0.0F);
        builder.define(TRAIL_COLOR, 0x00AAAA);
        builder.define(STACK, ItemStack.EMPTY);
    }

    @Override
    public void tick() {
        super.tick();
        if (decaying || entityData.get(DECAY) > 0.0F) {
            setPos(p3());
            if (!level().isClientSide) {
                decayAge++;
                entityData.set(DECAY, Math.min(1.0F, decayAge / (float) DECAY_TICKS));
                if (decayAge >= DECAY_TICKS) discard();
            }
            return;
        }

        flightAge++;
        float progress = Math.min(1.0F, flightAge / (float) FLIGHT_TICKS);
        entityData.set(PROGRESS, progress);
        setPos(cubicBezier(progress, p0(), p1(), p2(), p3()));

        if (!level().isClientSide && progress >= 1.0F) {
            LivingEntity target = null;
            if (level() instanceof ServerLevel serverLevel
                    && serverLevel.getEntity(targetId) instanceof LivingEntity livingTarget) {
                target = livingTarget;
            }
            if (target != null && target.isAlive()) {
                if (targetEffect != null) {
                    targetEffect.accept(target, effectValue);
                }
                if (target instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer,
                            new ItemActivationPayload(getItem().copy()));
                    if (impactSound != null) {
                        serverPlayer.playNotifySound(
                                impactSound.value(), SoundSource.PLAYERS, 0.5F, 1.0F);
                    }
                }
            }
            decaying = true;
            decayAge = 0;
        }
    }

    public Vec3 p0() { return point(X0, Y0, Z0); }
    public Vec3 p1() { return point(X1, Y1, Z1); }
    public Vec3 p2() { return point(X2, Y2, Z2); }
    public Vec3 p3() { return point(X3, Y3, Z3); }
    public float progress() { return entityData.get(PROGRESS); }
    public float decay() { return entityData.get(DECAY); }
    public boolean decaying() { return decay() > 0.0F; }
    public int getTrailColor() { return entityData.get(TRAIL_COLOR); }
    public ItemStack getItem() { return entityData.get(STACK); }

    public static Vec3 cubicBezier(float t, Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
        double mt = 1.0 - t, mt2 = mt * mt, t2 = t * t;
        return a.scale(mt2 * mt).add(b.scale(3.0 * mt2 * t))
                .add(c.scale(3.0 * mt * t2)).add(d.scale(t2 * t));
    }

    public static Vec3 cubicTangent(float t, Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
        double mt = 1.0 - t;
        return b.subtract(a).scale(3.0 * mt * mt)
                .add(c.subtract(b).scale(6.0 * mt * t))
                .add(d.subtract(c).scale(3.0 * t * t));
    }

    private void setPoint(EntityDataAccessor<Float> x, EntityDataAccessor<Float> y,
                          EntityDataAccessor<Float> z, Vec3 point) {
        entityData.set(x, (float) point.x); entityData.set(y, (float) point.y); entityData.set(z, (float) point.z);
    }

    private Vec3 point(EntityDataAccessor<Float> x, EntityDataAccessor<Float> y, EntityDataAccessor<Float> z) {
        return new Vec3(entityData.get(x), entityData.get(y), entityData.get(z));
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
}
