package icu.icuqalt10.panlingre.entity;

import icu.icuqalt10.panlingre.client.FireTrailRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class FireTornadoEntity extends Mob {

    private Vec3 targetPos = null;          // 目标位置
    private int lifespan = 40;              // 总寿命（tick）
    private int age = 0;                    // 已存活 tick
    private boolean initialized = false;    // 是否已设置参数

    public FireTornadoEntity(EntityType<FireTornadoEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setInvulnerable(true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    // ---------- 外部设置参数（供指令调用） ----------
    public void setMovementParameters(Vec3 target, int lifespanTicks) {
        this.targetPos = target;
        this.lifespan = Math.max(1, lifespanTicks);
        this.age = 0;
        this.initialized = true;
    }

    // ---------- 核心 tick ----------
    @Override
    public void tick() {
        super.tick();

        // 记录火焰轨迹（服务端和客户端都执行）
        double x = position().x;
        double y = position().y - 0.5;
        double z = position().z;
        List<BlockPos> belowPoses = List.of(
                BlockPos.containing(x, y, z),
                BlockPos.containing(x + 1, y, z),
                BlockPos.containing(x, y, z + 1),
                BlockPos.containing(x - 1, y, z),
                BlockPos.containing(x, y, z - 1));

        for (BlockPos pos : belowPoses) {
            BlockState state = level().getBlockState(pos);
            if (!state.isAir() && state.isSolidRender(level(), pos)) {
                // 记录到轨迹跟踪器（服务端用于逻辑检测，客户端用于渲染）
                FireTrailTracker.addTrail(pos, state, 60);

                // 客户端：同时添加到渲染器
                if (level().isClientSide) {
                    FireTrailRenderer.addTrailBlock(pos, state, 60);
                }
            }
        }

        // 客户端直接返回，不执行后续逻辑
        if (level().isClientSide) {
            return;
        }

        // 首次 tick 若未初始化，则设置默认值
        if (!initialized) {
            // 默认目标：向前 10 格（Z 轴正向）
            this.targetPos = position().add(0, 0, 10);
            this.lifespan = 40;
            this.age = 0;
            this.initialized = true;
        }

        age++;

        // 寿命结束 → 删除
        if (age > lifespan) {
            discard();
            return;
        }

        // 匀速移动到目标位置
        int remaining = lifespan - age;
        if (remaining > 0) {
            Vec3 delta = targetPos.subtract(position());
            if (delta.lengthSqr() > 0.0001) {
                Vec3 velocity = delta.scale(1.0 / remaining);
                setDeltaMovement(velocity);
                move(MoverType.SELF, getDeltaMovement());
            }
        } else {
            // 精确到达目标并删除
            setPos(targetPos.x, targetPos.y, targetPos.z);
            discard();
            return;
        }

        // ---------- 攻击所有活体生物 ----------
        AABB tornadoBox = getBoundingBox().inflate(0.1); // 稍微膨胀避免边缘遗漏
        List<LivingEntity> entities = level().getEntitiesOfClass(LivingEntity.class, tornadoBox);

        for (LivingEntity entity : entities) {
            // 排除自身
            if (entity == this) continue;

            // 排除无敌生物
            if (entity.isInvulnerable()) continue;

            // 如果龙卷风有队伍，不攻击同队
            if (this.getTeam() != null && this.getTeam().isAlliedTo(entity.getTeam())) continue;

            // 如果是玩家，排除创造模式和旁观模式
            if (entity instanceof Player player) {
                if (player.isCreative() || player.isSpectator()) continue;
            }

            // 击飞（所有生物均适用）
            Vec3 pushDir = entity.position().subtract(position()).normalize();
            Vec3 push = pushDir.scale(1.5).add(0, 0.6, 0);
            entity.setDeltaMovement(push);
            if (entity instanceof Player) {
                entity.hurtMarked = true; // 玩家需要标记
            }

            // 造成 30 点伤害
            entity.hurt(this.damageSources().inFire(), 30.0F);
            //清除冰冻值
            entity.setTicksFrozen(0);
        }
    }

    // ---------- NBT 持久化 ----------
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (targetPos != null) {
            compound.putDouble("TargetX", targetPos.x);
            compound.putDouble("TargetY", targetPos.y);
            compound.putDouble("TargetZ", targetPos.z);
        }
        compound.putInt("Lifespan", lifespan);
        compound.putInt("Age", age);
        compound.putBoolean("Initialized", initialized);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("TargetX")) {
            double x = compound.getDouble("TargetX");
            double y = compound.getDouble("TargetY");
            double z = compound.getDouble("TargetZ");
            targetPos = new Vec3(x, y, z);
        }
        if (compound.contains("Lifespan")) {
            lifespan = compound.getInt("Lifespan");
        }
        if (compound.contains("Age")) {
            age = compound.getInt("Age");
        }
        if (compound.contains("Initialized")) {
            initialized = compound.getBoolean("Initialized");
        }
    }

    // ---------- 无敌 ----------
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }
}