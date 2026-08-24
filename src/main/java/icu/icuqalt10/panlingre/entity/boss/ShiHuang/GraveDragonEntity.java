package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import icu.icuqalt10.panlingre.entity.PanLingEntities;
import icu.icuqalt10.panlingre.init.ModEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Objects;

public class GraveDragonEntity extends Monster implements GeoEntity, PanLingEntities {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ===== BossBar 设置 =====
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            this.getDisplayName(),
            BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.NOTCHED_20
    ).setDarkenScreen(true)
            .setCreateWorldFog(true);

    // ===== 新增:被抓玩家锁定 =====
    private Player lockedPlayer;

    private LivingEntity attackTarget; // 攻击期间锁定的目标

    public void setAttackTarget(LivingEntity target) { this.attackTarget = target; }
    public LivingEntity getAttackTarget() { return attackTarget; }

    // ===== 服务端动画计时器系统 =====
    private String currentAnimation = "";
    private int animationTick = 0;

    // ===== 状态机字段 =====
    public enum ActionState { INTRO, IDLE_OR_WALK, ATTACKING,ATTACK_COOLDOWN, SKILL, DYING,FROZEN }
    private static final EntityDataAccessor<Integer> DATA_ACTION_STATE =
            SynchedEntityData.defineId(GraveDragonEntity.class, EntityDataSerializers.INT);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION_STATE, ActionState.IDLE_OR_WALK.ordinal());
    }

    public ActionState getActionState() {
        return ActionState.values()[this.entityData.get(DATA_ACTION_STATE)];
    }

    public void setActionState(ActionState state) {
        this.entityData.set(DATA_ACTION_STATE, state.ordinal());
    }

    public int attackCooldown = 0;       // 攻击后冷却,期间走"盯着+乱走"逻辑

    private Vec3 spawnPos = null;
    public Vec3 getSpawnPos() {
        if (this.spawnPos == null) {
            this.spawnPos = this.position();
        }
        return this.spawnPos;
    }

    // ========== 技能控制 =========

    public GraveDragonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.setNoAi(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10000.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ARMOR, 100)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 80.0);
    }

    //第一次出生检测
    private boolean firstSpawnInitialized = true;

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);

        nbt.putBoolean("FirstSpawnInitialized", this.firstSpawnInitialized);
        Vec3 pos = getSpawnPos();
        nbt.putDouble("SpawnX", pos.x);
        nbt.putDouble("SpawnY", pos.y);
        nbt.putDouble("SpawnZ", pos.z);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);

        if (nbt.contains("FirstSpawnInitialized")) {
            this.firstSpawnInitialized = nbt.getBoolean("FirstSpawnInitialized");
        }
        if (nbt.contains("SpawnX") && nbt.contains("SpawnY") && nbt.contains("SpawnZ")) {
            this.spawnPos = new Vec3(
                    nbt.getDouble("SpawnX"),
                    nbt.getDouble("SpawnY"),
                    nbt.getDouble("SpawnZ")
            );
        }
    }

    // ===== 生成时:禁止AI,播intro,倒数结束才正式开始战斗 =====
    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!this.level().isClientSide) {

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.getEntitiesOfClass(
                                GraveDragonEntity.class,
                                this.getBoundingBox().inflate(100.0D),
                                other -> other != this && other.distanceToSqr(this) <= 100.0D * 100.0D
                        )
                        .forEach(Entity::discard);
            }

            if (this.firstSpawnInitialized) {
                this.firstSpawnInitialized = false;
                //记录初始坐标
                this.spawnPos = this.position();
            }

            //记录玩家数量并刷新血量
            if (this.level() instanceof ServerLevel serverLevel) {
                int playerCount = (int) serverLevel.players().stream()
                        .filter(player -> player.distanceToSqr(this) <= 100.0D * 100.0D)
                        .count();

                Objects.requireNonNull(this.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(playerCount * 4000.0D);
            }

            //清理渲染
            this.bossEvent.removeAllPlayers();

            //回到初始状态
            float targetYaw = -90.0F;
            this.moveTo(this.spawnPos.x, this.spawnPos.y, this.spawnPos.z, targetYaw, 0.0F);
            this.setYRot(targetYaw);
            this.setYBodyRot(targetYaw);
            this.setYHeadRot(targetYaw);
            this.yRotO = targetYaw;
            this.yBodyRotO = targetYaw;
            this.yHeadRotO = targetYaw;
            this.setHealth(this.getMaxHealth());

            var scoreboard = this.level().getScoreboard();
            var monsterTeam = scoreboard.getPlayerTeam("monster");
            if (monsterTeam != null) {
                scoreboard.addPlayerToTeam(this.getStringUUID(), monsterTeam);
            }

            startAnimation("intro");

            this.setActionState(ActionState.INTRO);
            this.setNoAi(true);
            this.setInvulnerable(true);
        }
    }

    @Override
    protected void registerGoals() {
        int priority = 0;


        this.targetSelector.addGoal(priority, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        // 同步 BossBar 血量进度
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        if (this.tickCount % 20 == 0 && this.level() instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : serverLevel.players()) {
                if (this.distanceTo(player) <= 80.0D) {
                    // 如果在80格内，且还没加入BossBar，则添加
                    if (!this.bossEvent.getPlayers().contains(player)) {
                        this.bossEvent.addPlayer(player);
                    }
                } else {
                    // 如果超出了80格，移除显示
                    this.bossEvent.removePlayer(player);
                }
            }
        }


        //如果被冻结 暂停以下方法
        if (this.hasEffect(ModEffects.freeze)) return;

        // 服务端动画计时器
        if (!currentAnimation.isEmpty()) {
            animationTick++;
            tickAnimation(currentAnimation, animationTick);
        }

        //攻击冷却
        if (this.getActionState() == ActionState.ATTACK_COOLDOWN) {
            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }

            if (this.attackCooldown <= 0) {
                this.attackCooldown = 0;
                this.setActionState(ActionState.IDLE_OR_WALK);
            }
        }
    }

    // 防止因距离玩家过远而自然消失
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean shouldDespawnInPeaceful() {
        return false;
    }

    // 实体被移除（如自然刷掉、代码强制移除、死亡动画播完后）时
    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide()) {
            //清理渲染
            this.bossEvent.removeAllPlayers();
        }
        super.remove(reason);
    }

    // 当玩家离开这个实体的加载/渲染范围、或者退出游戏时
    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);

        if (!this.level().isClientSide()) {
            //清理渲染
            this.bossEvent.removeAllPlayers();
        }
    }

    // ===== 攻击命中后调用 =====
    public boolean LastAttackIsCommon = false;
    public boolean cooldownStartedThisAttack = false;

    public void startAttackCooldown() {
        this.attackCooldown = 50;
        this.setActionState(ActionState.ATTACK_COOLDOWN);
    }

    public boolean isInAttackCooldown() {
        return attackCooldown > 0;
    }

    public boolean isAttacking() {
        return getActionState() == ActionState.ATTACKING;
    }

    public void setAttacking(boolean attacking) {
        this.setActionState(attacking ? ActionState.ATTACKING : ActionState.IDLE_OR_WALK);
    }

    @Override
    public void die(DamageSource source) {
        this.cooldownStartedThisAttack = true;
        if (getActionState() != ActionState.DYING) {
            this.setHealth(0.01f);
            this.setActionState(ActionState.DYING);
            this.setNoAi(true);
            this.setInvulnerable(true);

            startAnimation("died");
        }
    }

    // ===== GeckoLib动画 =====
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "body_controller", 5, this::bodyPredicate));

        controllers.add(new AnimationController<>(this, "action_controller", 0, this::attackPredicate)
                .triggerableAnim("intro", RawAnimation.begin().thenPlay("intro"))
        );
    }

    private PlayState bodyPredicate(AnimationState<GraveDragonEntity> event) {
        switch (getActionState()) {
            case INTRO, DYING, SKILL -> {
                return PlayState.STOP;
            }
            case FROZEN -> {
                event.getController().setAnimationSpeed(0.001D);
                return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            }
            default -> {
                event.getController().setAnimationSpeed(1.0D);
                return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            }
        }
    }

    private PlayState attackPredicate(AnimationState<GraveDragonEntity> event) {
        // 如果实体进入冻结，立刻终止攻击控制器的一切动画
        if (this.getActionState() == ActionState.FROZEN) {
            return PlayState.STOP;
        }
        event.getController().setAnimationSpeed(1.0D);
        return PlayState.CONTINUE;
    }

    // 开始播放动画（服务端调用）
    public void startAnimation(String animName) {
        this.currentAnimation = animName;
        this.animationTick = 0;
        this.triggerAnim("action_controller", animName);
    }

    // 停止动画
    private void stopAnimation() {
        this.stopTriggeredAnim("action_controller",this.currentAnimation);
        this.currentAnimation = "";
        this.animationTick = 0;
    }

    @Override
    public void whenFroozen() {
        this.setActionState(ActionState.FROZEN);
        stopAnimation();
    }

    @Override
    public void whenUnFroozen() {
        this.setActionState(ActionState.IDLE_OR_WALK);
        this.setAttacking(false);
        this.startAttackCooldown();
    }

    // ===== 服务端动画计时器 =====
    private void tickAnimation(String animName, int tick) {
        switch (animName) {
            case "intro" -> {
                if (tick == 172) {
                }
            }
        }
    }

    // ===== 方法 =====

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
