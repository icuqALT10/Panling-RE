package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

import icu.icuqalt10.panlingre.entity.PanLingEntities;
import icu.icuqalt10.panlingre.animation.WorldTimeAnimationController;
import icu.icuqalt10.panlingre.entity.MultipartEntity;
import icu.icuqalt10.panlingre.entity.OrientedBoundingBox;
import icu.icuqalt10.panlingre.network.GraveDragonActionPayload;
import icu.icuqalt10.panlingre.instance.InstanceManager;
import icu.icuqalt10.panlingre.instance.shihuang.ShiHuangController;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.entity.PartEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Set;

public class GraveDragonEntity extends MultipartEntity implements GeoEntity, PanLingEntities {
    /** 当前动画的起始时刻（世界时钟）。两侧共用它推算动画相位，掉线重连也不会重启动画。 */
    private static final EntityDataAccessor<Long> ANIMATION_START = SynchedEntityData.defineId(GraveDragonEntity.class, EntityDataSerializers.LONG);
    /** 当前动画名。用字符串而不是下标，因为 animationNames() 的遍历顺序不保证稳定。 */
    private static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(GraveDragonEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> FAST_LANDING = SynchedEntityData.defineId(GraveDragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FROZEN = SynchedEntityData.defineId(GraveDragonEntity.class, EntityDataSerializers.BOOLEAN);
    private boolean frozenWasNoAi;
    /** 形态：0 = 地面，1 = 空中。 */
    private static final EntityDataAccessor<Integer> FORM = SynchedEntityData.defineId(GraveDragonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> BODY_PITCH = SynchedEntityData.defineId(GraveDragonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> HEAD_BROKEN = SynchedEntityData.defineId(GraveDragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> BROKEN_MASK = SynchedEntityData.defineId(GraveDragonEntity.class, EntityDataSerializers.INT);

    /** 龙首俯仰的限幅（度）。身体水平长 46 格，再大就会大面积戳进地形。 */
    private static final float MAX_BODY_PITCH = 22.0F;
    /** 俯仰平滑系数，免得速度抖动直接传到龙首上。 */
    private static final float BODY_PITCH_SMOOTHING = 0.25F;

    /** 上一 tick 的俯仰，渲染插值用。 */
    private float bodyPitchO;
    private long lastClientActionSequence = Long.MIN_VALUE;
    private String blendFrom = "";
    private double blendFromSeconds;
    private String blendTarget = "";
    private long blendStart;

    /**
     * {@link #ANIMATION_START} 的未初始化哨兵。
     *
     * <p>仅此值表示未初始化，已保存的其他起点必须照常计时。
     */
    private static final long UNINITIALISED_ANIMATION_START = Long.MIN_VALUE;

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANIMATION_START, UNINITIALISED_ANIMATION_START);
        builder.define(ANIMATION, "idle_air");
        builder.define(FAST_LANDING, false);
        builder.define(FROZEN, false);
        builder.define(FORM, FORM_GROUND);
        builder.define(BODY_PITCH, 0.0F);
        builder.define(HEAD_BROKEN, false);
        builder.define(BROKEN_MASK, 0);
    }

    /** 地面 / 空中两种形态。 */
    public enum Form { GROUND, AIR }

    private static final int FORM_GROUND = 0;
    private static final int FORM_AIR = 1;

    /** 空中形态要求的最小离地高度，也是起飞前要检查的垂直净空（格）。 */
    private static final double FLIGHT_CLEARANCE = 12.0;
    /**
     * 起飞净空检查里每个姿态采样多少个相位。
     *
     * <p>姿态是**逐个碰撞箱**判定的（见 {@code poseFitsAt}），所以要覆盖尾巴最低、身体最开的
     * 时刻，不能只看首帧。
     */
    private static final int POSE_CLEARANCE_SAMPLES = 6;
    /**
     * 起飞净空判定允许的穿透容差（格）。
     *
     * <p>龙站在地面上时，脚底 OBB 与地面方块顶面在浮点上必然重叠一丁点（1/16 像素级），
     * 布尔相交会把它判成"穿模"，于是永远不起飞。1 厘米的容差足够滤掉这类噪声，
     * 同时任何真实的方块遮挡都远超这个量级。
     */
    private static final double POSE_CLEARANCE_TOLERANCE = 0.05;
    /** 姿态判定的整体抬升（格）：抵消"脚底与地面方块浮点贴合"造成的假穿透。 */
    private static final double POSE_CLEARANCE_LIFT = 0.06;
    /** 净空不足时推迟多久再试一次。 */
    private static final int TAKEOFF_RETRY_TICKS = 20 * 5;

    /** 循环播放的动画；其余按一次性处理（播完停在末帧，由 takeoff/land/turn_* 这类过渡用）。 */
    private static final Set<String> LOOPING_ANIMATIONS = Set.of("idle_air", "idle_ground", "fly", "run", "into_1", "escape_wait", "escape_flight");

    /** 飞行时每 tick 最多抬升多少格，用来维持离地高度。 */
    /** 是否正在移动，决定播 fly/run 还是 idle_*。 */
    private boolean moving;
    /** 空中移动的过渡动画播完后要进入的循环动画（null = 没有正在过渡）。 */
    private String pendingLoopAnimation;
    /** 正在走"下落第一段"（fly → idle_air），播完接着播 land。 */

    /** 服务端：下一次形态切换的时刻。 */
    private long nextFormSwitchAt;
    /** 服务端：正在过渡到哪个形态（null = 没在过渡）。过渡动画播完才真正切换。 */
    private Form pendingForm;
    private boolean introStarted;
    private boolean introComplete;
    private boolean loadingDragonData;
    private boolean bypassHealthLock;
    private boolean introRoarPlayed;
    private Vec3 spawnPosition;
    private final GraveDragonCombat combat = new GraveDragonCombat(this);
    private final float[] partDurabilityDamage = new float[6];
    private final boolean[] brokenParts = new boolean[6];
    /** 过渡动画期间的起止高度；位置每 tick 按动画进度插值，播完刚好到位。 */
    private double transitionFromY;
    private double transitionToY;

    /** 当前正在播放的动画名。 */
    public String animation() {
        return entityData.get(ANIMATION);
    }

    /** 当前动画已播放的秒数；{@code partialTick} 用于渲染插值。 */
    public double animationSeconds(float partialTick) {
        if (entityData.get(FROZEN)) return 0;
        long start = entityData.get(ANIMATION_START);
        if (start == UNINITIALISED_ANIMATION_START) return 0;
        return Math.max(0, level().getGameTime() - start + partialTick) / 20.0 * animationSpeed();
    }

    private double animationSpeed() {
        if (animation().startsWith("turn_ground_") || animation().startsWith("turn_air_"))
            return GraveDragonPose.duration(animation()) * 4;
        return entityData.get(FAST_LANDING) && animation().equals("land") ? GraveDragonPose.duration("land") * 2 : 1;
    }

    /** 当前动画的起始世界时刻，用于同步动作与特效。 */
    public long animationStart() {
        return entityData.get(ANIMATION_START);
    }

    /** 该动画是否循环播放；一次性动画的时间会被采样器钳在末帧。 */
    public boolean loopingAnimation() {
        return LOOPING_ANIMATIONS.contains(animation());
    }

    public Form form() {
        return entityData.get(FORM) == FORM_AIR ? Form.AIR : Form.GROUND;
    }

    public boolean flying() {
        return form() == Form.AIR;
    }

    /** 服务端：切换到某个动画。{@code restart} 为真时即使同名也从头播。 */
    private void playAnimation(String name, boolean restart) {
        if (!restart && name.equals(animation())) return;
        blendFrom = "";
        entityData.set(FAST_LANDING, false);
        entityData.set(ANIMATION, name);
        entityData.set(ANIMATION_START, level().getGameTime());
    }

    void beginCombatAnimation(String name) {
        pendingLoopAnimation = null;
        playAnimation(name, true);
    }

    ShiHuangController tombController() {
        var session = InstanceManager.sessionFor(this);
        return session != null && session.controller() instanceof ShiHuangController tomb ? tomb : null;
    }

    /** The owning instance controls all motion until removal; combat cannot restart. */
    public void beginSceneAnimation(String name) {
        if (!name.startsWith("escape_")) throw new IllegalArgumentException("Not a tomb scene animation: " + name);
        pendingForm = null;
        moving = false;
        disableAutomaticFormSwitch();
        entityData.set(FROZEN, false);
        entityData.set(BODY_PITCH, 0F);
        setNoAi(true);
        setNoGravity(true);
        setInvulnerable(true);
        setDeltaMovement(Vec3.ZERO);
        getNavigation().stop();
        beginCombatAnimation(name);
        combat.announceAnimation(name);
        updateDragonParts();
    }

    public Vec3 sceneHeadOffset(String animation, double seconds, float yaw) {
        return GraveDragonPose.box(GraveDragonPose.sample(animation, seconds, false),
                PART_LABELS[11], PART_BOUNDS[11], GraveDragonPose.modelToEntity(yaw, 0, getScale()), Vec3.ZERO).center;
    }

    public Vec3 sceneGateOffset(double seconds, float yaw) {
        var frame = GraveDragonPose.sample("escape_flight", seconds, true);
        var transform = GraveDragonPose.modelToEntity(yaw, 0, getScale());
        var head = GraveDragonPose.box(frame, PART_LABELS[11], PART_BOUNDS[11], transform, Vec3.ZERO);
        double front = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < PART_LABELS.length; i++)
            front = Math.max(front, GraveDragonPose.box(frame, PART_LABELS[i], PART_BOUNDS[i], transform, Vec3.ZERO).enclosingAabb().maxZ);
        return new Vec3(head.center.x, head.center.y, front);
    }

    public boolean moveSceneTo(Vec3 destination, float yaw, double floor) {
        Vec3 start = position();
        int steps = Math.max(1, (int) Math.ceil(start.distanceTo(destination) * 2));
        float previousYaw = getYRot();
        steps = Math.max(steps, (int) Math.ceil(Math.abs(Mth.wrapDegrees(yaw - previousYaw)) / 3));
        for (int i = 1; i <= steps; i++) {
            double weight = i / (double) steps;
            Vec3 candidate = start.lerp(destination, weight);
            float facing = previousYaw + Mth.wrapDegrees(yaw - previousYaw) * (float) weight;
            if (!combatPoseFitsAt(animation(), animationSeconds(0), facing, 0, candidate, floor)) return false;
        }
        setYRot(yaw);
        yBodyRot = yHeadRot = yaw;
        setPos(destination);
        setDeltaMovement(Vec3.ZERO);
        updateDragonParts();
        return true;
    }

    void beginBlendedCombatAnimation(String name) {
        String previous = animation();
        double seconds = animationSeconds(0);
        double duration = GraveDragonPose.duration(previous);
        seconds = loopingAnimation() ? seconds % duration : Math.min(seconds, duration);
        beginCombatAnimation(name);
        blendFrom = previous;
        blendFromSeconds = seconds;
        blendTarget = name;
        blendStart = animationStart();
    }

    String blendFrom() { return blendFrom; }
    double blendFromSeconds() { return blendFromSeconds; }

    /** Rendering, hitboxes and attached particles share the same two-tick transition. */
    public GraveDragonPose.Frame poseAt(double seconds) {
        var frame = GraveDragonPose.sample(animation(), seconds, loopingAnimation());
        if (blendFrom.isEmpty() || !blendTarget.equals(animation()) || blendStart != animationStart()
                || seconds >= .1) return frame;
        return GraveDragonPose.blend(GraveDragonPose.sample(blendFrom, blendFromSeconds, false),
                frame, Math.max(0, seconds / .1));
    }

    void requestCombatForm(Form wanted) {
        if (form() != wanted && pendingForm == null) nextFormSwitchAt = level().getGameTime();
    }

    boolean canTakeoffForCombat() { return !flying() && hasTakeoffClearance(); }

    double combatGroundLevel() { return groundLevelBelow(); }
    Vec3 combatSpawnPosition() { return spawnPosition == null ? position() : spawnPosition; }
    void setCombatPitch(float pitch) { entityData.set(BODY_PITCH, pitch); }

    public boolean headBroken() { return entityData.get(HEAD_BROKEN); }

    /** The action activation packet starts animation and effects on the same client tick. */
    public void applyClientAction(GraveDragonActionPayload payload) {
        if (!level().isClientSide || payload.entityId() != getId()
                || payload.sequence() < lastClientActionSequence) return;
        lastClientActionSequence = payload.sequence();
        blendFrom = payload.blendFrom();
        blendFromSeconds = payload.blendFromSeconds();
        blendTarget = payload.action();
        blendStart = payload.startTick();
        if (!payload.action().equals(animation()) || payload.startTick() != animationStart()) {
            entityData.set(ANIMATION, payload.action());
            entityData.set(ANIMATION_START, payload.startTick());
        }
        updateDragonParts();
    }

    /**
     * 龙首俯仰，**正值表示爬升（龙首抬起）**，负值表示俯冲。
     *
     * <p>符号来自旋转所在的坐标系：{@code GraveDragonPose.modelToEntity} 是 {@code Ry · Rx}
     * （顺序必须和 GeckoLib 的 {@code applyRotations} 一致），所以这个 X 轴旋转作用在**模型空间**，
     * 而模型空间里 −Z 才是龙首、+Z 是尾巴——因此正角把尾巴压低、龙首抬起。
     *
     * <p>由服务端根据**实际速度方向**算出并同步，所以两侧共用同一个角：渲染走
     * {@code GraveDragonRenderer.applyRotations}，碰撞箱走 {@code GraveDragonPose.modelToEntity}，
     * 两处都用这个值，龙首倾斜时碰撞箱才会跟着一起斜。
     */
    public float bodyPitch() {
        return entityData.get(BODY_PITCH);
    }

    /** 渲染用：在上一 tick 与当前 tick 之间插值。 */
    public float bodyPitch(float partialTick) {
        return Mth.lerp(partialTick, bodyPitchO, bodyPitch());
    }

    /**
     * 让龙首跟随飞行方向：上升抬头、俯冲低头。速度接近零时回正。
     *
     * <p>{@code atan2(vy, 水平速度)} 在上升时为正，正好对应"爬升为正"的约定。
     * 这个角度由渲染器的 {@code applyRotations} 和 {@code GraveDragonPose.modelToEntity}
     * 绕**实体局部 X 轴**施加（顺序 {@code Ry · Rx}，符号取负），所以是竖直面内的俯仰。
     */
    private void updateBodyPitch() {
        if (isTransitioningForm()) {
            entityData.set(BODY_PITCH, 0.0F);
            return;
        }
        // 地面形态不俯仰：走路本来就不该斜；而且下落时的俯冲会让几十格长的身体插进地面，
        // 被 move() 的逐部件判定挡住，表现成"悬在空中"。飞行形态会 noGravity=true，届时自动生效。
        if (!this.isNoGravity()) {
            entityData.set(BODY_PITCH, Mth.lerp(BODY_PITCH_SMOOTHING, bodyPitch(), 0.0F));
            return;
        }
        Vec3 velocity = getDeltaMovement();
        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float target = horizontal < 1.0E-4 && Math.abs(velocity.y) < 1.0E-4
                ? 0.0F
                : (float) Math.toDegrees(Math.atan2(velocity.y, horizontal));
        target = Mth.clamp(target, -MAX_BODY_PITCH, MAX_BODY_PITCH);
        entityData.set(BODY_PITCH, Mth.lerp(BODY_PITCH_SMOOTHING, bodyPitch(), target));
    }

    // ===== 形态状态机 =====

    /** 服务端每 tick 驱动形态切换，客户端只读同步结果。 */
    private void tickForm() {
        if (combat.active() || combat.retreatReady()) return;
        long now = level().getGameTime();

        if (pendingForm != null) {
            // 过渡动画**播放期间**就做垂直位移：按动画进度插值高度，播完的那一刻刚好到位。
            applyTransitionLift();
            if (animationSeconds(0) >= GraveDragonPose.duration(animation())) {
                Form target = pendingForm;
                pendingForm = null;
                // 收尾到精确高度，消掉插值残差。
                setPos(getX(), transitionToY, getZ());
                entityData.set(FORM, target == Form.AIR ? FORM_AIR : FORM_GROUND);
                applyFormPhysics(target == Form.AIR);
                combat.onFormChanged(target == Form.AIR);
                playAnimation(target == Form.AIR ? "idle_air" : "idle_ground", true);
                disableAutomaticFormSwitch();
            }
            return;
        }

        if (now < nextFormSwitchAt) return;

        if (form() == Form.AIR) {
            beginLanding();
        } else if (hasTakeoffClearance()) {
            pendingForm = Form.AIR;
            beginVerticalTransition(true, getY() + FLIGHT_CLEARANCE);
            playAnimation("takeoff", true);
        } else {
            // 当前位置放不下起飞过程：保持地面形态，过几秒换一个位置再试（漫游还在继续）。
            nextFormSwitchAt = now + TAKEOFF_RETRY_TICKS;
        }
    }

    /** 从当前空中姿态直接播放 land，动画播完刚好贴地。 */
    private void beginLanding() {
        pendingLoopAnimation = null;
        pendingForm = Form.GROUND;
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        moving = false;
        beginVerticalTransition(false, groundLevelBelow());
        playAnimation("land", true);
        combat.announceAnimation("land");
    }

    void beginRetreatLanding() {
        entityData.set(BODY_PITCH, 0F);
        beginLanding();
        entityData.set(FAST_LANDING, true);
    }

    boolean isRetreatLanding() { return pendingForm == Form.GROUND && entityData.get(FAST_LANDING); }

    /**
     * 记下过渡的起止高度，并临时关掉重力——否则插值抬升会和下落叠加。
     * 过渡结束后由 {@link #applyFormPhysics} 决定最终的重力状态。
     */
    private void beginVerticalTransition(boolean ascending, double targetY) {
        this.transitionFromY = getY();
        // 方向由状态机决定，不能被地面探测的边界情况反转：下降只允许往下、上升只允许往上。
        this.transitionToY = ascending ? Math.max(targetY, getY()) : Math.min(targetY, getY());
        this.setNoGravity(true);
    }

    /** 按过渡动画的播放进度插值高度。 */
    private void applyTransitionLift() {
        double duration = GraveDragonPose.duration(animation());
        double progress = duration <= 0 ? 1 : Mth.clamp(animationSeconds(0) / duration, 0, 1);
        double y = Mth.lerp(progress, transitionFromY, transitionToY);
        if (Math.abs(y - getY()) > 1.0E-6) setPos(getX(), y, getZ());
    }

    /** 从实体脚下向下探测当前房间的碰撞表面；虚空中保持原高。 */
    private double groundLevelBelow() {
        // 从实体下方找当前房间的地面，避免高度图把墓室屋顶当成落地高度。
        var hit = level().clip(new net.minecraft.world.level.ClipContext(
                position().add(0, .25, 0), new Vec3(getX(), level().getMinBuildHeight(), getZ()),
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, this));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK ? hit.getLocation().y : getY();
    }

    /** 墓龙是飞行生物：落地、以及过渡期间的程序化位移都不该造成摔落伤害。 */
    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    /**
     * 不会因为玩家走远而被自然清除。
     *
     * <p>{@code Monster} 默认会在超出"离家距离"后 {@code discard()}——墓龙带 BossBar、还有领地行为，
     * 玩家跑去别处转一圈回来会发现它凭空消失。它的漫游本来就锁在领地半径内，不受玩家位置影响，
     * 所以这里直接关掉远距离清除。
     */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /** 和平难度下也不消失（BossBar 生物的常规做法）。 */
    @Override
    public boolean shouldDespawnInPeaceful() {
        return false;
    }

    // ===== 漫游与动画选择 =====

    /** 是否正在形态过渡（takeoff/land 播放中）。 */
    public boolean isTransitioningForm() {
        return pendingForm != null;
    }

    /** 是否正在移动；服务端设置，决定播 fly/run 还是 idle_*。 */
    public void setMoving(boolean moving) {
        this.moving = moving;
    }

    /** Select the idle or locomotion loop when combat and form transitions are not driving a clip. */
    private void tickAnimation() {
        if (pendingForm != null || combat.active() || combat.controlsMovement()) return;

        if (pendingLoopAnimation != null) {
            if (animationSeconds(0) >= GraveDragonPose.duration(animation())) {
                String next = pendingLoopAnimation;
                pendingLoopAnimation = null;
                playAnimation(next, true);
            }
            return;
        }

        String desired = moving ? (flying() ? "fly" : "run") : (flying() ? "idle_air" : "idle_ground");
        if (desired.equals(animation())) return;

        // 空中的待机 <-> 飞行之间必须走过渡动画；地面 run 与 idle_ground 是同一套形状，直接切。
        if (flying()) {
            if (animation().equals("idle_air") && desired.equals("fly")) {
                startLoopTransition("fly");
                return;
            }
            if (animation().equals("fly") && desired.equals("idle_air")) {
                startLoopTransition("idle_air");
                return;
            }
        }
        playAnimation(desired, true);
    }

    /** 播放空中待机<->飞行的过渡动画，播完切到 {@code next}。 */
    private void startLoopTransition(String next) {
        pendingLoopAnimation = next;
        playAnimation(next.equals("fly") ? "idle_air_to_fly" : "fly_to_idle_air", true);
    }

    private void disableAutomaticFormSwitch() {
        this.nextFormSwitchAt = Long.MAX_VALUE;
    }

    private void applyFormPhysics(boolean air) {
        this.setNoGravity(air);
        this.setDeltaMovement(Vec3.ZERO);
        this.moving = false;
        // 换形态就换寻路器。旧导航连同它的路径一起丢弃，新导航是干净的，不会残留上一种形态的路线。
        this.navigation = createNavigation(this.level());
    }

    /**
     * 起飞前的净空检查：**把整个飞行姿态逐碰撞箱摆到巡航高度**，任何一个碰撞箱插进方块都不许起飞。
     *
     * <p>按最新导出动作的实际 79 个 OBB 检查，不沿用旧模型的固定身体深度。
     * 采样 {@code fly} / {@code idle_air} 的多个相位，起点再检查 {@code idle_ground}。
     */
    private boolean hasTakeoffClearance() {
        if (!poseFitsAt("fly", FLIGHT_CLEARANCE)) return false;
        if (!poseFitsAt("idle_air", FLIGHT_CLEARANCE)) return false;
        // 起点也要放得下：地面姿态此刻就在地面上。
        return poseFitsAt("idle_ground", 0.0);
    }

    /**
     * 把 {@code pose} 姿态摆在"当前位置抬高 {@code lift} 格"处，看有没有碰撞箱插进方块。
     *
     * <p>直接构造 OBB 而不是 {@code level().clip}：那个重载会把墓龙自己当成忽略对象，而它的身体
     * 有 20 多格高，射线一出锚点就撞到自己身上，"地面"永远探在当前高度（实测确认过）。
     */
    private boolean poseFitsAt(String pose, double lift) {
        // v6 地面循环最低点约 -0.033 格；小幅抬高检测位，避免脚底贴地被当成穿透。
        Vec3 origin = new Vec3(getX(), getY() + lift + POSE_CLEARANCE_LIFT, getZ());
        double duration = GraveDragonPose.duration(pose);
        var transform = GraveDragonPose.modelToEntity(yBodyRot, 0.0F, getScale());
        // 采样几个相位，覆盖尾巴最低 / 身体最开的时刻。
        for (int step = 0; step < POSE_CLEARANCE_SAMPLES; step++) {
            var frame = GraveDragonPose.sample(pose, duration * step / POSE_CLEARANCE_SAMPLES, false);
            for (int i = 0; i < PART_LABELS.length; i++) {
                var box = GraveDragonPose.box(frame, PART_LABELS[i], PART_BOUNDS[i],
                        transform, origin);
                if (penetrationIntoBlocks(box) > POSE_CLEARANCE_TOLERANCE) return false;
            }
        }
        return true;
    }

    /**
     * 某个 OBB 插进方块最深多少格（没有插入则为 0）。
     *
     * <p>用"最深穿透量"而不是布尔相交：龙站在地面上时脚底与地面方块顶面在浮点上会重叠
     * 一丁点（1/pixel 级），布尔判定会把它当成穿模，于是永远不起飞、{@code move()} 也永远
     * 把移动取消掉（表现就是"面朝前却在原地/横向漂"）。给它一个容差即可。
     *
     * <p>深度取 OBB 包围盒与方块的 Y 重叠量：对"站在方块上"这种情形就是最直观的穿模深度。
     */
    private double penetrationIntoBlocks(OrientedBoundingBox box) {
        return penetrationIntoBlocks(box, Double.NEGATIVE_INFINITY);
    }

    private double penetrationIntoBlocks(OrientedBoundingBox box, double contactFloor) {
        net.minecraft.world.phys.AABB envelope = box.enclosingAabb();
        double deepest = 0;
        for (var shape : level().getBlockCollisions(this, envelope)) {
            for (var block : shape.toAabbs()) {
                if (block.maxY <= contactFloor + POSE_CLEARANCE_TOLERANCE) continue;
                deepest = Math.max(deepest, obbBlockPenetration(box, block));
            }
        }
        return deepest;
    }

    /**
     * OBB 与方块 AABB 的穿透深度（不相交为 0）。用的是标准 OBB-AABB 分离轴测试。
     *
     * <p>**不能用包围盒近似**：龙是 40 多格长、又带朝向旋转，它的 OBB 包围盒会膨胀成一大块，
     * 于是"站在平地上"也会被算成插进旁边方块——表现就是永远不起飞、地面形态也动不了。
     * 15 根分离轴（各自 3 个面法线 + 9 个叉积）逐一判定，全部重叠才算真的穿模，
     * 深度取重叠轴里的最小值。
     */
    private static double obbBlockPenetration(OrientedBoundingBox box, net.minecraft.world.phys.AABB block) {
        Vec3[] boxAxes = {box.axisX, box.axisY, box.axisZ};
        Vec3[] worldAxes = {new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1)};
        Vec3 blockCentre = new Vec3((block.minX + block.maxX) / 2, (block.minY + block.maxY) / 2,
                (block.minZ + block.maxZ) / 2);
        double[] blockHalf = {(block.maxX - block.minX) / 2, (block.maxY - block.minY) / 2,
                (block.maxZ - block.minZ) / 2};
        Vec3 between = box.center.subtract(blockCentre);
        double[] boxHalf = {box.halfExtents.x, box.halfExtents.y, box.halfExtents.z};
        Vec3[] candidates = new Vec3[15];
        candidates[0] = boxAxes[0];
        candidates[1] = boxAxes[1];
        candidates[2] = boxAxes[2];
        candidates[3] = worldAxes[0];
        candidates[4] = worldAxes[1];
        candidates[5] = worldAxes[2];
        int at = 6;
        for (Vec3 a : boxAxes) {
            for (Vec3 b : worldAxes) candidates[at++] = a.cross(b);
        }
        double minOverlap = Double.MAX_VALUE;
        for (Vec3 candidate : candidates) {
            double length = candidate.length();
            if (length < 1.0E-9) continue;
            Vec3 axis = candidate.scale(1.0 / length);
            double rBox = 0, rBlock = 0;
            for (int i = 0; i < 3; i++) {
                rBox += boxHalf[i] * Math.abs(axis.dot(boxAxes[i]));
                rBlock += blockHalf[i] * Math.abs(axis.dot(worldAxes[i]));
            }
            double distance = Math.abs(between.dot(axis));
            if (distance >= rBox + rBlock - 1.0E-9) return 0; // 找到分离轴：不相交
            minOverlap = Math.min(minOverlap, rBox + rBlock - distance);
        }
        return minOverlap == Double.MAX_VALUE ? 0 : minOverlap;
    }

    // ===== OBB 调整表：行号必须与下方 PART_LABELS 一一对应，不要单独增删/换序 =====
    // 每行前 6 项：[宽 X, 高 Y, 长 Z, 中心 X, 中心 Y, 中心 Z]。
    // 尺寸是完整边长，不是半长；单位为方块（Blockbench 的像素坐标/尺寸除以 16）。
    // 中心为模型静止姿态下的绝对坐标，不是相对骨骼 pivot 的偏移，也不是世界坐标。
    // 坐标方向沿用 BB 模型：Y 向上，嘴朝 -Z、尾朝 +Z；左右沿用骨骼 l/r 命名。
    // 前 3 项改大小，后 3 项移位置；动画、实体朝向及 getScale() 缩放由姿态代码统一应用。
    // 可选第 7~9 项：[旋转 X, 旋转 Y, 旋转 Z]，单位为度；不填即 0。
    // 旋转围绕碰撞框中心，矩阵为 Rz * Ry * Rx（对点先 X、再 Y、最后 Z）。
    // 2026-10-01：按用户最终 dragon_v6 的实心几何重新拟合；忽略鬃毛/胡须面片。
    // 上下嘴各自贴合模型；四肢保留双段 ID，指节与龙角使用几何自身的局部旋转。
    // 碰撞箱尺寸以本表为准；修改 Java 后须重新启动游戏。
    private static final float[][] HARD_CODED_PART_BOUNDS = {
        {3.16125000f, 2.95937500f, 4.00000000f, 0.00000000f, 8.23518125f, 15.25000000f, 0.00000000f, 0.00000000f, 0.00000000f}, // [0] neck_01
        {3.16125000f, 2.95937500f, 4.00000000f, 0.00000000f, 8.23518125f, 12.25000000f, 0.00000000f, 0.00000000f, 0.00000000f}, // [1] neck_02
        {3.16125000f, 2.95937500f, 4.00000000f, 0.00000000f, 8.23518125f, 9.25000000f, 0.00000000f, 0.00000000f, 0.00000000f}, // [2] neck_03
        {3.16125000f, 2.95937500f, 4.00000000f, 0.00000000f, 8.23518125f, 6.25000000f, 0.00000000f, 0.00000000f, 0.00000000f}, // [3] neck_04
        {3.16125000f, 2.95937500f, 4.37500000f, 0.00000000f, 8.23518125f, 3.06250000f, 0.00000000f, 0.00000000f, 0.00000000f}, // [4] neck_05
        {3.16125000f, 2.95937500f, 3.50000000f, 0.00000000f, 8.23518125f, 0.00000000f, 0.00000000f, 0.00000000f, 0.00000000f}, // [5] neck_06
        {3.16125000f, 2.95937500f, 4.00000000f, 0.00000000f, 8.23518125f, -2.75000000f, 0.00000000f, 0.00000000f, 0.00000000f}, // [6] neck_07
        {3.17143904f, 2.95937500f, 4.01601147f, -0.02379250f, 8.23518125f, -5.75800574f, 0.00000000f, 0.00000000f, 0.00000000f}, // [7] neck_08
        {3.17143904f, 2.95937500f, 4.01601147f, -0.02379250f, 8.23518125f, -8.75800574f, 0.00000000f, 0.00000000f, 0.00000000f}, // [8] neck_09
        {3.17143904f, 2.95937500f, 4.01601147f, -0.02379250f, 8.23518125f, -11.75800574f, 0.00000000f, 0.00000000f, 0.00000000f}, // [9] neck_10
        {3.17143904f, 2.95937500f, 4.01601147f, -0.02379250f, 8.23518125f, -14.75800574f, 0.00000000f, 0.00000000f, 0.00000000f}, // [10] neck_11
        {4.90922001f, 4.47634781f, 4.57501377f, -0.04951432f, 8.79381203f, -19.18794739f, 0.00000000f, 0.00000000f, 0.00000000f}, // [11] head
        {1.87704875f, 0.87516813f, 3.32710937f, -0.04951375f, 7.31714406f, -22.12241906f, 0.00000000f, 0.00000000f, 0.00000000f}, // [12] jaw
        {3.16125000f, 2.95937500f, 4.00000000f, 0.00000000f, 8.23518125f, 18.25000000f, 0.00000000f, 0.00000000f, 0.00000000f}, // [13] tail_01
        {3.16125000f, 2.95937500f, 4.00000000f, 0.00000000f, 8.23518125f, 21.25000000f, 0.00000000f, 0.00000000f, 0.00000000f}, // [14] tail_02
        {2.68394000f, 2.51060062f, 3.98453125f, 0.00000000f, 8.25678719f, 24.24226563f, 0.00000000f, 0.00000000f, 0.00000000f}, // [15] tail_03
        {2.17640687f, 1.98868125f, 3.87828125f, 0.00000031f, 8.25479375f, 27.18914063f, 0.00000000f, 0.00000000f, 0.00000000f}, // [16] tail_04
        {1.65817000f, 1.46234562f, 3.71890625f, 0.00000000f, 8.25632344f, 30.10945313f, 0.00000000f, 0.00000000f, 0.00000000f}, // [17] tail_05
        {1.14875000f, 0.95000000f, 3.58609375f, 0.00000000f, 8.25549375f, 33.04304688f, 0.00000000f, 0.00000000f, 0.00000000f}, // [18] tail_06
        {0.81125000f, 0.63750000f, 3.47984375f, 0.00000000f, 8.23986875f, 35.98992188f, 0.00000000f, 0.00000000f, 0.00000000f}, // [19] tail_07
        {0.43750000f, 0.43750000f, 3.12500000f, 0.00000000f, 8.27111875f, 38.81250000f, 0.00000000f, 0.00000000f, 0.00000000f}, // [20] tail_tip
        {2.97700709f, 2.95937500f, 3.51604197f, -0.05780479f, 8.23518125f, -17.50783724f, 0.00000000f, 0.00000000f, 0.00000000f}, // [21] neck_joint
        {2.05000000f, 1.75000000f, 2.30000000f, -2.05623432f, 8.42226581f, -0.40990349f, -26.40319000f, -7.98133000f, -25.62489000f}, // [22] front_l_upper_a
        {1.65000000f, 3.95226938f, 1.65000000f, -2.00000000f, 6.35662781f, -0.18750000f, 0.00000000f, 90.00000000f, 180.00000000f}, // [23] front_l_upper_b
        {1.35000000f, 2.03125000f, 1.20000000f, -2.00000063f, 3.50545688f, -0.18750000f, 0.00000000f, 0.00000000f, 0.00000000f}, // [24] front_l_forearm_a
        {1.35000000f, 2.03125000f, 1.20000000f, -2.00000063f, 1.47420688f, -0.18750000f, 0.00000000f, 0.00000000f, 0.00000000f}, // [25] front_l_forearm_b
        {2.02500000f, 0.56250000f, 1.57500000f, -2.00000000f, 0.28125000f, -0.56250000f, 0.00000000f, 0.00000000f, 0.00000000f}, // [26] front_l_hand
        {0.37500000f, 1.31250000f, 0.37500000f, -2.91484312f, 0.18750000f, -1.86976437f, -90.00000000f, 25.00000000f, 0.00000000f}, // [27] front_l_digit_1
        {0.25000000f, 0.93750000f, 0.25000000f, -3.36387500f, 0.12500000f, -2.83271688f, -90.00000000f, 25.00000000f, 0.00000000f}, // [28] front_l_digit_1_tip
        {0.37500000f, 1.50000000f, 0.37500000f, -2.00000000f, 0.18750000f, -2.02500000f, -90.00000000f, 0.00000000f, 0.00000000f}, // [29] front_l_digit_2
        {0.25000000f, 0.93750000f, 0.25000000f, -2.00000000f, 0.12500000f, -3.18125000f, -90.00000000f, 0.00000000f, 0.00000000f}, // [30] front_l_digit_2_tip
        {0.37500000f, 1.50000000f, 0.37500000f, -1.04553625f, 0.18750000f, -1.95473063f, -90.00000000f, -25.00000000f, 0.00000000f}, // [31] front_l_digit_3
        {0.25000000f, 0.93750000f, 0.25000000f, -0.55688375f, 0.12500000f, -3.00264937f, -90.00000000f, -25.00000000f, 0.00000000f}, // [32] front_l_digit_3_tip
        {0.37500000f, 1.50000000f, 0.37500000f, -1.96250000f, 0.18750000f, 0.90000000f, 90.00000000f, 0.00000000f, 180.00000000f}, // [33] front_l_thumb
        {0.25000000f, 0.93750000f, 0.25000000f, -1.96250000f, 0.12500000f, 2.05625000f, 90.00000000f, 0.00000000f, 180.00000000f}, // [34] front_l_thumb_tip
        {2.05000000f, 1.75000000f, 2.30000000f, 1.84692453f, 8.32186950f, -0.40990345f, -26.40320000f, -7.98130000f, 25.62490000f}, // [35] front_r_upper_a
        {1.65000000f, 3.95226937f, 1.65000000f, 2.00000000f, 6.35662844f, -0.18750000f, 0.00000000f, 90.00000000f, 180.00000000f}, // [36] front_r_upper_b
        {1.35000000f, 2.03125000f, 1.20000000f, 2.00000000f, 3.50545688f, -0.18750000f, 0.00000000f, 0.00000000f, 0.00000000f}, // [37] front_r_forearm_a
        {1.35000000f, 2.03125000f, 1.20000000f, 2.00000000f, 1.47420688f, -0.18750000f, 0.00000000f, 0.00000000f, 0.00000000f}, // [38] front_r_forearm_b
        {2.02500000f, 0.56250000f, 1.57500000f, 2.00000000f, 0.28125000f, -0.56250000f, 0.00000000f, 0.00000000f, 0.00000000f}, // [39] front_r_hand
        {0.37500000f, 1.31250000f, 0.37500000f, 1.08515687f, 0.18750000f, -1.86976437f, -90.00000000f, 25.00000000f, 0.00000000f}, // [40] front_r_digit_1
        {0.25000000f, 0.93750000f, 0.25000000f, 0.63612500f, 0.12500000f, -2.83271688f, -90.00000000f, 25.00000000f, 0.00000000f}, // [41] front_r_digit_1_tip
        {0.37500000f, 1.50000000f, 0.37500000f, 2.00000000f, 0.18750000f, -2.02500000f, -90.00000000f, 0.00000000f, 0.00000000f}, // [42] front_r_digit_2
        {0.25000000f, 0.93750000f, 0.25000000f, 2.00000000f, 0.12500000f, -3.18125000f, -90.00000000f, 0.00000000f, 0.00000000f}, // [43] front_r_digit_2_tip
        {0.37500000f, 1.50000000f, 0.37500000f, 2.95446375f, 0.18750000f, -1.95473125f, -90.00000000f, -25.00000000f, 0.00000000f}, // [44] front_r_digit_3
        {0.31250000f, 0.93750000f, 0.25000000f, 3.44311625f, 0.12500000f, -3.00264937f, -90.00000000f, -25.00000000f, 0.00000000f}, // [45] front_r_digit_3_tip
        {0.37500000f, 1.50000000f, 0.37500000f, 2.03750000f, 0.18750000f, 0.90000000f, 90.00000000f, 0.00000000f, 180.00000000f}, // [46] front_r_thumb
        {0.25000000f, 0.93750000f, 0.25000000f, 2.03750000f, 0.12500000f, 2.05625000f, 90.00000000f, 0.00000000f, 180.00000000f}, // [47] front_r_thumb_tip
        {2.05000000f, 1.75000000f, 2.30000000f, -2.05623433f, 8.42226583f, 20.59009655f, -26.40320000f, -7.98130000f, -25.62490000f}, // [48] hind_l_upper_a
        {1.65000000f, 3.95226938f, 1.65000000f, -2.00000000f, 6.35662781f, 20.81250000f, 0.00000000f, 90.00000000f, 180.00000000f}, // [49] hind_l_upper_b
        {1.35000000f, 2.03125000f, 1.20000000f, -2.00000063f, 3.50545688f, 20.81250000f, 0.00000000f, 0.00000000f, 0.00000000f}, // [50] hind_l_forearm_a
        {1.35000000f, 2.03125000f, 1.20000000f, -2.00000063f, 1.47420688f, 20.81250000f, 0.00000000f, 0.00000000f, 0.00000000f}, // [51] hind_l_forearm_b
        {2.02500000f, 0.56250000f, 1.57500000f, -2.00000000f, 0.28125000f, 20.43750000f, 0.00000000f, 0.00000000f, 0.00000000f}, // [52] hind_l_hand
        {0.37500000f, 1.31250000f, 0.37500000f, -2.91484313f, 0.18750000f, 19.13023563f, -90.00000000f, 25.00000000f, 0.00000000f}, // [53] hind_l_digit_1
        {0.25000000f, 0.93750000f, 0.25000000f, -3.36387500f, 0.12500000f, 18.16728313f, -90.00000000f, 25.00000000f, 0.00000000f}, // [54] hind_l_digit_1_tip
        {0.37500000f, 1.50000000f, 0.37500000f, -2.00000000f, 0.18750000f, 18.97500000f, -90.00000000f, 0.00000000f, 0.00000000f}, // [55] hind_l_digit_2
        {0.25000000f, 0.93750000f, 0.25000000f, -2.00000000f, 0.12500000f, 17.81875000f, -90.00000000f, 0.00000000f, 0.00000000f}, // [56] hind_l_digit_2_tip
        {0.37500000f, 1.50000000f, 0.37500000f, -1.04553625f, 0.18750000f, 19.04526937f, -90.00000000f, -25.00000000f, 0.00000000f}, // [57] hind_l_digit_3
        {0.25000000f, 0.93750000f, 0.25000000f, -0.55688375f, 0.12500000f, 17.99735062f, -90.00000000f, -25.00000000f, 0.00000000f}, // [58] hind_l_digit_3_tip
        {0.37500000f, 1.50000000f, 0.37500000f, -1.96250000f, 0.18750000f, 21.90000000f, 90.00000000f, 0.00000000f, 180.00000000f}, // [59] hind_l_thumb
        {0.25000000f, 0.93750000f, 0.25000000f, -1.96250000f, 0.12500000f, 23.05625000f, 90.00000000f, 0.00000000f, 180.00000000f}, // [60] hind_l_thumb_tip
        {2.05000000f, 1.75000000f, 2.30000000f, 1.84692453f, 8.32186950f, 20.59009655f, -26.40320000f, -7.98130000f, 25.62490000f}, // [61] hind_r_upper_a
        {1.65000000f, 3.95226937f, 1.65000000f, 2.00000000f, 6.35662844f, 20.81250000f, 0.00000000f, 90.00000000f, 180.00000000f}, // [62] hind_r_upper_b
        {1.35000000f, 2.03125000f, 1.20000000f, 2.00000000f, 3.50545688f, 20.81250000f, 0.00000000f, 0.00000000f, 0.00000000f}, // [63] hind_r_forearm_a
        {1.35000000f, 2.03125000f, 1.20000000f, 2.00000000f, 1.47420688f, 20.81250000f, 0.00000000f, 0.00000000f, 0.00000000f}, // [64] hind_r_forearm_b
        {2.02500000f, 0.56250000f, 1.57500000f, 2.00000000f, 0.28125000f, 20.43750000f, 0.00000000f, 0.00000000f, 0.00000000f}, // [65] hind_r_hand
        {0.37500000f, 1.31250000f, 0.37500000f, 1.08515687f, 0.18750000f, 19.13023562f, -90.00000000f, 25.00000000f, 0.00000000f}, // [66] hind_r_digit_1
        {0.25000000f, 0.93750000f, 0.25000000f, 0.63612500f, 0.12500000f, 18.16728313f, -90.00000000f, 25.00000000f, 0.00000000f}, // [67] hind_r_digit_1_tip
        {0.37500000f, 1.50000000f, 0.37500000f, 2.00000000f, 0.18750000f, 18.97500000f, -90.00000000f, 0.00000000f, 0.00000000f}, // [68] hind_r_digit_2
        {0.25000000f, 0.93750000f, 0.25000000f, 2.00000000f, 0.12500000f, 17.81875000f, -90.00000000f, 0.00000000f, 0.00000000f}, // [69] hind_r_digit_2_tip
        {0.37500000f, 1.50000000f, 0.37500000f, 2.95446375f, 0.18750000f, 19.04526875f, -90.00000000f, -25.00000000f, 0.00000000f}, // [70] hind_r_digit_3
        {0.25000000f, 0.93750000f, 0.25000000f, 3.44311625f, 0.12500000f, 17.99735062f, -90.00000000f, -25.00000000f, 0.00000000f}, // [71] hind_r_digit_3_tip
        {0.37500000f, 1.50000000f, 0.37500000f, 1.96250000f, 0.18750000f, 21.90000000f, 90.00000000f, 0.00000000f, 180.00000000f}, // [72] hind_r_thumb
        {0.25000000f, 0.93750000f, 0.25000000f, 1.96250000f, 0.12500000f, 23.05625000f, 90.00000000f, 0.00000000f, 180.00000000f}, // [73] hind_r_thumb_tip
        {0.69710479f, 4.24955625f, 1.65265285f, -2.43765162f, 11.59364793f, -18.22347607f, 43.63387000f, -22.22635000f, 12.52337000f}, // [74] horn_l_1
        {2.02707429f, 3.14880841f, 1.16569227f, -3.62348373f, 13.46511602f, -16.09811009f, 55.75736000f, -7.08012000f, 3.10512000f}, // [75] horn_l_2
        {0.69710485f, 4.24955625f, 1.65265277f, 2.31265159f, 11.59364791f, -18.22347603f, 43.63387000f, 22.22635000f, -12.52337000f}, // [76] horn_r_1
        {2.02707429f, 3.14880841f, 1.16569227f, 3.49848373f, 13.46511602f, -16.09811009f, 55.75736000f, 7.08012000f, -3.10512000f}, // [77] horn_r_2
        {1.87704875f, 1.38457125f, 3.40218688f, -0.04951375f, 8.28292375f, -22.14738031f, 0.00000000f, 0.00000000f, 0.00000000f}, // [78] upper_jaw
    };
    static final float[][] PART_BOUNDS = HARD_CODED_PART_BOUNDS;
    private final GraveDragonPartEntity[] worldParts = new GraveDragonPartEntity[PART_BOUNDS.length];
    static final String[] PART_LABELS = {
            "neck_01", "neck_02", "neck_03", "neck_04", "neck_05", "neck_06", "neck_07", "neck_08",
            "neck_09", "neck_10", "neck_11", "head", "jaw", "tail_01", "tail_02", "tail_03", "tail_04",
            "tail_05", "tail_06", "tail_07", "tail_tip", "neck_joint", "front_l_upper_a", "front_l_upper_b",
            "front_l_forearm_a", "front_l_forearm_b", "front_l_hand", "front_l_digit_1", "front_l_digit_1_tip", "front_l_digit_2", "front_l_digit_2_tip",
            "front_l_digit_3", "front_l_digit_3_tip", "front_l_thumb", "front_l_thumb_tip", "front_r_upper_a", "front_r_upper_b",
            "front_r_forearm_a", "front_r_forearm_b", "front_r_hand", "front_r_digit_1", "front_r_digit_1_tip", "front_r_digit_2",
            "front_r_digit_2_tip", "front_r_digit_3", "front_r_digit_3_tip", "front_r_thumb", "front_r_thumb_tip",
            "hind_l_upper_a", "hind_l_upper_b", "hind_l_forearm_a", "hind_l_forearm_b", "hind_l_hand", "hind_l_digit_1", "hind_l_digit_1_tip", "hind_l_digit_2",
            "hind_l_digit_2_tip", "hind_l_digit_3", "hind_l_digit_3_tip", "hind_l_thumb", "hind_l_thumb_tip",
            "hind_r_upper_a", "hind_r_upper_b", "hind_r_forearm_a", "hind_r_forearm_b", "hind_r_hand", "hind_r_digit_1", "hind_r_digit_1_tip", "hind_r_digit_2",
            "hind_r_digit_2_tip", "hind_r_digit_3", "hind_r_digit_3_tip", "hind_r_thumb", "hind_r_thumb_tip",
            "horn_l_1", "horn_l_2", "horn_r_1", "horn_r_2", "upper_jaw"
    };

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ===== BossBar 设置 =====
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            this.getDisplayName(),
            BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.NOTCHED_20
    ).setDarkenScreen(true)
            .setCreateWorldFog(true);

