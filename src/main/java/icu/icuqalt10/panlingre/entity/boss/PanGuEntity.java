package icu.icuqalt10.panlingre.entity.boss;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.client.ClientModEvents;
import icu.icuqalt10.panlingre.client.GroundSmashRenderer;
import icu.icuqalt10.panlingre.entity.FireTornadoEntity;
import icu.icuqalt10.panlingre.entity.FireTrailTracker;
import icu.icuqalt10.panlingre.entity.boss.PanGu.*;
import icu.icuqalt10.panlingre.event.GameBusEvents;
import icu.icuqalt10.panlingre.init.ModEffects;
import icu.icuqalt10.panlingre.init.ModEntities;
import icu.icuqalt10.panlingre.network.AttackInstructionPayload;
import icu.icuqalt10.panlingre.network.particle.GatherBall;
import icu.icuqalt10.panlingre.network.particle.ParticleCluster;
import icu.icuqalt10.panlingre.network.particle.ParticleLighting;
import icu.icuqalt10.panlingre.util.BlockSet;
import icu.icuqalt10.panlingre.util.LocalWeatherManager;
import icu.icuqalt10.panlingre.util.SkillHelper;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.keyframe.event.CustomInstructionKeyframeEvent;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import java.util.ArrayList;
import java.util.List;

public class PanGuEntity extends Monster implements GeoEntity {

