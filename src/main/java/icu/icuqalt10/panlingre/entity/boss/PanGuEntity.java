package icu.icuqalt10.panlingre.entity.boss;

import icu.icuqalt10.panlingre.entity.boss.PanGu.ApproachTargetGoal;
import icu.icuqalt10.panlingre.entity.boss.PanGu.DashAttackGoal;
import icu.icuqalt10.panlingre.entity.boss.PanGu.MeleeComboGoal;
import icu.icuqalt10.panlingre.entity.boss.PanGu.PostAttackBehaviorGoal;
import icu.icuqalt10.panlingre.network.AttackInstructionPayload;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.keyframe.event.CustomInstructionKeyframeEvent;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

public class PanGuEntity extends Monster implements GeoEntity {

    // ===== BossBar 设置 =====
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            this.getDisplayName(),
            BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.NOTCHED_20
    ).setDarkenScreen(false)
            .setCreateWorldFog(false);

    // ==== 攻击多人去重 ====
    private final java.util.Set<String> executedKeys = new java.util.HashSet<>(); // 本次攻击已执行的关键帧

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
            ResourceLocation.fromNamespaceAndPath("panlingre", "catch_lock");

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
    public boolean didDashHit() { return dashHasHit; }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ===== 状态机字段 =====
    public enum ActionState { INTRO, IDLE_OR_WALK, ATTACKING, SKILL, DYING }
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

    private int attackCooldown = 0;       // 攻击后冷却,期间走"盯着+乱走"逻辑
    private boolean diedAnimPlaying = false;
    private int diedAnimTicks = 58;       // died动画2.875s ≈ 58tick

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
                .add(Attributes.ARMOR, 100)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false; // 返回 false 表示无论玩家离得多远，该实体都不会被系统自动清除
    }

    // ===== 头部转动限制在[-90,90] =====
    @Override
    public int getMaxHeadYRot() {
        return 90;
    }

    private int introTicks = 115;          // 出生时倒数
    private boolean firstSpawnInitialized = false;

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        // 将状态写入 NBT 存档，这样区块卸载、服务器重启都能存下来
        compound.putBoolean("FirstSpawnInitialized", this.firstSpawnInitialized);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        // 当区块重新加载、读取 NBT 时，把之前存的状态读出来
        if (compound.contains("FirstSpawnInitialized")) {
            this.firstSpawnInitialized = compound.getBoolean("FirstSpawnInitialized");
        }
    }
    // ===== 生成时:禁止AI,播intro,倒数结束才正式开始战斗 =====
    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!this.level().isClientSide && !this.firstSpawnInitialized) {
            this.firstSpawnInitialized = true;
            this.setActionState(ActionState.INTRO);
            this.setNoAi(true);          // intro期间完全不跑goal逻辑
            this.setInvulnerable(true);  // intro期间无敌,避免被打断
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new PostAttackBehaviorGoal(this));
        this.goalSelector.addGoal(1, new DashAttackGoal(this));
        this.goalSelector.addGoal(1, new MeleeComboGoal(this));
        this.goalSelector.addGoal(2, new ApproachTargetGoal(this));

        this.targetSelectorGoals();
    }

    private void targetSelectorGoals() {
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
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
                this.startAttackAnim("attack.throw.catch"); // 命中→播抓取动画(catch动画里的damage.catch会造成30伤害)
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
            case ATTACKING -> {}
            default -> { if (attackCooldown > 0) attackCooldown--; }
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

    // 实体被移除（如自然刷掉、代码强制移除、死亡动画播完后）时，清理所有玩家的 BossBar
    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        this.bossEvent.removeAllPlayers();
    }

    // 当玩家离开这个实体的加载/渲染范围、或者退出游戏时，强制移除 BossBar
    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    // ===== 攻击命中后调用 =====
    public boolean cooldownStartedThisAttack = false;

    public void startAttackCooldown() {
        this.attackCooldown = 40;
        this.setActionState(ActionState.IDLE_OR_WALK);
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

    // ===== 受击:仅在idle/walk时播放僵直,死亡判定也在这里处理 =====
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);

        return result;
    }

    @Override
    public void die(DamageSource source) {
        this.cooldownStartedThisAttack = true;
        releaseLockedPlayer(); // 安全网,boss死亡时强制解锁玩家
        if (getActionState() != ActionState.DYING) {
            this.setHealth(0.01f);
            this.setActionState(ActionState.DYING);
            this.diedAnimPlaying = true;
            this.setNoAi(true);
            this.setInvulnerable(true);

            triggerAnim("body_controller", "died");
            this.diedAnimTicks = 60;
        }
    }

    // ===== GeckoLib动画 =====
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "body_controller", 5, this::bodyPredicate)
                .setCustomInstructionKeyframeHandler(this::handleInstruction));

        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> {
            // 如果实体进入死亡状态，立刻终止攻击控制器的一切动画
            if (this.getActionState() == ActionState.DYING) {
                return PlayState.STOP;
            }
            return PlayState.CONTINUE;
        })
                .triggerableAnim("attack.heavy", RawAnimation.begin().thenPlay("attack.heavy"))
                .triggerableAnim("attack.combo", RawAnimation.begin().thenPlay("attack.combo"))
                .triggerableAnim("attack.throw", RawAnimation.begin().thenPlay("attack.throw"))
                .triggerableAnim("attack.throw.catch", RawAnimation.begin().thenPlay("attack.throw.catch"))
                .triggerableAnim("attack.throw.end", RawAnimation.begin().thenPlay("attack.throw.end"))
                .setCustomInstructionKeyframeHandler(this::handleInstruction));
    }

    private PlayState bodyPredicate(AnimationState<PanGuEntity> state) {
        switch (getActionState()) {
            case INTRO -> {
                return state.setAndContinue(RawAnimation.begin().then("intro", Animation.LoopType.HOLD_ON_LAST_FRAME));
            }
            case DYING -> {
                return state.setAndContinue(RawAnimation.begin().then("died", Animation.LoopType.HOLD_ON_LAST_FRAME));
            }
            case SKILL -> {
                return state.setAndContinue(RawAnimation.begin().then("skill.start", Animation.LoopType.HOLD_ON_LAST_FRAME));
            }
            default -> {
                if (state.isMoving()) {
                    return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
                }
                return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            }
        }
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
            case "teleport" -> doTeleportToTarget();
            case "damage.heavy" -> doDamage(20.0f, AttackKind.MELEE);
            case "damage.combo.1",
                 "damage.combo.2",
                 "damage.combo.3"
                    -> doDamage(8.0f, AttackKind.MELEE);
            case "damage.catch" -> doDamage(25.0f, AttackKind.DASH);
            case "throw.start" -> doThrowStart();
            case "throw.end" -> doThrowEnd();
            case "catch.start" -> doCatchStart();
            case "catch.end" -> doCatchEnd();
            case "damage.finish" -> {
                this.cooldownStartedThisAttack = true;
            }
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
        dashStepPerTick = realDist / dashTicksLeft;
        dashHasHit = false;
        dashMoving = true;
    }

    private void doThrowEnd() {
        dashMoving = false; // 双保险
        if (!dashHasHit) {
            this.startAttackAnim("attack.throw.end");
        }
    }

    public void startAttackAnim(String animName) {
        this.triggerAnim("attack_controller", animName);
    }

    private void doCatchStart() {
        doDamage(10.0f, AttackKind.DASH);
        LivingEntity target = this.getAttackTarget();
        if (!(target instanceof Player player)) return;
        lockedPlayer = player;

        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null && speedAttr.getModifier(CATCH_LOCK_ID) == null) {
            speedAttr.addTransientModifier(new AttributeModifier(
                    CATCH_LOCK_ID, -10.0, AttributeModifier.Operation.ADD_VALUE));
        }
        AttributeInstance jumpAttr = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (jumpAttr != null && jumpAttr.getModifier(CATCH_LOCK_ID) == null) {
            jumpAttr.addTransientModifier(new AttributeModifier(
                    CATCH_LOCK_ID, -10.0, AttributeModifier.Operation.ADD_VALUE));
        }
        player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
    }

    private void doCatchEnd() {
        releaseLockedPlayer();
    }

    private void releaseLockedPlayer() {
        if (lockedPlayer == null) return;

        AttributeInstance speedAttr = lockedPlayer.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.removeModifier(CATCH_LOCK_ID);

        AttributeInstance jumpAttr = lockedPlayer.getAttribute(Attributes.JUMP_STRENGTH);
        if (jumpAttr != null) jumpAttr.removeModifier(CATCH_LOCK_ID);

        lockedPlayer = null;
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

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}