    /**
     * BossBar 必须显式把玩家加进来才会显示——{@link ServerBossEvent} 不会自己订阅。
     * 原版凋灵/末影龙都是在 {@code startSeenByPlayer} / {@code stopSeenByPlayer} 里成对增删的，
     * 这里之前漏了这两个覆盖，所以墓龙的 BossBar 从创建到移除都没有任何观众。
     */
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (introStarted) this.bossEvent.addPlayer(player);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, combat.trackingPayload());
        combat.sendFireField(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    public GraveDragonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        if (!level.isClientSide) {
            // Spawn dormant on the ground; the first NoAI:false starts into_2.
            entityData.set(ANIMATION_START, level.getGameTime());
            entityData.set(ANIMATION, "into_1");
            entityData.set(FORM, FORM_GROUND);
            this.nextFormSwitchAt = Long.MAX_VALUE;
        }
        // The tiny root is only a locomotion anchor; OBB parts handle interaction.
        for (int i = 0; i < worldParts.length; i++) {
            worldParts[i] = new GraveDragonPartEntity(this, i);
        }
        // Same id reservation as NeoForge's EnderDragon: clients derive part ids
        // from the root spawn packet, with no independently tracked child entities.
        setId(ENTITY_COUNTER.getAndAdd(worldParts.length + 1) + 1);
        updatePartPose(GraveDragonPose.sample(0), yBodyRot, position());
        // Entity 的构造函数只把 dimensions 设成 EntityType 的默认值，refreshDimensions() 要等
        // pose 同步数据变化才会跑；所以这里显式刷一次，否则 NAME_TAG 挂点还是默认值。
        this.refreshDimensions();
        this.noPhysics = false;
        this.setNoAi(true);
        // 初始是地面形态，先吃重力；起飞过渡开始时才会临时关掉。
        this.setNoGravity(false);
    }

    @Override public boolean isMultipartEntity() { return true; }
    @Override public PartEntity<?>[] getParts() { return worldParts; }
    @Override protected Entity[] multipartParts() { return worldParts; }
    public GraveDragonPartEntity[] getWorldParts() { return worldParts; }

    /** Shared charge damage volume and client cloud emitter volume. */
    public net.minecraft.world.phys.AABB chargeBounds() {
        var box = worldParts[0].getOrientedBox().enclosingAabb();
        for (int i = 1; i < worldParts.length; i++)
            box = box.minmax(worldParts[i].getOrientedBox().enclosingAabb());
        return box;
    }

    @Override public void setId(int id) {
        super.setId(id);
        if (worldParts != null) {
            for (int i = 0; i < worldParts.length; i++) {
                if (worldParts[i] != null) worldParts[i].setId(id + i + 1);
            }
        }
    }

    /** The locomotion anchor is never an extra damage target. */
    @Override public boolean isPickable() { return false; }

    @Override
    protected PathNavigation createNavigation(Level level) {
        // 始皇陵副本是个固定竞技场：出招全部由脚本化的攻击状态机驱动（见设计规格），
        // 不做 A* 寻路，所以这里用原版地面导航即可（只为满足 Mob 的导航非空）。
        return new GroundPathNavigation(this, level);
    }

    // ===== 名牌 / 世界内血条的挂点 =====
    // 世界内血条（TES 的 in-world HUD）和原版名牌都挂在 EntityAttachment.NAME_TAG 上，而挂点
    // 由 EntityDimensions 的 attachments 决定。主体只有 1cm，挂点自然就贴在锚点上，血条于是
    // 显示在龙的身体根部而不是头上。
    //
    // 这里只把 NAME_TAG 挂点抬到头顶，**宽高原样不动**：碰撞盒、getBbWidth/getBbHeight、
    // 粒子散布、MoveControl 全都不受影响，未指定的挂点由 EntityAttachments.Builder.build()
    // 自动补默认值。
    //
    // 数值实测自当前 idle_air 姿态在实体局部坐标下的头部范围：Z -5.4..+1.2、Y 20.9..26.2
    // （头就在锚点正上方，尾巴才在后方 40 格）。+0.5 是渲染时的固定抬升。
    private static final float NAME_TAG_HEIGHT = 26.0F;
    private static final float NAME_TAG_FORWARD = -2.0F;

    private static final EntityAttachments.Builder NAME_TAG_ATTACHMENT = EntityAttachments.builder()
            .attach(EntityAttachment.NAME_TAG, 0.0F, NAME_TAG_HEIGHT, NAME_TAG_FORWARD);

    /** 宽高仍是 1cm 锚点，只是挂点变了。 */
    private static final EntityDimensions ANCHOR_DIMENSIONS =
            EntityDimensions.scalable(0.01F, 0.01F).withAttachments(NAME_TAG_ATTACHMENT);

    /**
     * {@code LivingEntity.getDimensions(Pose)} 是 final，这是唯一的尺寸钩子（它会再乘 getScale()）。
     * 这里只为了换掉 attachments，宽高保持锚点大小。
     */
    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return ANCHOR_DIMENSIONS;
    }

    /** Part damage is routed here; head is vulnerable, tail is more resistant. */
    protected float damageMultiplierForPart(int index) {
        if (index == 11 || index == 12 || index >= 74 && index <= 78) return 2.0F;
        if (index >= 15 && index <= 20) return 0.6F;
        if (index <= 10 || index == 21) return 1.0F;
        return 0.8F;
    }

    private static int durabilityGroup(int index) {
        if (index == 11 || index == 12 || index >= 74 && index <= 78) return 0;
        if (index >= 22 && index <= 34) return 1;
        if (index >= 35 && index <= 47) return 2;
        if (index >= 48 && index <= 60) return 3;
        if (index >= 61 && index <= 73) return 4;
        if (index >= 15 && index <= 20) return 5;
        return -1;
    }

    private static final String[] BREAK_GROUPS = {"head", "front_l", "front_r", "hind_l", "hind_r", "tail"};
    private static final float[] BREAK_THRESHOLDS = {.20F, .08F, .08F, .08F, .08F, .10F};

    public boolean partBroken(String group) {
        for (int i = 0; i < BREAK_GROUPS.length; i++)
            if (BREAK_GROUPS[i].equals(group)) return (entityData.get(BROKEN_MASK) & (1 << i)) != 0;
        return false;
    }

    public boolean hurtPart(int index, DamageSource source, float amount) {
        return hurtSelectedPart(index, source, amount);
    }

    /** Count damage to each breakable part through the shared multipart damage path. */
    @Override
    protected boolean hurtSelectedPart(int partIndex, DamageSource source, float amount) {
        float healthBefore = getHealth();
        boolean applied = super.hurtSelectedPart(partIndex, source, amount);
        float dealt = healthBefore - getHealth();
        if (applied && dealt > 0) {
            int group = durabilityGroup(partIndex);
            if (group >= 0 && !brokenParts[group]) {
                // Head durability counts actual health lost, including its vulnerability multiplier.
                // Keep the existing unmultiplied durability rules for limbs and tail.
                float effective = dealt;
                if (group != 0) {
                    float multiplier = damageMultiplierForPart(partIndex);
                    float armor = (float) getArmorValue();
                    float toughness = (float) getAttributeValue(Attributes.ARMOR_TOUGHNESS);
                    float withMultiplier = source.is(DamageTypeTags.BYPASSES_ARMOR) ? amount * multiplier
                            : net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(
                                    this, amount * multiplier, source, armor, toughness);
                    float withoutMultiplier = source.is(DamageTypeTags.BYPASSES_ARMOR) ? amount
                            : net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(
                                    this, amount, source, armor, toughness);
                    effective = withMultiplier > 0 ? dealt * withoutMultiplier / withMultiplier : 0;
                }
                partDurabilityDamage[group] += effective;
                float threshold = group == 0 ? 2000F : getMaxHealth() * BREAK_THRESHOLDS[group];
                if (partDurabilityDamage[group] >= threshold) {
                    brokenParts[group] = true;
                    entityData.set(BROKEN_MASK, entityData.get(BROKEN_MASK) | (1 << group));
                    if (group == 0) entityData.set(HEAD_BROKEN, true);
                }
            }
        }
        return applied;
    }

    /** Validate reach for the client's selected OBB part without re-casting a different server frame. */
    public int resolveMeleeStrike(Player player, int requested) {
        // The client's pick is authoritative, exactly as it is for a vanilla mob.
        //
        // A server-side ray was tried here and had to be removed: measured over 76 attacks it
        // overrode the client 26 times, and the "corrected" part was 6 to 67 indices away from
        // the one aimed at. The two rays are simply not the same ray — the client samples the
        // animation with a different partial tick and predicts its own position, while the
        // server sees a position that lags a tick behind a moving player. Overriding the pick
        // with that ray is what produced the "each swing hits a random part" behaviour.
        //
        // Reach is guarded twice: vanilla drops the attack packet entirely when the named part
        // fails Player#canInteractWithEntity, and the wide clamp below covers code paths that
        // call hurt() directly.
        double limit = player.entityInteractionRange() + 1.0 + MELEE_OUT_OF_RANGE_MARGIN;
        if (!canPlayerReachPartWithin(player, requested, limit)) return -1;
        return requested;
    }

    /** Margin beyond the vanilla interaction limit that still counts as a legitimate hit. */
    private static final double MELEE_OUT_OF_RANGE_MARGIN = 8.0;

    /** Reach test against an explicit limit, so callers can choose their own threshold. */
    public boolean canPlayerReachPartWithin(Player player, int index, double limit) {
        if (index < 0 || index >= worldParts.length) return false;
        if (worldParts[index].getOrientedBox() == null) return false;
        Vec3 eye = player.getEyePosition();
        return worldParts[index].getBoundingBox().distanceToSqr(eye) <= limit * limit;
    }

    /**
     * 有真实身体部件会撞进方块时，取消这一步移动。
     *
     * <p>用"穿透深度超过 {@link #POSE_CLEARANCE_TOLERANCE}" 而不是布尔相交：龙站在地面上时
     * 脚底与地面方块顶面在浮点上必然重叠一丁点，布尔判定会让**每一步都被取消**——
     * 表现就是"朝向前方却几乎不动/横向漂"。容差只滤浮点噪声，真实遮挡远超它。
     */
    @Override
    public void move(MoverType type, Vec3 delta) {
        if (!delta.equals(Vec3.ZERO) && !level().isClientSide && !noPhysics) {
            for (GraveDragonPartEntity part : worldParts) {
                // Only blocks stop the dragon. Other child hitboxes belong to
                // this same dragon and must never cancel movement/knockback.
                var box = part.getOrientedBox().move(delta);
                if (penetrationIntoBlocks(box) > POSE_CLEARANCE_TOLERANCE) {
                    super.move(type, Vec3.ZERO);
                    return;
                }
            }
        }
        super.move(type, delta);
    }

    /** Tick poses drive server physics and initialise client parts before the first render. */
    private void updateDragonParts() {
        updatePartPose(poseAt(animationSeconds(0)),
                yBodyRot, position());
    }

    void refreshCombatPose() { updateDragonParts(); }

    boolean combatPoseFits(String animation, double... seconds) {
        for (double time : seconds)
            if (!combatPoseFitsAt(animation, time, yBodyRot, bodyPitch(), position())) return false;
        return true;
    }

    boolean combatPoseFitsAt(String animation, double seconds, float yaw, float pitch, Vec3 position) {
        return combatPoseFitsAt(animation, seconds, yaw, pitch, position, Double.NEGATIVE_INFINITY);
    }

    boolean combatPoseFitsAt(String animation, double seconds, float yaw, float pitch, Vec3 position, double contactFloor) {
        Vec3 origin = position.add(0, POSE_CLEARANCE_LIFT, 0);
        var transform = GraveDragonPose.modelToEntity(yaw, pitch, getScale());
        var frame = GraveDragonPose.sample(animation, seconds, false);
        for (int i = 0; i < PART_LABELS.length; i++) {
            var box = GraveDragonPose.box(frame, PART_LABELS[i], PART_BOUNDS[i], transform, origin);
            if (penetrationIntoBlocks(box, contactFloor) > POSE_CLEARANCE_TOLERANCE) return false;
        }
        return true;
    }

    private void updatePartPose(GraveDragonPose.Frame frame, float yaw, Vec3 origin) {
        // 俯仰必须和渲染用同一个角，否则龙首一斜，碰撞箱就和模型错开。
        var transform = GraveDragonPose.modelToEntity(yaw, bodyPitch(), getScale());
        for (int i = 0; i < worldParts.length; i++) {
            worldParts[i].setOrientedBox(GraveDragonPose.box(frame, PART_LABELS[i], PART_BOUNDS[i], transform, origin));
        }
    }

    /** Refresh before client picking, which runs before the model is rendered. */
    public void updateClientPartPose(float partialTick) {
        updateClientPartPose(partialTick, poseAt(animationSeconds(partialTick)));
    }

    /** Reuse the exact bone frame applied to the visible model. */
    public void updateClientPartPose(float partialTick, GraveDragonPose.Frame frame) {
        if (!level().isClientSide) return;
        Vec3 origin = new Vec3(Mth.lerp(partialTick, xOld, getX()), Mth.lerp(partialTick, yOld, getY()), Mth.lerp(partialTick, zOld, getZ()));
        var transform = GraveDragonPose.modelToEntity(Mth.rotLerp(partialTick, yBodyRotO, yBodyRot),
                bodyPitch(partialTick), getScale());
        for (int i = 0; i < worldParts.length; i++)
            worldParts[i].setOrientedBox(GraveDragonPose.box(frame, PART_LABELS[i], PART_BOUNDS[i], transform, origin));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10000.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ARMOR, 200)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 80.0);
    }

    @Override
    protected void registerGoals() {
        // 移动、转向与选招由 combat.tick 驱动。
    }

    @Override
    protected float tickHeadTurn(float yaw, float animationStep) {
        // Combat owns yaw. Vanilla's delayed head-follow otherwise rotates the
        // client model/OBBs differently from the server during short claw attacks.
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        return animationStep;
    }

    @Override
    public void tick() {
        // Instance sessions are rebuilt after a restart; never retain an orphaned,
        // invulnerable cinematic dragon in a previously occupied slot.
        if (!level().isClientSide && getPersistentData().hasUUID("ShiHuangOwner")
                && InstanceManager.sessionFor(this) == null) {
            discard();
            return;
        }
        if (animation().startsWith("escape_")) {
            setDeltaMovement(Vec3.ZERO);
            setNoGravity(true);
            super.tick();
            bodyPitchO = bodyPitch();
            updateDragonParts();
            if (!level().isClientSide) bossEvent.setProgress(getHealth() / getMaxHealth());
            return;
        }
        if (entityData.get(FROZEN)) setDeltaMovement(Vec3.ZERO);
        if (!level().isClientSide && spawnPosition == null) spawnPosition = position();
        if (!level().isClientSide && introComplete) {
            Vec3 velocity = getDeltaMovement();
            setDeltaMovement(0, flying() ? 0 : velocity.y, 0);
        }
        super.tick();
        // 先记下上一 tick 的俯仰供渲染插值，再由服务端按新的速度方向更新（客户端用同步值）。
        this.bodyPitchO = bodyPitch();
        if (entityData.get(FROZEN) || !level().isClientSide && introComplete && isNoAi()) {
            setDeltaMovement(Vec3.ZERO);
            updateDragonParts();
            if (!level().isClientSide) bossEvent.setProgress(getHealth() / getMaxHealth());
            return;
        }
        if (!this.level().isClientSide) {
            if (!introComplete) {
                if (!introStarted) {
                    if (!isNoAi()) {
                        introStarted = true;
                        playAnimation("into_2", true);
                        combat.announceAnimation("into_2");
                        if (level() instanceof net.minecraft.server.level.ServerLevel server)
                            for (ServerPlayer player : server.players())
                                if (player.distanceToSqr(this) <= 128 * 128) bossEvent.addPlayer(player);
                    }
                } else if (!introRoarPlayed && animationSeconds(0) >= 5.1) {
                    introRoarPlayed = true;
                    updateDragonParts();
                    Vec3 head = worldParts[11].getOrientedBox().center;
                    level().playSound(null, head.x, head.y, head.z,
                            net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL,
                            net.minecraft.sounds.SoundSource.HOSTILE, 5F, .8F);
                } else if (animationSeconds(0) >= GraveDragonPose.duration("into_2")) {
                    introComplete = true;
                    playAnimation("idle_ground", true);
                    combat.announceAnimation("idle_ground");
                    combat.onFormChanged(false);
                }
                updateDragonParts();
                bossEvent.setProgress(getHealth() / getMaxHealth());
                return;
            }
            tickForm();
            tickAnimation();
            updateBodyPitch();
        }
        updateDragonParts();
        if (this.level().isClientSide) return;
        combat.tick();
        // 同步 BossBar 血量进度
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public void kill() {
        super.kill();
        if (!level().isClientSide && isDeadOrDying() && !isRemoved())
            remove(RemovalReason.KILLED);
    }

    @Override
    public void whenFroozen() {
        if (animation().startsWith("escape_")) return;
        if (level().isClientSide || entityData.get(FROZEN)) return;
        frozenWasNoAi = isNoAi();
        entityData.set(FROZEN, true);
        setNoAi(true);
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        getNavigation().stop();
        moving = false;
        pendingForm = null;
        pendingLoopAnimation = null;
        disableAutomaticFormSwitch();
        entityData.set(BODY_PITCH, 0F);
        bodyPitchO = 0F;
        if (introStarted) introComplete = true;
        combat.interruptForFreeze();
    }

    @Override
    public void whenUnFroozen() {
        if (animation().startsWith("escape_")) return;
        if (level().isClientSide || !entityData.get(FROZEN)) return;
        entityData.set(FROZEN, false);
        setNoAi(frozenWasNoAi);
        applyFormPhysics(flying());
        combat.recoverFromFreeze();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        boolean previous = bypassHealthLock;
        bypassHealthLock = true;
        try {
            return super.hurt(source, amount);
        } finally {
            bypassHealthLock = previous;
        }
    }

    @Override
    public void setHealth(float health) {
        if (!level().isClientSide && !loadingDragonData && !bypassHealthLock && combat != null
                && !combat.retreatReady() && health <= getMaxHealth() * .5F) {
            float floor = getMaxHealth() * (combat.phaseDone() ? .05F : .5F);
            health = Math.max(health, floor);
        }
        super.setHealth(health);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("AnimationStartedAt", entityData.get(ANIMATION_START));
        tag.putString("Animation", animation());
        tag.putInt("Form", entityData.get(FORM));
        tag.putBoolean("DragonIntroStarted", introStarted);
        tag.putBoolean("DragonIntroComplete", introComplete);
        tag.putBoolean("DragonIntroRoarPlayed", introRoarPlayed);
        tag.putBoolean("DragonFrozen", entityData.get(FROZEN));
        tag.putBoolean("DragonFrozenWasNoAI", frozenWasNoAi);
        if (spawnPosition != null) {
            tag.putDouble("DragonSpawnX", spawnPosition.x);
            tag.putDouble("DragonSpawnY", spawnPosition.y);
            tag.putDouble("DragonSpawnZ", spawnPosition.z);
        }
        combat.save(tag);
        for (int i = 0; i < brokenParts.length; i++) {
            tag.putFloat("PartDamage" + i, partDurabilityDamage[i]);
            tag.putBoolean("PartBroken" + i, brokenParts[i]);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        loadingDragonData = true;
        try {
            super.readAdditionalSaveData(tag);
        } finally {
            loadingDragonData = false;
        }
        if (tag.contains("AnimationStartedAt")) entityData.set(ANIMATION_START, tag.getLong("AnimationStartedAt"));
        if (tag.contains("Animation")) entityData.set(ANIMATION, tag.getString("Animation"));
        if (tag.contains("Form")) entityData.set(FORM, tag.getInt("Form"));
        introStarted = tag.getBoolean("DragonIntroStarted");
        introComplete = tag.getBoolean("DragonIntroComplete");
        introRoarPlayed = tag.getBoolean("DragonIntroRoarPlayed");
        entityData.set(FROZEN, tag.getBoolean("DragonFrozen"));
        frozenWasNoAi = tag.getBoolean("DragonFrozenWasNoAI");
        if (tag.contains("DragonSpawnX"))
            spawnPosition = new Vec3(tag.getDouble("DragonSpawnX"), tag.getDouble("DragonSpawnY"), tag.getDouble("DragonSpawnZ"));
        if (introStarted && !introComplete) {
            playAnimation("into_2", true);
            setNoAi(false);
        }
        combat.load(tag);
        for (int i = 0; i < brokenParts.length; i++) {
            partDurabilityDamage[i] = tag.getFloat("PartDamage" + i);
            brokenParts[i] = tag.getBoolean("PartBroken" + i);
        }
        int mask = 0;
        for (int i = 0; i < brokenParts.length; i++) if (brokenParts[i]) mask |= 1 << i;
        entityData.set(BROKEN_MASK, mask);
        entityData.set(HEAD_BROKEN, brokenParts[0]);
    }

    // ===== 方法 =====

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 这个 controller 驱动每帧的 setCustomAnimations；最终骨骼姿态由共用采样器生成。
        controllers.add(new WorldTimeAnimationController<>(this, "dragon",
                () -> new WorldTimeAnimationController.Playback(animation(), animationStart(), true, animationSpeed()),
                state -> entityData.get(FROZEN) ? animationStart() : level().getGameTime() + state.getPartialTick()));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