    // ===== BossBar 设置 =====
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            this.getDisplayName(),
            BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.NOTCHED_20
    ).setDarkenScreen(true)
            .setCreateWorldFog(true);

    // ===== 新增:当前攻击上下文,用来区分"damage"指令该怎么处理 =====
    public enum AttackKind { NONE, MELEE, DASH }
    private AttackKind currentAttackKind = AttackKind.NONE;

    // ===== 新增:冲刺移动相关运行时状态,由throw.start/throw.end驱动 =====
    private boolean dashMoving = false;
    private Vec3 dashDir = Vec3.ZERO;
    private double dashStepPerTick = 0;
    private int dashTicksLeft = 0;        // 新增:冲刺还剩多少tick,归零自动停
    private boolean dashHasHit = false;

    // ===== 新增:被抓玩家锁定 =====
    private Player lockedPlayer;
    private static final ResourceLocation CATCH_LOCK_ID =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "catch_lock");

    private LivingEntity attackTarget; // 攻击期间锁定的目标

    public void setAttackTarget(LivingEntity target) { this.attackTarget = target; }
    public LivingEntity getAttackTarget() { return attackTarget; }

    // ===== 给Goal调用,声明"我接下来要播放什么类型的攻击" =====
    public void beginAttack(AttackKind kind) {
        this.currentAttackKind = kind;
        this.cooldownStartedThisAttack = false;
    }

    public void endAttack() {
        this.currentAttackKind = AttackKind.NONE;
        this.dashMoving = false;
    }

    // ===== 给DashAttackGoal读取冲刺移动状态 =====
    public boolean isDashMoving() { return dashMoving; }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ===== 状态机字段 =====
    public enum ActionState { INTRO, IDLE_OR_WALK, ATTACKING,ATTACK_COOLDOWN, SKILL, DYING }
    private static final EntityDataAccessor<Integer> DATA_ACTION_STATE =
            SynchedEntityData.defineId(PanGuEntity.class, EntityDataSerializers.INT);

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
    private boolean diedAnimPlaying = false;
    private int diedAnimTicks = 58;       // died动画2.875s ≈ 58tick

    private Vec3 spawnPos = null;
    public Vec3 getSpawnPos() {
        if (this.spawnPos == null) {
            this.spawnPos = this.position();
        }
        return this.spawnPos;
    }

    // ========== 技能控制 =========
    public boolean SkillPhase1Triggered = false;
    public boolean SkillPhase2Triggered = false;
    private BlockPos srcFrom = new BlockPos(3022,129,-2351);
    private BlockPos srcTo = new BlockPos(3037 ,150,-2334);
    private BlockPos mountain1 = new BlockPos(3071 ,129,-2254);
    private BlockPos mountain2 = new BlockPos(3104 ,129,-2215);
    private BlockPos mountain3 = new BlockPos(3102 ,129,-2279);
    private BlockPos mountain4 = new BlockPos(3153 ,129,-2250);
    private final List<Shockwave> activeShockwaves = new ArrayList<>(); //震动波
    private AABB mountain1Box = new AABB(new Vec3(mountain1.getX(),mountain1.getY(),mountain1.getZ()),
            new Vec3(mountain1.getX()+16,mountain1.getY()+25,mountain1.getZ()+18));
    private AABB mountain2Box = new AABB(new Vec3(mountain2.getX(),mountain2.getY(),mountain2.getZ()),
            new Vec3(mountain2.getX()+16,mountain2.getY()+25,mountain2.getZ()+18));
    private AABB mountain3Box = new AABB(new Vec3(mountain3.getX(),mountain3.getY(),mountain3.getZ()),
            new Vec3(mountain3.getX()+16,mountain3.getY()+25,mountain3.getZ()+18));
    private AABB mountain4Box = new AABB(new Vec3(mountain4.getX(),mountain4.getY(),mountain4.getZ()),
            new Vec3(mountain4.getX()+16,mountain4.getY()+25,mountain4.getZ()+18));
    private boolean is_snowing = false;
    public boolean isInSnowing() {return is_snowing;}
    // 天气轮子
    private final LocalWeatherManager weatherManager = new LocalWeatherManager(this);

    public PanGuEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.setNoAi(false);
        this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_AXE));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 2000.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ARMOR, 500)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 80.0);
    }

    private int introTicks = 172;          // 出生时倒数
    private boolean firstSpawnInitialized = false;

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);

        nbt.putBoolean("FirstSpawnInitialized", this.firstSpawnInitialized);
        Vec3 pos = getSpawnPos();
        nbt.putDouble("SpawnX", pos.x);
        nbt.putDouble("SpawnY", pos.y);
        nbt.putDouble("SpawnZ", pos.z);
        nbt.putBoolean("SkillPhase1Triggered", this.SkillPhase1Triggered);
        nbt.putBoolean("SkillPhase2Triggered", this.SkillPhase2Triggered);
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
        if (nbt.contains("SkillPhase1Triggered")) {
            this.SkillPhase1Triggered = nbt.getBoolean("SkillPhase1Triggered");
        }
        if (nbt.contains("SkillPhase2Triggered")) {
            this.SkillPhase2Triggered = nbt.getBoolean("SkillPhase2Triggered");
        }
    }
    // ===== 生成时:禁止AI,播intro,倒数结束才正式开始战斗 =====
    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!this.level().isClientSide && !this.firstSpawnInitialized) {
            this.firstSpawnInitialized = true;
            this.setActionState(ActionState.INTRO);
            this.setNoAi(true);
            this.setInvulnerable(true);
            this.triggerAnim("action_controller","intro");
            //记录初始坐标
            this.spawnPos = this.position();
        }
    }

    @Override
    protected void registerGoals() {
        int priority = 0;
        this.goalSelector.addGoal(priority++, new PostAttackBehaviorGoal(this));
        this.goalSelector.addGoal(priority++, new Heavy2Goal(this));
        this.goalSelector.addGoal(priority++, new DashAttackGoal(this));
        this.goalSelector.addGoal(priority++, new MeleeComboGoal(this));
        this.goalSelector.addGoal(priority++, new ApproachTargetGoal(this));

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

        //天气
        weatherManager.tick();
        //下雪天
        if(isInSnowing() && this.level() instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : serverLevel.players()) {
                if (this.distanceTo(player) <= 80.0D) {
                    int frozenTicks = player.getTicksFrozen();
                    //冰冻满 扣血
                    if (frozenTicks >= 140 && this.tickCount/20 == 0) {
                        player.hurt(this.damageSources().freeze(), player.getMaxHealth()*0.2f);
                    }
                    //增加冰冻值
                    else if (FireTrailTracker.isPlayerInTrail(player)) {
                        player.setTicksFrozen(frozenTicks-3);
                    } else {
                        player.setTicksFrozen(frozenTicks+5);}
                }
            }
        }

        //攻击冷却
        if (this.getActionState() == ActionState.ATTACK_COOLDOWN) {
            this.attackCooldown--;

            LivingEntity target = this.getTarget();
            if (target == null || this.distanceTo(target) > 15.0) {
                this.attackCooldown = 0;
            }
            if (this.attackCooldown <= 0) {
                this.setActionState(PanGuEntity.ActionState.IDLE_OR_WALK);
            }
        }

        // 冲刺移动:只要throw.start到throw.end之间这段,每tick固定步进
        if (dashMoving) {
            // 移动一步
            Vec3 delta = dashDir.scale(dashStepPerTick);
            this.move(MoverType.SELF, delta);
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            dashTicksLeft--;

            // 冲刺过程中实时检测有没有撞到目标
            LivingEntity target = this.getAttackTarget();
            if (!dashHasHit && target != null && this.distanceTo(target) < 1.5) {
                dashHasHit = true;
                dashMoving = false;
                this.triggerAnim("action_controller", "attack.throw.catch");
            }

            // 移动窗口用完还没撞到→停下,等throw.end播放miss收尾
            if (dashMoving && dashTicksLeft <= 0) {
                dashMoving = false;
                // 没命中,throw.end动画会在关键帧自然触发doThrowEnd()播放miss
            }
        }

        switch (getActionState()) {
            case INTRO -> tickIntro();
            case DYING -> tickDying();
        }

        if (!this.activeShockwaves.isEmpty()) {
            // 调用震动波自己的 tick。当其返回 false（即超时），自动将其从列表中剔除销毁
            this.activeShockwaves.removeIf(wave -> !wave.tick((ServerLevel) this.level(), this));
        }
    }

    private void tickIntro() {
        if (introTicks > 0) {
            introTicks--;
            if (introTicks == 0) {
                this.setNoAi(false);
                this.setInvulnerable(false);
                this.setActionState(ActionState.IDLE_OR_WALK);
            }
        }
    }

    private void tickDying() {
        if (diedAnimTicks > 0) {
            diedAnimTicks--;
        } else if (diedAnimPlaying) {
            diedAnimPlaying = false;
            this.remove(RemovalReason.KILLED);
        }
    }

    //永不移除
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        if (!this.level().isClientSide()) {
            //清理渲染
            weatherManager.cleanup();
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
            this.firstSpawnInitialized = false;
            this.SkillPhase1Triggered = false;
            this.SkillPhase2Triggered = false;
            SkillPhase1Finish();
            SkillPhase2Finish();
        }
        return false;
    }

    // 实体被移除（如自然刷掉、代码强制移除、死亡动画播完后）时，清理所有玩家的 BossBar
    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide()) {
            weatherManager.cleanup();
            this.bossEvent.removeAllPlayers();
        }
        super.remove(reason);
    }

    // 当玩家离开这个实体的加载/渲染范围、或者退出游戏时，强制移除 BossBar
    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    // ===== 攻击命中后调用 =====
    public boolean LastAttackIsCommon = false;
    public boolean cooldownStartedThisAttack = false;

    public void startAttackCooldown() {
        this.attackCooldown = 20;
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

    // ==== 受击 ====
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 掉出世界 或 kill
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC_KILL)) {
            return super.hurt(source, amount);
        }


        if (!this.level().isClientSide()) {
            //一阶段判定
            if (!this.SkillPhase1Triggered) {
                float goalHealth = this.getMaxHealth() * 0.75f;

                if (this.getHealth() - amount <= goalHealth) {
                    this.SkillPhase1Run();

                    this.markHurt();
                    this.playSound(this.getHurtSound(source), this.getSoundVolume(), this.getVoicePitch());

                    return true;
                }
            }
            //二阶段判定
            if (!this.SkillPhase2Triggered) {
                float goalHealth = this.getMaxHealth() * 0.5f;

                if (this.getHealth() - amount <= goalHealth) {
                    this.SkillPhase2Run();

                    this.markHurt();
                    this.playSound(this.getHurtSound(source), this.getSoundVolume(), this.getVoicePitch());

                    return true;
                }
            }
        }

        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        //技能判定
        if (!this.level().isClientSide()) {
            //一阶段判定
            if (!this.SkillPhase1Triggered) {
                float goalHealth = this.getMaxHealth() * 0.75f;

                if (this.getHealth() <= goalHealth) {
                    this.SkillPhase1Run();
                    return;
                }
            }
            //二阶段判定
            if (!this.SkillPhase2Triggered) {
                float goalHealth = this.getMaxHealth() * 0.5f;

                if (this.getHealth() <= goalHealth) {
                    this.SkillPhase2Run();
                    return;
                }
            }
        }

        this.cooldownStartedThisAttack = true;
        if (getActionState() != ActionState.DYING) {
            this.setHealth(0.01f);
            this.setActionState(ActionState.DYING);
            this.diedAnimPlaying = true;
            this.setNoAi(true);
            this.setInvulnerable(true);
            //强制转向
            float targetYaw = -90.0F;
            this.moveTo(this.getX(),this.getY(),this.getZ(), targetYaw, 0.0F);
            this.setYRot(targetYaw);
            this.setYBodyRot(targetYaw);
            this.setYHeadRot(targetYaw);
            this.yRotO = targetYaw;
            this.yBodyRotO = targetYaw;
            this.yHeadRotO = targetYaw;

            triggerAnim("action_controller", "died");
            this.diedAnimTicks = 60;
        }
    }

    // ==== 一阶段技能 ====
    private void SkillPhase1Run() {
        this.SkillPhase1Triggered = true;
        //设置属性
        this.setAttacking(true);
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setHealth(this.getMaxHealth() * 0.75f);
        this.setPos(this.getSpawnPos());
        //强制转向
        float targetYaw = -90.0F;
        this.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, targetYaw, 0.0F);
        this.setYRot(targetYaw);
        this.setYBodyRot(targetYaw);
        this.setYHeadRot(targetYaw);
        this.yRotO = targetYaw;
        this.yBodyRotO = targetYaw;
        this.yHeadRotO = targetYaw;

        this.setActionState(ActionState.SKILL);
        this.triggerAnim("action_controller","skill.phase1");

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 5.0f,1.0f);
    }

    // ==== 二阶段技能 ====
    private void SkillPhase2Run() {
        this.SkillPhase2Triggered = true;
        //设置属性
        this.setAttacking(true);
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setHealth(this.getMaxHealth() * 0.5f);
        this.setPos(this.getSpawnPos());
        //强制转向
        float targetYaw = -90.0F;
        this.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, targetYaw, 0.0F);
        this.setYRot(targetYaw);
        this.setYBodyRot(targetYaw);
        this.setYHeadRot(targetYaw);
        this.yRotO = targetYaw;
        this.yBodyRotO = targetYaw;
        this.yHeadRotO = targetYaw;

        this.setActionState(ActionState.SKILL);
        this.triggerAnim("action_controller","skill.phase2");

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 5.0f,1.0f);
    }

    // ===== GeckoLib动画 =====
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "body_controller", 5, this::bodyPredicate)
                .setCustomInstructionKeyframeHandler(this::handleInstruction));

        controllers.add(new AnimationController<>(this, "action_controller", 0, this::attackPredicate)
                .triggerableAnim("intro", RawAnimation.begin().thenPlay("intro"))
                .triggerableAnim("died", RawAnimation.begin().thenPlay("died"))
                .triggerableAnim("attack.heavy", RawAnimation.begin().thenPlay("attack.heavy"))
                .triggerableAnim("attack.heavy2", RawAnimation.begin().thenPlay("attack.heavy2"))
                .triggerableAnim("attack.skill1", RawAnimation.begin().thenPlay("attack.skill1"))
                .triggerableAnim("attack.skill2", RawAnimation.begin().thenPlay("attack.skill2"))
                .triggerableAnim("attack.skill3", RawAnimation.begin().thenPlay("attack.skill3"))
                .triggerableAnim("attack.skill4", RawAnimation.begin().thenPlay("attack.skill4"))
                .triggerableAnim("attack.combo", RawAnimation.begin().thenPlay("attack.combo"))
                .triggerableAnim("attack.throw", RawAnimation.begin().thenPlay("attack.throw"))
                .triggerableAnim("attack.throw.catch", RawAnimation.begin().thenPlay("attack.throw.catch"))
                .triggerableAnim("attack.throw.end", RawAnimation.begin().thenPlay("attack.throw.end"))
                .triggerableAnim("skill.phase1", RawAnimation.begin().thenPlay("skill.phase1"))
                .triggerableAnim("skill.phase2", RawAnimation.begin().thenPlay("skill.phase2"))
                .setCustomInstructionKeyframeHandler(this::handleInstruction));
    }

    private PlayState bodyPredicate(AnimationState<PanGuEntity> event) {
        switch (getActionState()) {
            case INTRO, DYING, SKILL -> {
                return PlayState.STOP;
            }
            default -> {
                if (event.isMoving()) {
                    Player nearest = event.getAnimatable().level().getNearestPlayer(
                            event.getAnimatable().getX(), event.getAnimatable().getY(), event.getAnimatable().getZ(),
                            80.0D, false
                    );

                    if (nearest == null) {
                        event.getController().setAnimationSpeed(1.0D);
                        return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
                    }

                    if (event.getAnimatable().distanceTo(nearest) > 15) {
                        event.getController().setAnimationSpeed(1.0D);
                        return event.setAndContinue(RawAnimation.begin().thenLoop("running"));
                    } else {
                        event.getController().setAnimationSpeed(0.75D);
                    }

                    event.getController().setAnimationSpeed(1.0D);
                    return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
                }

                event.getController().setAnimationSpeed(1.0D);
                return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            }
        }
    }

    private PlayState attackPredicate(AnimationState<PanGuEntity> state) {
        // 如果实体进入死亡状态，立刻终止攻击控制器的一切动画
        if (this.getActionState() == ActionState.DYING) {
            return PlayState.STOP;
        }
        return PlayState.CONTINUE;
    }

    private void handleInstruction(CustomInstructionKeyframeEvent<PanGuEntity> event) {
        if (!this.level().isClientSide) return;
        String instructions = event.getKeyframeData().getInstructions();
        for (String part : instructions.split(";")) {
            part = part.trim();
            if (!part.isEmpty()) {
                PacketDistributor.sendToServer(
                        new AttackInstructionPayload(this.getId(), part));
            }
        }
    }

    private final java.util.Map<String, Long> lastExecuteTime = new java.util.HashMap<>();

    public void serverHandleInstruction(String instruction) {
        if (this.level().isClientSide) return;

        long now = this.level().getGameTime();
        Long last = lastExecuteTime.get(instruction);
        // 同一个指令,如果距离上次执行不到3tick,认为是多人重发的同一帧,丢弃
        if (last != null && now - last < 3) {
            return;
        }
        lastExecuteTime.put(instruction, now);

        switch (instruction) {
            case "intro.end" -> {this.introTicks = 1;}
            case "teleport" -> doTeleportToTarget();
            case "damage.heavy" -> {
                doDamage(20.0f, AttackKind.MELEE);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 0.5f,1.0f);
            }
            case "damage.combo.1",
                 "damage.combo.2",
                 "damage.combo.3"
                    -> {
                doDamage(8.0f, AttackKind.MELEE);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.5f,1.0f);
            }
            case "damage.catch" -> {
                doDamage(25.0f, AttackKind.MELEE);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 0.5f,1.0f);
            }
            case "throw.start" -> doThrowStart();
            case "throw.end" -> doThrowEnd();
            case "catch.start" -> doCatchStart();
            case "catch.end" -> doCatchEnd();
            case "damage.finish" -> DamageFinish();
            case "attack.finish" -> AttackFinish();
            case "died" -> ActionDied();
            case "attack.transform" -> this.setInvulnerable(true);
            case "attack.weakness" -> AttackWeakness();
            case "damage.heavy2" -> DamageHeavy2();
            case "damage.skill1" -> DamageSkill1();
            case "damage.skill2" -> DamageSkill2();
            case "damage.skill3" -> DamageSkill3();
            case "damage.skill4" -> DamageSkill4();

            case "skill.phase1.particle.cold" -> SkillPhase1Cold();
            case "skill.phase1.weather" -> SkillPhase1Weather();
            case "skill.phase1.start_snow" -> this.is_snowing = true;
            case "skill.phase1.fire" -> Skill1Phase1Fire();
            case "skill.phase1.finish" -> SkillPhase1Finish();

            case "skill.phase2.weather" -> SkillPhase2Weather();
            case "skill.phase2.start_attack" -> SkillPhase2AttackStart();
            case "skill.phase2.attack" -> SkillPhase2Attack();
            case "skill.phase2.lighting" -> SkillPhase2Lighting();
            case "skill.phase2.finish" -> SkillPhase2Finish();

            case "skill.transform" -> this.setInvulnerable(false);
            case "skill.end" -> SkillEnd();

            default -> {
                if (this.level() instanceof ServerLevel sl) {
                    runCommand(sl, instruction);
                }
            }
        }
    }

    private void doTeleportToTarget() {
        LivingEntity target = this.getAttackTarget();
        if (target == null) return;
        Vec3 toTarget = new Vec3(target.getX() - this.getX(), 0, target.getZ() - this.getZ());
        if (toTarget.lengthSqr() < 0.01) return;
        Vec3 dir = toTarget.normalize();
        Vec3 landing = target.position().subtract(dir.scale(1.5));
        this.setPos(landing.x, this.getY(), landing.z);

        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
        this.getNavigation().stop();
    }
    private void doDamage(float amount, AttackKind kind) {
        LivingEntity target = this.getAttackTarget();
        if (target == null) return;
        if (this.distanceTo(target) < 5.0) {
            target.hurt(this.damageSources().mobAttack(this), amount);
        }
    }
    private void doDamage(Vec3 Pos,float amount, AttackKind kind,float rad) {
        LivingEntity target = this.getAttackTarget();
        if (target == null) return;
        if (target.distanceToSqr(Pos) < rad*rad) {
            target.hurt(this.damageSources().mobAttack(this), amount);
        }
    }

    private void doThrowStart() {
        LivingEntity target = this.getAttackTarget();
        if (target == null) { dashMoving = false; return; }

        Vec3 toTarget = new Vec3(target.getX() - this.getX(), 0, target.getZ() - this.getZ());
        double rawDist = toTarget.length();
        if (rawDist < 0.1) { dashMoving = false; return; }
        dashDir = toTarget.normalize();

        // 锁定身体朝向冲刺方向
        float yaw = (float) Math.toDegrees(Math.atan2(-dashDir.x, dashDir.z));
        this.setYRot(yaw);
        this.setYBodyRot(yaw);

        double desiredDist = Math.min(15.0, rawDist + 1.0);
        HitResult wallCheck = this.level().clip(new ClipContext(
                this.position(), this.position().add(dashDir.scale(desiredDist)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        double realDist = wallCheck.getType() != HitResult.Type.MISS
                ? this.position().distanceTo(wallCheck.getLocation())
                : desiredDist;

        dashTicksLeft = 10;
        dashStepPerTick = realDist / dashTicksLeft *2;
        dashHasHit = false;
        dashMoving = true;
    }

    private void doThrowEnd() {
        dashMoving = false; // 双保险
        if (!dashHasHit) {
            this.triggerAnim("action_controller", "attack.throw.end");
        }
    }

    private void doCatchStart() {
        doDamage(10.0f, AttackKind.DASH);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.HOSTILE, 0.5f,1.0f);
        LivingEntity target = this.getAttackTarget();
        if (!(target instanceof Player player)) return;
        lockedPlayer = player;

        SkillHelper.freezeEntity(player,20);
    }

    private void doCatchEnd() {
        if (lockedPlayer == null) return;

        SkillHelper.unfreezeEntity(lockedPlayer);

        lockedPlayer = null;
    }

    private void AttackWeakness() {
        this.setInvulnerable(false);
        this.addEffect(new MobEffectInstance(ModEffects.po_jia, 20, 4));
    }

    private void DamageHeavy2() {
        ClientModEvents.startShake(this.position(),80,5, 1.5f);
        Vec3 position = this.position();
        Vec3 lookVec = this.getLookAngle();
        Vec3 targetPos = position.add(lookVec.scale(13));;
        doDamage(targetPos,25.0f, AttackKind.MELEE,5);
        this.level().playSound(null, targetPos.x,targetPos.y,targetPos.z,
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.NEUTRAL, 5f,1.0f);
        GroundSmashRenderer.triggerSmash(targetPos, 4, 10);
    }

    private void DamageSkill1() {
        ClientModEvents.startShake(this.position(),80,3, 1.5f);

        Vec3 position = new Vec3(this.position().x,this.position().y+10,this.position().z);
        Vec3 lookVec = this.getLookAngle();
        Vec3 targetPos = position.add(lookVec.scale(20));;
        PacketDistributor.sendToPlayersTrackingEntity(
                this,
                new ParticleCluster(
                        position, targetPos,
                        ParticleTypes.SNOWFLAKE,
                        3000, 12
                ));

        List<LivingEntity> targetEntities = SkillHelper.getLivingEntitiesInFront(this, 10.0, 10.0, 20.0);
        for (LivingEntity entity : targetEntities) {
            SkillHelper.freezeEntity(entity,100);
            entity.setTicksFrozen(140);
        }

        this.level().playSound(null, this.position().x,this.position().y,this.position().z,
                SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 5f,1.0f);

        if (this.level() instanceof ServerLevel sl) {
            runCommand(sl, "execute as @a[distance=..80] run dialog show boss_pangu_skill_1");
        }
    }

    private void DamageSkill2() {
        ClientModEvents.startShake(this.position(),80,3, 1.5f);
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        Vec3 position = this.position();
        Vec3 lookVec = this.getLookAngle();
        Vec3 targetPos = position.add(lookVec.scale(15));

        Vec3 pos = this.position();                    // 实体绝对坐标
        Vec3 forward = this.getLookAngle();            // 单位前向量（含俯仰）
        Vec3 up = new Vec3(0, 1, 0);                    // 世界 Y 轴
        Vec3 left = up.cross(forward).normalize();      // 单位左向量（水平方向，与俯仰无关）

        Vec3 midStart = pos.add(forward);               // 实体前方 1 格
        Vec3 leftStart = pos.add(left.scale(-5)).add(forward);  // 左 5，前 1
        Vec3 rightStart = pos.add(left.scale(5)).add(forward);  // 右 5，前 1

        Vec3 midTarget = pos.add(forward.scale(31));                // 中间笔直 30 格
        Vec3 leftTarget = pos.add(left.scale(5)).add(forward.scale(29));   // 左起点 → 右前方
        Vec3 rightTarget = pos.add(left.scale(-5)).add(forward.scale(29)); // 右起点 → 左前方

        //生成3个火龙卷
        FireTornadoEntity tornadoMid = new FireTornadoEntity(ModEntities.FIRE_TORNADO.get(), serverLevel);
        tornadoMid.setPos(midStart);
        tornadoMid.setMovementParameters(midTarget, 20);
        serverLevel.addFreshEntity(tornadoMid);
        FireTornadoEntity tornadoLeft = new FireTornadoEntity(ModEntities.FIRE_TORNADO.get(), serverLevel);
        tornadoLeft.setPos(leftStart);
        tornadoLeft.setMovementParameters(leftTarget, 20);
        serverLevel.addFreshEntity(tornadoLeft);
        FireTornadoEntity tornadoRight = new FireTornadoEntity(ModEntities.FIRE_TORNADO.get(), serverLevel);
        tornadoRight.setPos(rightStart);
        tornadoRight.setMovementParameters(rightTarget, 20);
        serverLevel.addFreshEntity(tornadoRight);

        this.level().playSound(null, targetPos.x,targetPos.y,targetPos.z,
                SoundEvents.BREEZE_SHOOT, SoundSource.NEUTRAL, 5f,1.0f);

        if (this.level() instanceof ServerLevel sl) {
            runCommand(sl, "execute as @a[distance=..80] run dialog show boss_pangu_skill_2");
        }
    }

    private void DamageSkill3() {
        ClientModEvents.startShake(this.position(),80,3, 1.5f);

        SkillPhase2Lighting();

        if (this.level() instanceof ServerLevel sl) {
            runCommand(sl, "execute as @a[distance=..80] run dialog show boss_pangu_skill_3");
        }
    }

    private void DamageSkill4() {
        ClientModEvents.startShake(this.position(),80,5, 1.5f);
        Vec3 position = this.position();
        Vec3 lookVec = this.getLookAngle();
        Vec3 targetPos = position.add(lookVec.scale(3.5));;
        doDamage(targetPos,30.0f, AttackKind.MELEE,3.5f);
        this.level().playSound(null, targetPos.x,targetPos.y,targetPos.z,
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.NEUTRAL, 5f,1.0f);

        GroundSmashRenderer.triggerSmash(targetPos, 4, 15);
        this.activeShockwaves.add(new Shockwave(targetPos,5));

        if (this.level() instanceof ServerLevel sl) {
            runCommand(sl, "execute as @a[distance=..80] run dialog show boss_pangu_skill_4");
        }
    }

    private void SkillPhase1Cold() {
        PacketDistributor.sendToPlayersTrackingEntity(
                this,
                new GatherBall(
                        this.position().add(32,42,5),
                        ParticleTypes.SNOWFLAKE,
                        500,10,1
                ));
        this.level().playSound(null, this.getX(),this.getY(),this.getZ(),
                SoundEvents.BREEZE_INHALE, SoundSource.NEUTRAL, 5.0f,1.0f);
    }

    private void SkillPhase1Weather() {
        //开启下雪检测
        BlockSet builder = new BlockSet((ServerLevel) this.level());
        builder.doFill(new BlockPos(3186, 128, -2180),new BlockPos(3047, 128, -2299), Blocks.SNOW_BLOCK);
        this.level().playSound(null, this.getX(),this.getY(),this.getZ(),
                SoundEvents.SNOW_PLACE, SoundSource.NEUTRAL, 5.0f,1.0f);

        weatherManager.setWeather(LocalWeatherManager.WeatherType.SNOW, 80.0);
        if (this.level() instanceof ServerLevel serverLevel) {
            AABB searchBox = AABB.ofSize(this.position(), 160, 160, 160);
            List<ServerPlayer> players = serverLevel.getEntitiesOfClass(ServerPlayer.class, searchBox);

            for (ServerPlayer sp : players) {
                serverLevel.sendParticles(
                        sp,
                        ParticleTypes.SNOWFLAKE,
                        true,
                        this.getX() + 32, this.getY() + 42, this.getZ() + 5,
                        3000,
                        0, 0, 0,
                        2D
                );
            }
        }
        this.level().playSound(null, this.getX(),this.getY(),this.getZ(),
                SoundEvents.BREEZE_JUMP, SoundSource.NEUTRAL, 5.0f,1.0f);
    }

    private void Skill1Phase1Fire() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        this.level().playSound(null, this.getX(),this.getY(),this.getZ(),
                SoundEvents.BREEZE_SHOOT, SoundSource.NEUTRAL, 5.0f,1.0f);

        // 获取周围半径 80 格内的所有玩家
        AABB searchBox = this.getBoundingBox().inflate(80.0);
        List<ServerPlayer> players = serverLevel.getEntitiesOfClass(ServerPlayer.class, searchBox);

        for (ServerPlayer player : players) {
            // 记录玩家当前位置
            final Vec3 playerPos = player.position();

            // 1. 在玩家附近 5 格左右的位置生成初始火龙卷
            double angle = random.nextDouble() * 2 * Math.PI; // 随机角度
            double distance = 6.0 + random.nextDouble() * 4.0; // 6~10 格距离
            double offsetX = Math.cos(angle) * distance;
            double offsetZ = Math.sin(angle) * distance;

            Vec3 spawnPos = new Vec3(playerPos.x + offsetX, playerPos.y, playerPos.z + offsetZ);

            // 计算火龙卷朝向玩家的方向
            Vec3 directionToPlayer = playerPos.subtract(spawnPos).normalize();
            double travelDistance = 8.0 + random.nextDouble() * 4.0; // 8~12 格
            Vec3 targetPos = spawnPos.add(directionToPlayer.scale(travelDistance));

            // 生成火龙卷实体
            FireTornadoEntity tornado = new FireTornadoEntity(ModEntities.FIRE_TORNADO.get(), serverLevel);
            tornado.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            tornado.setMovementParameters(targetPos, 20);
            serverLevel.addFreshEntity(tornado);

            // 在周围生成额外的火龙卷
            for (int i = 1; i <= 3; i++) {
                final int waveIndex = i;
                GameBusEvents.queueTask(5 * i, () -> {
                    if (!serverLevel.getServer().isRunning() || player.hasDisconnected() || !player.isAlive()) return;

                    double waveAngle = random.nextDouble() * 2 * Math.PI;
                    double waveDistance = 8.0 + random.nextDouble() * 4.0; // 8~12 格
                    double waveOffsetX = Math.cos(waveAngle) * waveDistance;
                    double waveOffsetZ = Math.sin(waveAngle) * waveDistance;

                    Vec3 waveSpawnPos = new Vec3(
                        player.getX() + waveOffsetX,
                        player.getY(),
                        player.getZ() + waveOffsetZ
                    );

                    // 随机方向
                    double randomAngle = random.nextDouble() * 2 * Math.PI;
                    Vec3 randomDirection = new Vec3(Math.cos(randomAngle), 0, Math.sin(randomAngle));
                    double waveTravelDistance = 10.0 + random.nextDouble() * 8.0; // 10~16 格
                    Vec3 waveTargetPos = waveSpawnPos.add(randomDirection.scale(waveTravelDistance));

                    // 生成火龙卷
                    FireTornadoEntity waveTornado = new FireTornadoEntity(ModEntities.FIRE_TORNADO.get(), serverLevel);
                    waveTornado.setPos(waveSpawnPos.x, waveSpawnPos.y, waveSpawnPos.z);
                    waveTornado.setMovementParameters(waveTargetPos, 20);
                    serverLevel.addFreshEntity(waveTornado);
                });
            }
        }
    }

    private void SkillPhase1Finish() {
        //改回场地
        BlockSet builder = new BlockSet((ServerLevel) this.level());
        builder.doFill(new BlockPos(3186, 128, -2180),new BlockPos(3047, 128, -2299), Blocks.PRISMARINE);
        this.level().playSound(null, this.getX(),this.getY(),this.getZ(),
                SoundEvents.SNOW_BREAK, SoundSource.NEUTRAL, 5.0f,1.0f);

        //关闭天气
        weatherManager.stop();
        this.is_snowing = false;
        //清空冰冻
        if(this.level() instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : serverLevel.players()) {
                if (this.distanceTo(player) <= 80.0D) {
                        player.setTicksFrozen(0);
                }
            }
        }

        if (this.level() instanceof ServerLevel sl) {
            runCommand(sl, "execute as @a[distance=..80] run dialog show boss_pangu_skill_end");
        }
    }

    private void SkillPhase2Weather() {
        weatherManager.setWeather(LocalWeatherManager.WeatherType.THUNDER, 80.0);
        this.level().playSound(null, this.getX(),this.getY(),this.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.NEUTRAL, 5.0f,1.0f);
    }

    private void SkillPhase2Finish() {
        //摧毁山峰
        BlockSet builder = new BlockSet((ServerLevel) this.level());
        builder.doFill(mountain1,new BlockPos(mountain1.getX()+16,mountain1.getY()+25,mountain1.getZ()+18), Blocks.AIR);
        builder.doFill(mountain2,new BlockPos(mountain2.getX()+16,mountain2.getY()+25,mountain2.getZ()+18), Blocks.AIR);
        builder.doFill(mountain3,new BlockPos(mountain3.getX()+16,mountain3.getY()+25,mountain3.getZ()+18), Blocks.AIR);
        builder.doFill(mountain4,new BlockPos(mountain4.getX()+16,mountain4.getY()+25,mountain4.getZ()+18), Blocks.AIR);
        this.level().playSound(null, this.getX(),this.getY(),this.getZ(),
                SoundEvents.STONE_BREAK, SoundSource.NEUTRAL, 5.0f,1.0f);
        //关闭天气
        weatherManager.stop();

        if (this.level() instanceof ServerLevel sl) {
            runCommand(sl, "execute as @a[distance=..80] run dialog show boss_pangu_skill_end");
        }
    }

    private void SkillPhase2AttackStart() {
        //顶飞玩家
        for (Player player : this.level().players()) {
            if (player.distanceToSqr(this) > 80 * 80) continue;

            if(mountain1Box.contains(player.position())
                    || mountain2Box.contains(player.position())
                    || mountain3Box.contains(player.position())
                    || mountain4Box.contains(player.position())) {
                player.teleportTo(player.getX(),player.getY()+25,player.getZ());
                player.setDeltaMovement(new Vec3((this.random.nextDouble() - 0.5) * 0.5, 1.0, (this.random.nextDouble() - 0.5) * 0.5));
                player.hurtMarked = true;
                player.hurt(this.level().damageSources().mobAttack(this), 45.0F);
            }
        }

        //放置山峰
        BlockSet builder = new BlockSet((ServerLevel) this.level());
        builder.doClone(srcFrom,srcTo,mountain1);
        builder.doClone(srcFrom,srcTo,mountain2);
        builder.doClone(srcFrom,srcTo,mountain3);
        builder.doClone(srcFrom,srcTo,mountain4);
        this.level().playSound(null, this.getX(),this.getY(),this.getZ(),
                SoundEvents.MINECART_RIDING, SoundSource.NEUTRAL, 5.0f,1.0f);

        SkillPhase2Attack();
        runCommand((ServerLevel) this.level(),"");
    }

    private void SkillPhase2Attack() {
        ClientModEvents.startShake(this.position(),80,5, 2.0f);

        Vec3 leftWaveCenter = this.spawnPos.add(10.0D, 0.0D, -45.0D);
        Vec3 rightWaveCenter = this.spawnPos.add(10.0D, 0.0D, 45.0D);

        //裂地效果
        GroundSmashRenderer.triggerSmash(leftWaveCenter, 8, 30);
        GroundSmashRenderer.triggerSmash(rightWaveCenter, 8, 30);

        // 塞入激活的震动波列表中（Shockwave 类保持上一版的定义不变）
        this.activeShockwaves.add(new Shockwave(leftWaveCenter,100));
        this.activeShockwaves.add(new Shockwave(rightWaveCenter,100));

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.EXPLOSION_EMITTER,
                    leftWaveCenter.x(),leftWaveCenter.y(),leftWaveCenter.z(),
                    5,
                    1D, 1D, 1D,
                    0D
            );
            serverLevel.sendParticles(
                    ParticleTypes.EXPLOSION_EMITTER,
                    rightWaveCenter.x(),rightWaveCenter.y(),rightWaveCenter.z(),
                    5,
                    1D, 1D, 1D,
                    0D
            );
        }
        this.level().playSound(null, leftWaveCenter.x(),leftWaveCenter.y(),leftWaveCenter.z(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 5.0f,1.0f);
        this.level().playSound(null, rightWaveCenter.x(),rightWaveCenter.y(),rightWaveCenter.z(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 5.0f,1.0f);
    }

    private void SkillPhase2Lighting() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        // 1. 获取周围半径 80 格内的所有玩家
        AABB searchBox = this.getBoundingBox().inflate(80.0);
        List<ServerPlayer> players = serverLevel.getEntitiesOfClass(ServerPlayer.class, searchBox);

        for (ServerPlayer sp : players) {
            // 记录玩家当前的位置
            final Vec3 targetPos = sp.position();

            // 主雷：在 0.5 秒（10 ticks）后落下
            GameBusEvents.queueTask(10, () -> {
                if (!serverLevel.getServer().isRunning() || sp.hasDisconnected() || !sp.isAlive()) return;

                // 触发主雷粒子
                PacketDistributor.sendToPlayersTrackingEntity(
                        this,
                        new ParticleLighting(
                                targetPos
                        ));

                // 检测主雷伤害
                if (sp.position().distanceToSqr(targetPos) <= 4.0) {
                    sp.hurt(serverLevel.damageSources().lightningBolt(), 15.0f);
                }

                // 原地生成第一个滚地雷
                spawnGundilei(serverLevel, targetPos);
            });

            // 5道扩散雷
            for (int i = 0; i < 3; i++) {
                final int index = i;
                int staggeredDelay = 10 + 2 + (i * 3); // 阶梯式延迟

                // 极坐标均匀散开
                double angle = (index * 72.0 + random.nextInt(15)) * Math.PI / 180.0;
                double distance = 4.0 + random.nextDouble() * 4.0; // 离中心 4~8 格

                double offsetX = Math.cos(angle) * distance;
                double offsetZ = Math.sin(angle) * distance;

                GameBusEvents.queueTask(staggeredDelay, () -> {
                    if (!serverLevel.getServer().isRunning() || sp.hasDisconnected() || !sp.isAlive()) return;

                    // 精准锁定目标 XZ
                    double targetX = targetPos.x + offsetX;
                    double targetZ = targetPos.z + offsetZ;

                    // 【核心修改】智能地面探测：不使用全局Heightmap，而是围绕玩家的 Y 轴上下寻找落脚点
                    double safeY = targetPos.y;
                    BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(targetX, targetPos.y + 2, targetZ);

                    // 从玩家头顶 2 格往下探测 5 格，寻找第一个非空气方块作为地面
                    for (int dy = 0; dy < 6; dy++) {
                        if (!serverLevel.isEmptyBlock(mutablePos) && serverLevel.isEmptyBlock(mutablePos.above())) {
                            safeY = mutablePos.getY() + 1;
                            break;
                        }
                        mutablePos.move(0, -1, 0);
                    }

                    Vec3 spreadPos = new Vec3(targetX, safeY, targetZ);

                    // 发送粒子效果
                    PacketDistributor.sendToPlayersTrackingEntity(
                            this,
                            new ParticleLighting(
                                    spreadPos
                            ));

                    // 检测扩散雷伤害
                    if (sp.position().distanceToSqr(spreadPos) <= 4.0) {
                        sp.hurt(serverLevel.damageSources().lightningBolt(), 15.0f);
                    }

                    // 生成苦力怕
                    spawnGundilei(serverLevel, spreadPos);
                });
            }
        }
    }

    private void SkillEnd() {
        this.setAttacking(false);
        this.endAttack();
        this.startAttackCooldown();
        this.setNoAi(false);
        this.setInvulnerable(false);
    }

    private void DamageFinish() {
        this.LastAttackIsCommon = true;
        this.cooldownStartedThisAttack = true;
    }

    private void AttackFinish() {
        this.LastAttackIsCommon = false;
        this.cooldownStartedThisAttack = true;
    }

    private void ActionDied() {
        this.diedAnimTicks = 0;
        this.diedAnimPlaying = false;
        this.remove(RemovalReason.KILLED);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.WHITE_SMOKE,
                    this.getX(),this.getY(),this.getZ(),
                    50,
                    0.5D, 0.5D, 0.5D,
                    0D
            );
            serverLevel.sendParticles(
                    ParticleTypes.SMOKE,
                    this.getX(),this.getY(),this.getZ(),
                    50,
                    0.5D, 0.5D, 0.5D,
                    0D
            );
        }
    }

    private void runCommand(ServerLevel level, String command) {
        CommandSourceStack source = new CommandSourceStack(
                CommandSource.NULL,
                this.position(),
                this.getRotationVector(),
                level,
                4,
                this.getName().getString(),
                this.getDisplayName(),
                level.getServer(),
                this
        );
        level.getServer().getCommands().performPrefixedCommand(source, command);
    }

    //滚地雷（闪电苦力怕）生成
    private void spawnGundilei(ServerLevel level, Vec3 pos) {
        Creeper creeper = EntityType.CREEPER.create(level);
        if (creeper != null) {
            creeper.moveTo(pos.x, pos.y, pos.z, level.getRandom().nextFloat() * 360.0F, 0.0F);

            // 闪电苦力怕状态
            CompoundTag nbt = new CompoundTag();
            nbt.putBoolean("powered", true);
            creeper.readAdditionalSaveData(nbt);

            creeper.setNoAi(true);             // 无 AI
            creeper.setInvulnerable(true);     // 无敌
            creeper.setCustomName(Component.translatable("entity.panlingre.pan_gu.creeper"));
            creeper.setCustomNameVisible(true);

            // 写入自定义的 NBT Tag 用于记录生存时间
            creeper.getPersistentData().putInt("GundileiTicks", 20);

            level.addFreshEntity(creeper);
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}