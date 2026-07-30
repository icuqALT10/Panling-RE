package icu.icuqalt10.panlingre.entity.boss;

import icu.icuqalt10.panlingre.entity.PanLingEntities;
import icu.icuqalt10.panlingre.network.GroundSmashPayload;
import icu.icuqalt10.panlingre.network.ShakePayload;
import icu.icuqalt10.panlingre.entity.FireTornadoEntity;
import icu.icuqalt10.panlingre.entity.FireTrailTracker;
import icu.icuqalt10.panlingre.entity.boss.PanGu.*;
import icu.icuqalt10.panlingre.event.GameBusEvents;
import icu.icuqalt10.panlingre.init.ModEffects;
import icu.icuqalt10.panlingre.init.ModEntities;
import icu.icuqalt10.panlingre.network.particle.GatherBall;
import icu.icuqalt10.panlingre.network.particle.ParticleCluster;
import icu.icuqalt10.panlingre.network.particle.ParticleLighting;
import icu.icuqalt10.panlingre.util.BlockSet;
import icu.icuqalt10.panlingre.util.LocalWeatherManager;
import icu.icuqalt10.panlingre.util.Shockwave;
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
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import java.util.List;

public class PanGuEntity extends Monster implements GeoEntity, PanLingEntities {

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

    // ===== 服务端动画计时器系统 =====
    private String currentAnimation = "";
    private int animationTick = 0;

    // ===== 状态机字段 =====
    public enum ActionState { INTRO, IDLE_OR_WALK, ATTACKING,ATTACK_COOLDOWN, SKILL, DYING,FROZEN }
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
    private final BlockPos srcFrom = new BlockPos(3022,129,-2351);
    private final BlockPos srcTo = new BlockPos(3037 ,150,-2334);
    private final BlockPos mountain1 = new BlockPos(3071 ,129,-2254);
    private final BlockPos mountain2 = new BlockPos(3104 ,129,-2215);
    private final BlockPos mountain3 = new BlockPos(3102 ,129,-2279);
    private final BlockPos mountain4 = new BlockPos(3153 ,129,-2250);
    private final AABB mountain1Box = new AABB(new Vec3(mountain1.getX(),mountain1.getY(),mountain1.getZ()),
            new Vec3(mountain1.getX()+16,mountain1.getY()+25,mountain1.getZ()+18));
    private final AABB mountain2Box = new AABB(new Vec3(mountain2.getX(),mountain2.getY(),mountain2.getZ()),
            new Vec3(mountain2.getX()+16,mountain2.getY()+25,mountain2.getZ()+18));
    private final AABB mountain3Box = new AABB(new Vec3(mountain3.getX(),mountain3.getY(),mountain3.getZ()),
            new Vec3(mountain3.getX()+16,mountain3.getY()+25,mountain3.getZ()+18));
    private final AABB mountain4Box = new AABB(new Vec3(mountain4.getX(),mountain4.getY(),mountain4.getZ()),
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

            if (this.firstSpawnInitialized) {
                this.firstSpawnInitialized = false;
                //记录初始坐标
                this.spawnPos = this.position();
            }

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
            resetSkillPhase1();
            resetSkillPhase2();

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
                    if (frozenTicks >= 140 && this.tickCount % 20 == 0) {
                        player.hurt(this.damageSources().freeze(), player.getMaxHealth()*0.3f);
                    }
                    //增加冰冻值
                    else if (FireTrailTracker.isEntityInTrail(player)) {
                        player.setTicksFrozen(frozenTicks-3);
                    } else {
                        player.setTicksFrozen(frozenTicks+5);}
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
                startAnimation("attack.throw.catch");
            }

            // 移动窗口用完还没撞到→停下,等throw.end播放miss收尾
            if (dashMoving && dashTicksLeft <= 0) {
                dashMoving = false;
                // 没命中,throw.end动画会在关键帧自然触发doThrowEnd()播放miss
            }
        }
    }

    //移除时
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        if (!this.level().isClientSide()) {
            //清理渲染
            weatherManager.cleanup();
            this.bossEvent.removeAllPlayers();
        }
        return true;
    }

    // 实体被移除（如自然刷掉、代码强制移除、死亡动画播完后）时
    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide()) {
            //清理渲染
            weatherManager.cleanup();
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
            weatherManager.cleanup();
            this.bossEvent.removeAllPlayers();
        }
    }

    // ===== 攻击命中后调用 =====
    public boolean LastAttackIsCommon = false;
    public boolean cooldownStartedThisAttack = false;

    public void startAttackCooldown() {
        this.attackCooldown = 30;
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
            this.setNoAi(true);
            this.setInvulnerable(true);

            startAnimation("died");
        }
    }

    // ==== 一阶段技能 ====
    private void SkillPhase1Run() {
        this.SkillPhase1Triggered = true;
        //解冻
        this.removeEffect(ModEffects.freeze);
        //设置属性
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

        startAnimation("skill.phase1");
        this.setActionState(ActionState.SKILL);
        this.setNoAi(true);

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 5.0f,1.0f);
    }

    // ==== 二阶段技能 ====
    private void SkillPhase2Run() {
        this.SkillPhase2Triggered = true;
        //解冻
        this.removeEffect(ModEffects.freeze);
        //设置属性
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

        startAnimation("skill.phase2");
        this.setActionState(ActionState.SKILL);
        this.setNoAi(true);

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 5.0f,1.0f);
    }

    // ===== GeckoLib动画 =====
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "body_controller", 5, this::bodyPredicate));

        controllers.add(new AnimationController<>(this, "action_controller", 0, this::attackPredicate)
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
                .triggerableAnim("intro", RawAnimation.begin().thenPlay("intro"))
                .triggerableAnim("died", RawAnimation.begin().thenPlay("died"))
                .triggerableAnim("skill.phase1", RawAnimation.begin().thenPlay("skill.phase1"))
                .triggerableAnim("skill.phase2", RawAnimation.begin().thenPlay("skill.phase2")));
    }

    private PlayState bodyPredicate(AnimationState<PanGuEntity> event) {
        switch (getActionState()) {
            case INTRO, DYING, SKILL -> {
                return PlayState.STOP;
            }
            case FROZEN -> {
                event.getController().setAnimationSpeed(0.001D);
                return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
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
                    }

                    event.getController().setAnimationSpeed(0.75D);
                    return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
                }

                event.getController().setAnimationSpeed(1.0D);
                return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            }
        }
    }

    private PlayState attackPredicate(AnimationState<PanGuEntity> event) {
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
        this.dashMoving = false;
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
        this.endAttack();
        this.startAttackCooldown();
    }

    // ===== 服务端动画计时器 =====
    private void tickAnimation(String animName, int tick) {
        switch (animName) {
            case "intro" -> {
                if (tick == 172) {
                    this.setNoAi(false);
                    this.setInvulnerable(false);
                    this.setActionState(ActionState.IDLE_OR_WALK);
                }
                else if (this.level() instanceof ServerLevel sl) {
                    switch (tick) {
                        case 20 -> runCommand(sl, "execute as @a[distance=..80] run dialog show boss_gangu_intro_1");
                        case 80 -> runCommand(sl, "execute as @a[distance=..80] run dialog show boss_gangu_intro_2");
                        case 122 -> {
                            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                    SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 5f, 1.0f);

                            AABB searchBox = AABB.ofSize(this.position(), 160, 160, 160);
                            List<ServerPlayer> players = sl.getEntitiesOfClass(ServerPlayer.class, searchBox);
                            for (ServerPlayer sp : players) {
                                sl.sendParticles(
                                        sp,
                                        ParticleTypes.WHITE_SMOKE,
                                        true,
                                        this.position().x, this.position().y + 1, this.position().z,
                                        100,
                                        0.5, 0.5, 0.5,
                                        0.1
                                );
                            }
                        }
                        case 152 -> runCommand(sl, "execute as @a[distance=..80] run dialog show boss_gangu_intro_3");
                    }
                }
            }
            case "attack.heavy" -> {
                switch (tick) {
                    case 2,7 -> doTeleportToTarget();
                    case 15 -> {
                        doDamage(20.0f, AttackKind.MELEE);
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 0.5f,1.0f);
                    }
                    case 30 -> DamageFinish();
                }
            }
            case "attack.heavy2" -> {
                switch (tick) {
                    case 6 -> this.setInvulnerable(true);
                    case 18 -> DamageHeavy2();
                    case 25 -> AttackWeakness();
                    case 45 -> AttackFinish();
                }
            }
            case "attack.skill1" -> {
                switch (tick) {
                    case 6 -> this.setInvulnerable(true);
                    case 18 -> DamageSkill1();
                    case 25 -> AttackWeakness();
                    case 45 -> AttackFinish();
                }
            }
            case "attack.skill2" -> {
                switch (tick) {
                    case 6 -> this.setInvulnerable(true);
                    case 18 -> DamageSkill2();
                    case 25 -> AttackWeakness();
                    case 45 -> AttackFinish();
                }
            }
            case "attack.skill3" -> {
                switch (tick) {
                    case 6 -> this.setInvulnerable(true);
                    case 18 -> DamageSkill3();
                    case 25 -> AttackWeakness();
                    case 45 -> AttackFinish();
                }
            }
            case "attack.skill4" -> {
                switch (tick) {
                    case 6 -> this.setInvulnerable(true);
                    case 18 -> DamageSkill4();
                    case 25 -> AttackWeakness();
                    case 45 -> AttackFinish();
                }
            }
            case "attack.combo" -> {
                switch (tick) {
                    case 2,12,22 -> doTeleportToTarget();
                    case 5,15,25 -> {
                        doDamage(5.0f, AttackKind.MELEE);
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.5f,1.0f);
                    }
                    case 35 -> DamageFinish();
                }
            }
            case "attack.throw" -> {
                switch (tick) {
                    case 7 -> doThrowStart();
                    case 17 -> doThrowEnd();
                }
            }
            case "attack.throw.catch" -> {
                switch (tick) {
                    case 0 -> doCatchStart();
                    case 8 -> {
                        doDamage(20.0f, AttackKind.MELEE);
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.5f,1.0f);
                    }
                    case 12 -> doCatchEnd();
                    case 19 -> DamageFinish();
                }
            }
            case "attack.throw.end" -> {
                if (tick == 15) DamageFinish();
            }
            case "died" -> {
                if (tick == 56) ActionDied();
            }
            case "skill.phase1" -> {
                switch (tick) {
                    case 80,83,86,89,92,95,98,101,104,107,110 -> SkillPhase1Cold();
                    case 121 -> SkillPhase1Weather();
                    case 145 -> this.is_snowing = true;
                    case 163,223,283,343,403,463 -> Skill1Phase1Fire();
                    case 535 -> SkillPhase1Finish();
                    case 555 -> this.setInvulnerable(false);
                    case 618 -> SkillEnd();
                    default -> {
                        if (this.level() instanceof ServerLevel sl) {
                            switch (tick)
                            {
                                case 0 -> runCommand(sl, "execute as @a[distance=..80] run dialog show boss_pangu_skill_phase1_1");
                                case 10 -> {
                                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                            SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 5f, 1.0f);

                                    AABB searchBox = AABB.ofSize(this.position(), 160, 160, 160);
                                    List<ServerPlayer> players = sl.getEntitiesOfClass(ServerPlayer.class, searchBox);
                                    for (ServerPlayer sp : players) {
                                        sl.sendParticles(
                                                sp,
                                                ParticleTypes.WHITE_SMOKE,
                                                true,
                                                this.position().x, this.position().y + 1, this.position().z,
                                                50,
                                                0.5, 0.5, 0.5,
                                                0.1
                                        );
                                    }
                                }
                                case 30 -> {
                                    runCommand(sl, "execute as @a[distance=..80] run dialog show boss_pangu_skill_phase1_2");

                                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                            SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 5f, 1.0f);

                                    AABB searchBox = AABB.ofSize(this.position(), 160, 160, 160);
                                    List<ServerPlayer> players = sl.getEntitiesOfClass(ServerPlayer.class, searchBox);
                                    for (ServerPlayer sp : players) {
                                        sl.sendParticles(
                                                sp,
                                                ParticleTypes.WHITE_SMOKE,
                                                true,
                                                this.position().x, this.position().y + 12, this.position().z,
                                                3000,
                                                30, 30, 30,
                                                0.5
                                        );
                                        sl.sendParticles(
                                                sp,
                                                ParticleTypes.SMOKE,
                                                true,
                                                this.position().x, this.position().y + 12, this.position().z,
                                                3000,
                                                30, 30, 30,
                                                0.5
                                        );
                                    }
                                }
                            }
                        }
                    }
                }
            }
            case "skill.phase2" -> {
                switch (tick) {
                    case 70 -> SkillPhase2Weather();
                    case 90 -> SkillPhase2AttackStart();
                    case 150,210,270,330,390,450 -> SkillPhase2Attack();
                    case 120,160,200,240,280,320,360,400,440,480 -> SkillPhase2Lighting();
                    case 520 -> SkillPhase2Finish();
                    case 530 -> this.setInvulnerable(false);
                    case 593 -> SkillEnd();
                    default -> {
                        if (this.level() instanceof ServerLevel sl) {
                            switch (tick)
                            {
                                case 0 -> runCommand(sl, "execute as @a[distance=..80] run dialog show boss_pangu_skill_phase2_1");
                                case 10 -> {
                                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                            SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 5f, 1.0f);

                                    AABB searchBox = AABB.ofSize(this.position(), 160, 160, 160);
                                    List<ServerPlayer> players = sl.getEntitiesOfClass(ServerPlayer.class, searchBox);
                                    for (ServerPlayer sp : players) {
                                        sl.sendParticles(
                                                sp,
                                                ParticleTypes.WHITE_SMOKE,
                                                true,
                                                this.position().x, this.position().y + 1, this.position().z,
                                                50,
                                                0.5, 0.5, 0.5,
                                                0.1
                                        );
                                    }
                                }
                                case 30 -> {
                                    runCommand(sl, "execute as @a[distance=..80] run dialog show boss_pangu_skill_phase2_2");

                                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                            SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 5f, 1.0f);

                                    AABB searchBox = AABB.ofSize(this.position(), 160, 160, 160);
                                    List<ServerPlayer> players = sl.getEntitiesOfClass(ServerPlayer.class, searchBox);
                                    for (ServerPlayer sp : players) {
                                        sl.sendParticles(
                                                sp,
                                                ParticleTypes.WHITE_SMOKE,
                                                true,
                                                this.position().x, this.position().y + 12, this.position().z,
                                                3000,
                                                30, 30, 30,
                                                0.5
                                        );
                                        sl.sendParticles(
                                                sp,
                                                ParticleTypes.SMOKE,
                                                true,
                                                this.position().x, this.position().y + 12, this.position().z,
                                                3000,
                                                30, 30, 30,
                                                0.5
                                        );
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ===== 保留原有的所有方法供计时器调用 =====

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
        if (this.getTeam() != null && target.getTeam() == this.getTeam()) return;
        if (this.distanceTo(target) < 3.0) {
            target.hurt(this.damageSources().mobAttack(this), amount);
        }
    }
    private void doDamage(Vec3 Pos,float amount, AttackKind kind,float rad) {
        LivingEntity target = this.getAttackTarget();
        if (target == null) return;
        if (this.getTeam() != null && target.getTeam() == this.getTeam()) return;
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
            startAnimation("attack.throw.end");
        }
    }

    private void doCatchStart() {
        doDamage(10.0f, AttackKind.DASH);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.HOSTILE, 0.5f,1.0f);
        LivingEntity target = this.getAttackTarget();
        if (!(target instanceof Player player)) return;
        lockedPlayer = player;

        player.addEffect(new MobEffectInstance(ModEffects.freeze, 20, 0));
    }

    private void doCatchEnd() {
        if (lockedPlayer == null) return;

        lockedPlayer.removeEffect(ModEffects.freeze);

        lockedPlayer = null;
    }

    private void AttackWeakness() {
        this.setInvulnerable(false);
        this.addEffect(new MobEffectInstance(ModEffects.po_jia, 20, 4));
    }

    private void DamageHeavy2() {
        PacketDistributor.sendToPlayersTrackingEntity(this, new ShakePayload(this.position(), 80, 5, 1.5f));

        Vec3 position = this.position();
        Vec3 lookVec = this.getLookAngle();
        Vec3 targetPos = position.add(lookVec.scale(13));;
        doDamage(targetPos,25.0f, AttackKind.MELEE,5);
        this.level().playSound(null, targetPos.x,targetPos.y,targetPos.z,
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.NEUTRAL, 5f,1.0f);
        PacketDistributor.sendToPlayersTrackingEntity(this, new GroundSmashPayload(targetPos, 4, 10));
    }

    private void DamageSkill1() {
        PacketDistributor.sendToPlayersTrackingEntity(this, new ShakePayload(this.position(), 80, 3, 1.5f));

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

        List<LivingEntity> targetEntities = SkillHelper.getLivingEntitiesInFront(this, 10.0, 10.0, 40.0);
        for (LivingEntity entity : targetEntities) {
            if (this.getTeam() != null && this.getTeam() != entity.getTeam()) {
                entity.addEffect(new MobEffectInstance(ModEffects.freeze, 100, 0));
                entity.setTicksFrozen(140);
            }
        }

        this.level().playSound(null, this.position().x,this.position().y,this.position().z,
                SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 5f,1.0f);

        if (this.level() instanceof ServerLevel sl) {
            runCommand(sl, "execute as @a[distance=..80] run dialog show boss_pangu_skill_1");
        }
    }

    private void DamageSkill2() {
        PacketDistributor.sendToPlayersTrackingEntity(this, new ShakePayload(this.position(), 80, 5, 1.5f));
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        PlayerTeam team = this.getTeam();

        Vec3 pos = this.position();                    // 实体绝对坐标
        Vec3 forward = this.getLookAngle();            // 单位前向量（含俯仰）
        Vec3 up = new Vec3(0, 1, 0);                    // 世界 Y 轴
        Vec3 left = up.cross(forward).normalize();      // 单位左向量（水平方向，与俯仰无关）

        Vec3 midStart = pos.add(forward);               // 实体前方 1 格
        Vec3 leftStart = pos.add(left.scale(-8)).add(forward);  // 左 5，前 1
        Vec3 rightStart = pos.add(left.scale(8)).add(forward);  // 右 5，前 1

        Vec3 midTarget = pos.add(forward.scale(31));                // 中间笔直 30 格
        Vec3 leftTarget = pos.add(left.scale(8)).add(forward.scale(29));   // 左起点 → 右前方
        Vec3 rightTarget = pos.add(left.scale(-8)).add(forward.scale(29)); // 右起点 → 左前方

        //生成3个火龙卷
        FireTornadoEntity tornado = new FireTornadoEntity(ModEntities.FIRE_TORNADO.get(), serverLevel,
                midStart, midTarget, 20, 15, team);
        serverLevel.addFreshEntity(tornado);

        FireTornadoEntity tornadoL = new FireTornadoEntity(ModEntities.FIRE_TORNADO.get(), serverLevel,
                leftStart, leftTarget, 20, 15, team);
        serverLevel.addFreshEntity(tornadoL);

        FireTornadoEntity tornadoR = new FireTornadoEntity(ModEntities.FIRE_TORNADO.get(), serverLevel,
                rightStart, rightTarget, 20, 15, team);
        serverLevel.addFreshEntity(tornadoR);

        this.level().playSound(null, this.position().x,this.position().y,this.position().z,
                SoundEvents.BREEZE_SHOOT, SoundSource.NEUTRAL, 5f,1.0f);

        if (this.level() instanceof ServerLevel sl) {
            runCommand(sl, "execute as @a[distance=..80] run dialog show boss_pangu_skill_2");
        }
    }

    private void DamageSkill3() {
        PacketDistributor.sendToPlayersTrackingEntity(this, new ShakePayload(this.position(), 80, 3, 1.5f));

        SkillPhase2Lighting();

        if (this.level() instanceof ServerLevel sl) {
            runCommand(sl, "execute as @a[distance=..80] run dialog show boss_pangu_skill_3");
        }
    }

    private void DamageSkill4() {
        PacketDistributor.sendToPlayersTrackingEntity(this, new ShakePayload(this.position(), 80, 5, 1.5f));
        Vec3 position = this.position();
        Vec3 lookVec = this.getLookAngle();
        Vec3 targetPos = position.add(lookVec.scale(3.5));;
        doDamage(targetPos,15.0f, AttackKind.MELEE,3.5f);
        this.level().playSound(null, targetPos.x,targetPos.y,targetPos.z,
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.NEUTRAL, 5f,1.0f);

        PacketDistributor.sendToPlayersTrackingEntity(this, new GroundSmashPayload(targetPos, 4, 15));
        GameBusEvents.addShockwave(this, new Shockwave(targetPos,3,15,this.getTeam()));

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

        PlayerTeam team = this.getTeam();

        //固定4个
        List<Vec3> tornadoSetPoses = List.of(this.spawnPos.add(30.0D, 0.0D, 0.0D),this.spawnPos.add(-30.0D, 0.0D, 0.0D),this.spawnPos.add(0.0D, 0.0D, -30.0D),this.spawnPos.add(0.0D, 0.0D, 30.0D));
        List<Vec3> tornadoTargetPoses = List.of(this.spawnPos.add(0.0D, 0.0D, -30.0D),this.spawnPos.add(0.0D, 0.0D, 30.0D),this.spawnPos.add(-30.0D, 0.0D, 0.0D),this.spawnPos.add(30.0D, 0.0D, 0.0D));

        for (int i = 0;i <= 3 ; i++) {
            Vec3 tornadoSetPos = tornadoSetPoses.get(i);
            Vec3 tornadoTargetPos = tornadoTargetPoses.get(i);
            // 生成火龙卷
            FireTornadoEntity tornado = new FireTornadoEntity(ModEntities.FIRE_TORNADO.get(), serverLevel,
                    tornadoSetPos, tornadoTargetPos, 30, 15, team);
            serverLevel.addFreshEntity(tornado);
        }

        // 获取周围半径 80 格内的所有玩家
        AABB searchBox = this.getBoundingBox().inflate(80.0);
        List<ServerPlayer> players = serverLevel.getEntitiesOfClass(ServerPlayer.class, searchBox);

        for (ServerPlayer player : players) {
            // 在周围生成额外的火龙卷
            for (int i = 1; i <= 5; i++) {
                GameBusEvents.queueTask(5 * i, () -> {
                    if (!serverLevel.getServer().isRunning() || player.hasDisconnected() || !player.isAlive()) return;

                    double waveAngle = random.nextDouble() * 2 * Math.PI;
                    double waveDistance = 10.0 + random.nextDouble() * 5.0; // 10~15 格距离
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
                    double waveTravelDistance = 20.0 + random.nextDouble() * 5.0; // 20~25 格
                    Vec3 waveTargetPos = waveSpawnPos.add(randomDirection.scale(waveTravelDistance));

                    // 生成火龙卷
                    FireTornadoEntity waveTornado = new FireTornadoEntity(ModEntities.FIRE_TORNADO.get(), serverLevel,
                            waveSpawnPos, waveTargetPos, 30, 15, team);
                    serverLevel.addFreshEntity(waveTornado);
                });
            }
        }
    }

    private void SkillPhase1Finish() {
        resetSkillPhase1();

        if (this.level() instanceof ServerLevel sl) {
            runCommand(sl, "execute as @a[distance=..80] run dialog show boss_pangu_skill_end");
        }
    }

    private void resetSkillPhase1() {
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
    }

    private void SkillPhase2Weather() {
        weatherManager.setWeather(LocalWeatherManager.WeatherType.THUNDER, 80.0);
        this.level().playSound(null, this.getX(),this.getY(),this.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.NEUTRAL, 5.0f,1.0f);
    }

    private void SkillPhase2Finish() {
        resetSkillPhase2();

        if (this.level() instanceof ServerLevel sl) {
            runCommand(sl, "execute as @a[distance=..80] run dialog show boss_pangu_skill_end");
        }
    }

    private void resetSkillPhase2() {
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
        PacketDistributor.sendToPlayersTrackingEntity(this, new ShakePayload(this.position(), 80, 5, 2.0f));

        Vec3 WaveCenter = this.spawnPos.add(22.0D, 0.0D, 0.0D);

        //攻击
        doDamage(WaveCenter,30.0f, AttackKind.MELEE,12);

        //裂地效果
        PacketDistributor.sendToPlayersTrackingEntity(this, new GroundSmashPayload(WaveCenter, 16, 30));

        // 塞入激活的震动波列表中（Shockwave 类保持上一版的定义不变）
        GameBusEvents.addShockwave(this, new Shockwave(WaveCenter,100,15,this.getTeam()));

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.EXPLOSION_EMITTER,
                    WaveCenter.x(),WaveCenter.y(),WaveCenter.z(),
                    5,
                    1D, 1D, 1D,
                    0D
            );
        }
        this.level().playSound(null, WaveCenter.x(),WaveCenter.y(),WaveCenter.z(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 5.0f,1.0f);
    }

    private void SkillPhase2Lighting() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        // 1. 获取周围半径 80 格内的所有玩家
        AABB searchBox = this.getBoundingBox().inflate(80.0);
        List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox);

        for (LivingEntity entity : entities) {
            if (this.getTeam() != null && this.getTeam() != entity.getTeam()) {
                // 记录玩家当前的位置
                final Vec3 targetPos = entity.position();

                // 主雷：在 0.5 秒（10 ticks）后落下
                GameBusEvents.queueTask(10, () -> {
                    if (!serverLevel.getServer().isRunning() || !entity.isAlive()) return;

                    // 触发主雷粒子
                    PacketDistributor.sendToPlayersTrackingEntity(
                            this,
                            new ParticleLighting(
                                    targetPos
                            ));

                    // 检测主雷伤害
                    if (entity.position().distanceToSqr(targetPos) <= 4.0) {
                        entity.hurt(serverLevel.damageSources().lightningBolt(), 15.0f);
                    }

                    // 原地生成第一个滚地雷
                    spawnGundilei(serverLevel, targetPos);
                });

                // 3道扩散雷
                for (int i = 0; i < 3; i++) {
                    final int index = i;
                    int staggeredDelay = 10 + 2 + (i * 3); // 阶梯式延迟

                    // 极坐标均匀散开
                    double angle = (index * 72.0 + random.nextInt(15)) * Math.PI / 180.0;
                    double distance = 4.0 + random.nextDouble() * 4.0; // 离中心 4~8 格

                    double offsetX = Math.cos(angle) * distance;
                    double offsetZ = Math.sin(angle) * distance;

                    GameBusEvents.queueTask(staggeredDelay, () -> {
                        if (!serverLevel.getServer().isRunning() || !entity.isAlive()) return;

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
                        if (entity.position().distanceToSqr(spreadPos) <= 4.0) {
                            entity.hurt(serverLevel.damageSources().lightningBolt(), 15.0f);
                        }

                        // 生成苦力怕
                        spawnGundilei(serverLevel, spreadPos);
                    });
                }
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

        this.remove(RemovalReason.KILLED);
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
            //写入伤害
            creeper.getPersistentData().putFloat("GundileiDamage", 10);

            //加入队伍
            if (this.getTeam() != null) {
                creeper.level().getScoreboard()
                        .addPlayerToTeam(creeper.getStringUUID(), this.getTeam());
            }

            level.addFreshEntity(creeper);
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
