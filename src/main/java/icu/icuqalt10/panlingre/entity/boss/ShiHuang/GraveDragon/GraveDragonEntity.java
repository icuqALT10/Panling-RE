package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

import icu.icuqalt10.panlingre.entity.PanLingEntities;
import icu.icuqalt10.panlingre.animation.WorldTimeAnimationController;
import icu.icuqalt10.panlingre.entity.MultipartEntity;
import icu.icuqalt10.panlingre.entity.MultipartPartConfig;
import icu.icuqalt10.panlingre.entity.OrientedBoundingBox;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.entity.PartEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Set;
import net.neoforged.fml.loading.FMLPaths;

public class GraveDragonEntity extends MultipartEntity implements GeoEntity, PanLingEntities {
    /** 当前动画的起始时刻（世界时钟）。两侧共用它推算动画相位，掉线重连也不会重启动画。 */
    private static final EntityDataAccessor<Long> ANIMATION_START = SynchedEntityData.defineId(GraveDragonEntity.class, EntityDataSerializers.LONG);
    /** 当前动画名。用字符串而不是下标，因为 animationNames() 的遍历顺序不保证稳定。 */
    private static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(GraveDragonEntity.class, EntityDataSerializers.STRING);
    /** 形态：0 = 地面，1 = 空中。 */
    private static final EntityDataAccessor<Integer> FORM = SynchedEntityData.defineId(GraveDragonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> BODY_PITCH = SynchedEntityData.defineId(GraveDragonEntity.class, EntityDataSerializers.FLOAT);
    /**
     * 脊柱链的节点（"相对锚点、去掉 yaw"的偏移），服务端算好同步给客户端。
     *
     * <p>链是有状态的 verlet 积分，两侧各自跑一定会分歧；而碰撞箱与渲染必须逐位一致，
     * 所以统一由服务端推进、同步结果。没有它（地面形态/刚进世界）时退回纯动画姿态。
     */
    private static final EntityDataAccessor<CompoundTag> SPINE = SynchedEntityData.defineId(GraveDragonEntity.class, EntityDataSerializers.COMPOUND_TAG);

    /** 龙首俯仰的限幅（度）。身体水平长 46 格，再大就会大面积戳进地形。 */
    private static final float MAX_BODY_PITCH = 22.0F;
    /** 俯仰平滑系数，免得速度抖动直接传到龙首上。 */
    private static final float BODY_PITCH_SMOOTHING = 0.25F;

    /** 上一 tick 的俯仰，渲染插值用。 */
    private float bodyPitchO;

    /**
     * {@link #ANIMATION_START} 的未初始化哨兵。
     *
     * <p>判断"未初始化"只能用这个精确值，不能用"任意负数"：回拨动画时间（测试里模拟过渡播完）
     * 会让起点落到 0 以下，游戏刚开局的低 gameTime 也一样，那时必须照常计时。
     */
    private static final long UNINITIALISED_ANIMATION_START = -1L;

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANIMATION_START, UNINITIALISED_ANIMATION_START);
        builder.define(ANIMATION, "idle_air");
        builder.define(FORM, FORM_GROUND);
        builder.define(BODY_PITCH, 0.0F);
        builder.define(SPINE, new CompoundTag());
    }

    /** 地面 / 空中两种形态。 */
    public enum Form { GROUND, AIR }

    private static final int FORM_GROUND = 0;
    private static final int FORM_AIR = 1;

    /** 空中形态要求的最小离地高度，也是起飞前要检查的垂直净空（格）。 */
    private static final double FLIGHT_CLEARANCE = 12.0;
    /**
     * 飞行姿态相对锚点向下的最大深度（格，正数）。
     *
     * <p>实测（本地 {@code GraveDragonPoseExtentAudit} 量出）：{@code fly} 的 79 个碰撞箱相对锚点
     * 覆盖 Y ∈ [−10.72, +24.84]，{@code idle_air} 是 [−11.32, +24.21]。也就是说**身体会伸到锚点
     * 下方十几格**——所以"离地 10 格"是不足以让飞行姿态不插地的，判定必须按这个深度来。
     */
    private static final double FLIGHT_BODY_DEPTH = 11.5;
    /** 降落时向下探测地面的最大距离（格）。 */
    private static final double LANDING_PROBE = 160.0;
    /**
     * 起飞净空检查里每个姿态采样多少个相位。
     *
     * <p>姿态是**逐个碰撞箱**判定的（见 {@code poseFitsAt}），所以要覆盖尾巴最低、身体最开的
     * 时刻，不能只看首帧。
     */
    private static final int POSE_CLEARANCE_SAMPLES = 6;
    /** 起飞净空的采样柱沿身体轴前后偏移（格）：0、±这个值。 */
    private static final double CLEARANCE_PROBE_DISTANCE = 18.0;
    /** 起飞净空还要向上多留的高度（格）。 */
    private static final double CLEARANCE_PROBE_HEIGHT = 14.0;
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
    /** 探测地面时从锚点上方几格开始往下扫（覆盖"龙站在平台上、脚底方块就在头顶一格"的情况）。 */
    private static final int GROUND_SCAN_UP = 8;
    /** 形态切换区间：1~3 分钟。 */
    private static final int FORM_MIN_TICKS = 20 * 60;
    private static final int FORM_RANDOM_TICKS = 20 * 120;
    /** 净空不足时推迟多久再试一次。 */
    private static final int TAKEOFF_RETRY_TICKS = 20 * 5;

    /** 循环播放的动画；其余按一次性处理（播完停在末帧，由 takeoff/land/turn_* 这类过渡用）。 */
    private static final Set<String> LOOPING_ANIMATIONS = Set.of("idle_air", "idle_ground", "fly", "run");

    // ===== 无仇恨漫游 =====
    // 目前还不是敌对生物（没有攻击动画），所以不带仇恨目标：只在四周随机找个可达点过去，
    // 到达后有概率原地歇一会儿。空中与地面共用这一套，只是扩散范围与动画不同。

    /** 目标点的水平搜索距离区间（格）：地面形态。 */
    private static final double WANDER_MIN_DISTANCE = 12.0;
    /** 重新取点时至少要离开当前位置多少格（避免原地打转）。 */
    private static final double WANDER_MIN_STEP = 8.0;
    /** 空中形态的垂直扩散（格，总幅度）：±20，让漫游真的是三维的。 */
    private static final double WANDER_AIR_VERTICAL_SPREAD = 40.0;
    /**
     * 随机尝试次数：每次都用寻路验证可达性，全失败就歇一会儿。
     *
     * <p>别调大：每次尝试都是一整趟 A*，而这条龙的身体轮廓采样让每个节点要花 60 次方块查询
     * （见 {@code GraveDragonBodyPathing}），目标点又取得很远，代价是"尝试次数 × 距离"一起涨。
     */
    private static final int WANDER_ATTEMPTS = 4;
    /** 到达判定半径（格）。 */
    private static final double WANDER_ARRIVE_DISTANCE = 3.0;
    /** 单个目标点最多追多久（tick）；导航找不到路时不至于永远卡在原地。 */
    private static final int WANDER_TIMEOUT_TICKS = 20 * 30;
    /** 地面移动速度（传给 {@code moveTo}，见 {@link #wanderSpeed()} 的说明）。 */
    private static final double WANDER_SPEED = 0.7;
    /** 空中水平速度（格/tick）：直接给 {@code deltaMovement}，不再经过 moveTo 的倍率换算。 */
    private static final double FLIGHT_SPEED_PER_TICK = 0.22;
    /** 空中转向上限（度/tick）：大身体要有惯性感，不能像原版飞行控制那样一下子拧 90°。 */
    private static final float FLIGHT_TURN_RATE = 2.5F;
    /** 连续多少 tick 没位移算"撞住了"（随后抬高目标点绕行）。 */
    private static final int FLIGHT_STUCK_TICKS = 30;
    /** "挑不出目标点"之后停顿多久再试（tick）。 */
    private static final int STALL_RETRY_TICKS = 40;
    /** 空中爬升/下降速率（格/tick）。 */
    private static final double FLIGHT_CLIMB_RATE = 0.30;
    private static final double FLIGHT_DESCEND_RATE = 0.22;
    /**
     * 空中形态传给 {@code moveTo} 的速度。
     *
     * <p>{@code moveTo} 的 speed **不是**"格/秒"，而是 {@code speedModifier}：
     * {@code FlyingMoveControl} 先乘 {@code Attributes.FLYING_SPEED}（默认 0.4），
     * 再经 {@code LivingEntity#travel} 乘 0.1，所以两者差着一个数量级。
     *
     * <p>实测（{@code flightMovesAtTheConfiguredSpeed}）：2.1 时只有 0.102 格/tick（约 2 格/秒），
     * 12.0 时 0.212 格/tick（约 4.25 格/秒）——注意**不是线性**的：{@code travel} 的加速度会被
     * 摩擦与转向吃掉一部分。翼展 40 格的龙用 2 格/秒根本看不出在移动，4 格/秒才算"在飞"。
     */
    private static final double WANDER_AIR_SPEED = 12.0;
    /** 到达后原地待机的概率（%）与时长（tick）。 */
    private static final int WANDER_IDLE_CHANCE = 30;
    private static final int WANDER_IDLE_TICKS = 20 * 10;
    /**
     * 领地半径（格，水平）。
     *
     * <p>目标是"在某片区域里盘踞/盘旋"，而不是一路飞走，所以所有漫游目标点都被钳在这个圆内。
     * 比巡航距离大一些，这样它在领地内仍然有"从这头飞到那头"的行程感。
     */
    private static final double WANDER_TERRITORY_RADIUS = 48.0;
    /**
     * 空中待机的冷却（tick）：刚从 idle_air 切回 fly 之后，这段时间内不再进 idle_air。
     *
     * <p>否则 fly 飞两格就掷一次骰子，30% 的概率会让它频繁地"飞一下歇一下"，
     * 看不出在赶路。10 秒待机 + 30 秒冷却 = 空中最多每 40 秒歇一次。
     */
    private static final int AIR_IDLE_COOLDOWN_TICKS = 20 * 30;
    /** 飞行时每 tick 最多抬升多少格，用来维持离地高度。 */
    private static final double ALTITUDE_CLIMB_RATE = 0.5;
    /** 转向动画的判定阈值（度/tick）。 */
    private static final float TURN_ANIMATION_THRESHOLD = 1.2F;

    /** 漫游的当前目标点（null = 没有）。 */
    private Vec3 wanderTarget;
    /** 还要原地待机多少 tick。 */
    private int wanderIdleTicks;
    /** 是否正在移动，决定播 fly/run 还是 idle_*。 */
    private boolean moving;
    /** 漫游总开关；关掉之后 moving 不再被 tickWander 改写。 */
    private boolean wanderEnabled = true;
    /** 当前目标点已经追了多少 tick，用来做超时（导航找不到路时不至于永远卡着）。 */
    private int wanderTicks;
    /** 空中移动的过渡动画播完后要进入的循环动画（null = 没有正在过渡）。 */
    private String pendingLoopAnimation;
    /** 空中待机冷却剩余 tick；&gt;0 时 tickWander 不会进入 idle_air。 */
    private int airIdleCooldown;
    /** 脊柱链（服务端推进、同步给客户端）。两侧共用同一个 apply 反解。 */
    private final GraveDragonSpineChain spine = new GraveDragonSpineChain();
    /** 领地中心（水平）：召唤时定下，漫游目标点都钳在这个圆内。 */
    private Vec3 home;
    /** 正在走"下落第一段"（fly → idle_air），播完接着播 land。 */
    private boolean descending;
    /** 撞墙自救用的记录：上一 tick 位置与连续"没动"的 tick 数。 */
    private Vec3 lastFlightPosition = Vec3.ZERO;
    private int flightStuckTicks;
    /** "挑不出目标点"的短暂停顿剩余 tick（与待机分开计）。 */
    private int stallTicks;

    /** 服务端：下一次形态切换的时刻。 */
    private long nextFormSwitchAt;
    /** 服务端：正在过渡到哪个形态（null = 没在过渡）。过渡动画播完才真正切换。 */
    private Form pendingForm;
    /** 过渡动画期间的起止高度；位置每 tick 按动画进度插值，播完刚好到位。 */
    private double transitionFromY;
    private double transitionToY;

    /** 当前正在播放的动画名。 */
    public String animation() {
        return entityData.get(ANIMATION);
    }

    /** 当前动画已播放的秒数；{@code partialTick} 用于渲染插值。 */
    public double animationSeconds(float partialTick) {
        long start = entityData.get(ANIMATION_START);
        if (start == UNINITIALISED_ANIMATION_START) return 0;
        return Math.max(0, level().getGameTime() - start + partialTick) / 20.0;
    }

    /** 供测试与调试：当前动画的起始世界时刻。 */
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
        entityData.set(ANIMATION, name);
        entityData.set(ANIMATION_START, level().getGameTime());
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

    /**
     * Animation phase used for the collision boxes, quantised so both sides agree exactly.
     *
     * <p>The two sides sample the pose independently and their {@code getGameTime()} values are
     * not the same number at the moment each one works, so a fine-grained clock gives them
     * slightly different poses. With 79 overlapping parts, a few centimetres is enough for a
     * neighbouring box to win the pick: measured reports were "aiming at one segment damages the
     * next one" and "the far end of a chain cannot be hit at all".
     *
     * <p>Rounding the phase down to a fixed step makes the value a pure function of integers
     * that both sides share ({@code gameTime} and the synced {@link #ANIMATION_START}), so the boxes
     * come out bit-for-bit identical. The animation still plays; boxes only advance once per
     * {@link #POSE_QUANTUM_TICKS}, which is invisible over a 3.2 second cycle (13 steps).
     *
     * <p>Server-side hit resolution uses {@link #collisionPoseSeconds()} instead, which is
     * quantised the same way, so the ray is measured against exactly the boxes the client saw.
     */
    public double collisionPoseSeconds() {
        long start = entityData.get(ANIMATION_START);
        if (start == UNINITIALISED_ANIMATION_START) return 0;
        long stepped = Math.floorDiv(level().getGameTime() - start, POSE_QUANTUM_TICKS) * POSE_QUANTUM_TICKS;
        return Math.max(0, stepped) / 20.0;
    }

    // ===== 形态状态机 =====

    /** 服务端每 tick 驱动形态切换，客户端只读同步结果。 */
    private void tickForm() {
        long now = level().getGameTime();

        if (pendingForm != null) {
            // 过渡动画**播放期间**就做垂直位移：按动画进度插值高度，播完的那一刻刚好到位。
            applyTransitionLift();
            if (animationSeconds(0) >= GraveDragonPose.duration(animation())) {
                if (descending) {
                    // 第一段（fly → idle_air）结束：**不**切形态，接着播 land。
                    descending = false;
                    pendingForm = null;
                    beginLanding();
                    return;
                }
                Form target = pendingForm;
                pendingForm = null;
                // 收尾到精确高度，消掉插值残差。
                setPos(getX(), transitionToY, getZ());
                entityData.set(FORM, target == Form.AIR ? FORM_AIR : FORM_GROUND);
                applyFormPhysics(target == Form.AIR);
                playAnimation(target == Form.AIR ? "idle_air" : "idle_ground", true);
                scheduleNextFormSwitch(now);
            }
            return;
        }

        if (now < nextFormSwitchAt) return;

        if (form() == Form.AIR) {
            beginDescent();
        } else if (hasTakeoffClearance()) {
            pendingForm = Form.AIR;
            beginVerticalTransition(true, getY() + FLIGHT_CLEARANCE);
            playAnimation("takeoff", true);
        } else {
            // 当前位置放不下起飞过程：保持地面形态，过几秒换一个位置再试（漫游还在继续）。
            nextFormSwitchAt = now + TAKEOFF_RETRY_TICKS;
        }
    }

    /**
     * 落地分两段走：{@code fly → idle_air}（减速盘住），再 {@code idle_air → land}（落下）。
     *
     * <p>直接 {@code fly → land} 会出现一个 15 格的高度跳变：两个姿态的锚点高度差太大，
     * 过渡插值等于"瞬间跌下去"。分两段之后每段的姿态差都在 4 格以内，插值才像真的落下来。
     */
    private void beginDescent() {
        descending = true;
        pendingForm = Form.GROUND;
        // 只下到"起飞离地高度"：这个高度上 idle_air 的姿态刚好不插进地面，剩下的落差交给 land。
        beginVerticalTransition(false, groundLevelBelow() + FLIGHT_CLEARANCE);
        playAnimation("fly_to_idle_air", true);
    }

    /** 第二段：从巡航高度落到地面，动画播完刚好贴地。 */
    private void beginLanding() {
        pendingForm = Form.GROUND;
        beginVerticalTransition(false, groundLevelBelow());
        playAnimation("land", true);
    }

    /**
     * 记下过渡的起止高度，并临时关掉重力——否则插值抬升会和下落叠加。
     * 过渡结束后由 {@link #applyFormPhysics} 决定最终的重力状态。
     */
    private void beginVerticalTransition(boolean ascending, double targetY) {
        this.transitionFromY = getY();
        // 方向由状态机决定，不能被地面探测的边界情况反转：下降只允许往下、上升只允许往上。
        // （曾经因为 groundLevelBelow() 在测试世界里探到了比龙更高的"地面"，落地过程变成上升。）
        this.transitionToY = ascending ? Math.max(targetY, getY()) : Math.min(targetY, getY());
        // 换形态之后重新开始计冷却，否则落地形态的 tick 会把空中的冷却偷偷耗掉。
        this.airIdleCooldown = 0;
        this.setNoGravity(true);
    }

    /** 按过渡动画的播放进度插值高度。 */
    private void applyTransitionLift() {
        double duration = GraveDragonPose.duration(animation());
        double progress = duration <= 0 ? 1 : Mth.clamp(animationSeconds(0) / duration, 0, 1);
        double y = Mth.lerp(progress, transitionFromY, transitionToY);
        if (Math.abs(y - getY()) > 1.0E-6) setPos(getX(), y, getZ());
    }

    /**
     * 从当前位置向下探测可落地的地面高度；探不到（虚空）就保持原位。
     *
     * <p>这里用 {@code setPos} 直接落位而不是走 {@code move()}：{@code move()} 的逐部件判定会在
     * 身体刚碰到地面的那一刻取消整步，降落会永远卡在半空。起飞前已经检查过上方净空，
     * 降落用的是实际探测到的地面，所以直接落位是安全的。
     */
    /**
     * 当前位置下方的地面高度。
     *
     * <p>用高度图（{@code MOTION_BLOCKING}），不用向下扫方块：向下扫有两个独立的坑——
     * 龙若已经在山体内部，扫到的"地面"是它脚底那块，落地会变成原地不动甚至往上飞；
     * 而在测试世界里结构方块与方块坐标差着原点偏移，扫出来的值完全对不上（实测踩到过）。
     * 高度图给的是"这一列真正的表面"，与龙站在哪里无关。
     *
     * <p>只有高度图低于龙当前高度太多（说明它在洞穴/平台下方）时才退化成"保持原高"，
     * 避免把落点算到头顶去。
     */
    private double groundLevelBelow() {
        int height = level().getHeight(Heightmap.Types.MOTION_BLOCKING, getBlockX(), getBlockZ());
        if (height <= level().getMinBuildHeight()) return getY();
        // 只要高度图低于龙当前位置，就照它落；只有在"高度图反而在头顶"（龙在平台/洞穴下方）
        // 这种明显不可信的情况下才保持原高。
        return height <= getY() + 2.0 ? height : getY();
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

    /**
     * 暂停/恢复漫游。测试用来单独验证动画选择；后续接入攻击时也会用它让漫游让位。
     */
    public void setWanderEnabled(boolean enabled) {
        this.wanderEnabled = enabled;
        if (!enabled) {
            this.wanderTarget = null;
            this.wanderIdleTicks = 0;
            getNavigation().stop();
        }
    }

    public boolean isMoving() {
        return moving;
    }

    /**
     * 无仇恨漫游：在**领地范围内**随机挑一个可达点过去，到达后有 {@link #WANDER_IDLE_CHANCE}%
     * 概率原地待机 {@link #WANDER_IDLE_TICKS} tick，其余情况立刻找下一个点。
     *
     * <p>三条硬性约束：
     * <ul>
     *   <li>待机（idle_air / idle_ground）期间**完全不动**：导航停掉、moving 清零；</li>
     *   <li>过渡动画（takeoff / land / 空中待机过渡）播放期间不规划新移动；</li>
     *   <li>连续多次挑不出可达点就停下来歇一小会儿再试，避免每 tick 都跑 A*。</li>
     * </ul>
     */
    private void tickWander() {
        if (!wanderEnabled) return;
        if (airIdleCooldown > 0) airIdleCooldown--;

        // 待机：彻底站住，不走导航也不规划新目标。
        if (wanderIdleTicks > 0) {
            wanderIdleTicks--;
            getNavigation().stop();
            setMoving(false);
            if (flying()) hover();
            return;
        }

        // "挑不出目标点"的短暂停顿：同样站住，但不算待机（不播 idle_air，也不上冷却）。
        if (stallTicks > 0) {
            stallTicks--;
            getNavigation().stop();
            setMoving(false);
            if (flying()) hover();
            return;
        }

        // 过渡动画播放期间不规划新移动：位置由 applyTransitionLift 直接控制。
        if (pendingForm != null) {
            getNavigation().stop();
            setMoving(false);
            if (flying()) hover();
            return;
        }

        boolean arrived = wanderedTarget()
                || position().distanceToSqr(wanderTarget) < WANDER_ARRIVE_DISTANCE * WANDER_ARRIVE_DISTANCE
                || wanderTicks > WANDER_TIMEOUT_TICKS;
        wanderTicks++;
        if (!arrived) {
            if (flying()) tickFlightWander();
            else setMoving(true);
            return;
        }

        // 到达：先掷一次骰子决定要不要歇。
        // 空中待机还有冷却：刚从 idle_air 切回 fly 之后 30 秒内不再进 idle_air，
        // 否则飞两下就歇一下，看不出在赶路。
        if (wanderTarget != null && airIdleCooldown <= 0 && random.nextInt(100) < WANDER_IDLE_CHANCE) {
            wanderTarget = null;
            wanderIdleTicks = WANDER_IDLE_TICKS;
            if (flying()) airIdleCooldown = AIR_IDLE_COOLDOWN_TICKS;
            getNavigation().stop();
            setMoving(false);
            return;
        }

        Vec3 next = pickWanderTarget();
        if (next == null) {
            // 连续几次都挑不出可达点（被围住 / 空间不够）：停下来歇一小会儿再试，
            // 而不是每 tick 重新跑 A*。
            // **不占用 wanderIdleTicks**：那是"待机"状态，会被 isWanderIdle() 报出去，
            // 跟"挑不出点"完全是两码事（混用会让人把停顿误读成进入 idle_air）。
            wanderTarget = null;
            stallTicks = STALL_RETRY_TICKS;
            setMoving(false);
            return;
        }
        wanderTarget = next;
        wanderTicks = 0;
        if (flying()) {
            // 空中不走原版飞行导航：那条路会反复把路径算成 null/立刻做完，龙就僵在原地。
            // 直接操纵速度更可控，转向也能平滑。
            tickFlightWander();
        } else {
            getNavigation().moveTo(next.x, next.y, next.z, wanderSpeed());
            setMoving(true);
        }
    }

    // ===== 空中飞行转向 =====

    /**
     * 空中漫游的转向与速度，**不经过** {@code PathNavigation}。
     *
     * <p>为什么不用原版飞行导航：实测 {@code FlyingMoveControl} 会在路径被重算成 null 之后
     * 停在原地（{@code hasWanted=false}、{@code path=null} 却还在天上），而且它的
     * {@code rotlerp(..., 90f)} 会把朝向瞬间拧到目标方向——55 格长的身体看起来就是"猛地摆头"，
     * 非常僵硬。这里改成自己算：
     * <ul>
     *   <li>朝向按 {@link #FLIGHT_TURN_RATE} 度/tick 平滑转过去（大身体才有惯性感）；</li>
     *   <li>水平速度直接给 {@link #WANDER_AIR_SPEED} 对应的格/tick；</li>
     *   <li>高度用 {@link #maintainFlightAltitude()} 兜底"离地 ≥ {@link #FLIGHT_CLEARANCE}"，
     *       目标点更高就爬升、更低就缓降，升降率都受限。</li>
     * </ul>
     * 避障交给 {@code move()} 的逐部件判定：真的撞上就停住，而不是穿模。
     */
    private void tickFlightWander() {
        if (wanderTarget == null) return;

        // 领地硬边界兜底：目标点本身是钳在领地圆内的，但飞行有惯性（转向率有限），
        // 转弯时可能冲出去。真出界了就把水平位置拉回边界上——这条龙是"盘踞在某片区域"，
        // 不该越飞越远。
        Vec3 home = homePosition();
        double dx = getX() - home.x, dz = getZ() - home.z;
        double drift = Math.hypot(dx, dz);
        if (drift > WANDER_TERRITORY_RADIUS) {
            double scale = WANDER_TERRITORY_RADIUS / drift;
            setPos(home.x + dx * scale, getY(), home.z + dz * scale);
        }

        // 撞墙自救：贴着山体/树冠时 move() 会把整步否掉，表现就是"卡在原地不动"。
        // 连续 {@link #FLIGHT_STUCK_TICKS} tick 几乎没有位移就抬高目标点爬过去；
        // 再卡就干脆换一个目标点，别一直顶着墙。
        if (wanderTarget.distanceToSqr(position()) > 1.0E-4) {
            double progress = position().distanceToSqr(lastFlightPosition);
            lastFlightPosition = position();
            if (progress < 0.0025) { // 0.05 格/tick 以下算没动
                flightStuckTicks++;
                if (flightStuckTicks > FLIGHT_STUCK_TICKS * 2) {
                    // 抬高也过不去：换成**侧向绕行**，而不是清空目标点停下。
                    // 清空目标会让接下来 STALL_RETRY_TICKS 完全静止——自救动作本身变成"卡住"，
                    // 这正是之前测到的那几个"完全不动的时间窗"（实测确认）。
                    double yaw = Math.toRadians(getYRot() + (random.nextBoolean() ? 90 : -90));
                    wanderTarget = position().add(Math.sin(yaw) * 24.0, 6.0, Math.cos(yaw) * 24.0);
                    flightStuckTicks = 0;
                    return;
                }
                if (flightStuckTicks > FLIGHT_STUCK_TICKS) {
                    // 抬高 12 格从上面绕过去。
                    wanderTarget = new Vec3(wanderTarget.x, wanderTarget.y + 12.0, wanderTarget.z);
                    return;
                }
            } else {
                flightStuckTicks = 0;
            }
        }

        Vec3 to = wanderTarget.subtract(position());
        double horizontal = Math.hypot(to.x, to.z);
        if (horizontal > 1.0E-3) {
            // MC 的 yaw：0 面向 +Z、90 面向 -X、yaw 增大是向右转，
            // 所以朝向 (dx, dz) 的 yaw = atan2(dx, dz)。
            // 写成 atan2(-dx, dz) 会让龙**倒着飞**（朝向与位移相反），这一点实测确认过。
            float wanted = (float) Math.toDegrees(Math.atan2(to.x, to.z));
            float diff = Mth.wrapDegrees(wanted - getYRot());
            float step = Mth.clamp(diff, -FLIGHT_TURN_RATE, FLIGHT_TURN_RATE);
            setYRot(getYRot() + step);
            setYHeadRot(getYRot());
            yBodyRot = getYRot();
            yBodyRotO = yBodyRot;
        }

        double yaw = Math.toRadians(getYRot());
        double speed = FLIGHT_SPEED_PER_TICK;
        double dy = 0;
        double wantedY = wanderTarget.y;
        double lowest = groundLevelBelow() + FLIGHT_CLEARANCE;
        if (wantedY < lowest) wantedY = lowest;
        double climb = wantedY - getY();
        if (Math.abs(climb) > 0.35) dy = Mth.clamp(climb, -FLIGHT_DESCEND_RATE, FLIGHT_CLIMB_RATE);

        setDeltaMovement(Math.sin(yaw) * speed, dy, Math.cos(yaw) * speed);
        setMoving(true);
    }

    /** 空中"站着"（待机）：位置不动，速度清零，只靠重力逻辑维持悬浮。 */
    private void hover() {
        setDeltaMovement(Vec3.ZERO);
    }

    /**
     * 空中速度换算成格/tick。
     *
     * <p>实测 {@code WANDER_AIR_SPEED = 12.0} 经 {@code FlyingMoveControl} 只有
     * 0.212 格/tick；这里直接给速度，所以按同一个体感取 {@link #FLIGHT_SPEED_PER_TICK}。
     */
    private double flightBlocksPerTick() {
        return FLIGHT_SPEED_PER_TICK;
    }

    /**
     * 传给 {@code PathNavigation.moveTo} 的速度。
     *
     * <p>这是个 {@code speedModifier}，不是"格/秒"：飞行时还要乘 {@code FLYING_SPEED} 属性
     * （默认 0.4）和 {@code travel} 里的 0.1，所以两者差一个数量级，
     * 否则 55 格长的龙飞起来慢到看不出在移动。实测换算见 {@link #WANDER_AIR_SPEED}。
     */
    private double wanderSpeed() {
        return flying() ? WANDER_AIR_SPEED : WANDER_SPEED;
    }

    /**
     * 在**领地范围内**向四周扩散一段距离，随机取一个点，并用**寻路**验证它真的可达。
     *
     * <p>可达性交给寻路器判断，所以不会选到身体过不去的位置；这一点对这条 55 格长的龙尤其重要。
     *
     * <p>空中是**真 3D** 的：水平四周扩散之外还有 {@link #WANDER_AIR_VERTICAL_SPREAD} 的高度变化，
     * 只有"低于离地高度"时才被钳到 {@link #FLIGHT_CLEARANCE}。垂直扩散如果太小，
     * 看起来就会像在同一个平面上绕圈。地面形态不爬坡，目标点与自身等高。
     *
     * <p>所有候选点先水平钳进以 {@link #homePosition()} 为心、半径
     * {@link #WANDER_TERRITORY_RADIUS} 的领地内，所以龙是"在某片区域里盘踞与盘旋"，
     * 而不是一路往一个方向飞走。
     */
    public Vec3 pickWanderTarget() {
        Vec3 home = homePosition();
        for (int attempt = 0; attempt < WANDER_ATTEMPTS; attempt++) {
            // 目标点直接在以**领地中心**为心的圆盘里取，而不是相对当前位置扩散。
            // 相对当前位置扩散的话，龙每到一个点都会再挑一个更远的点，最后一路飞出领地。
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = flying()
                    ? Math.sqrt(random.nextDouble()) * WANDER_TERRITORY_RADIUS
                    : WANDER_MIN_DISTANCE + random.nextDouble() * (WANDER_TERRITORY_RADIUS - WANDER_MIN_DISTANCE);
            Vec3 candidate = new Vec3(
                    home.x + Math.cos(angle) * radius,
                    home.y,
                    home.z + Math.sin(angle) * radius);
            // 太近就换一个：贴着当前位置的目标会让"到达判定"立刻成立，看起来像没动。
            if (candidate.subtract(position()).horizontalDistance() < WANDER_MIN_STEP) continue;
            if (flying()) {
                // 高度一律以**当前位置**为基准，并夹在 ±WANDER_AIR_VERTICAL_SPREAD/2 以内。
                // 不夹的话，地面探测的任何异常（实测出现过目标 Y = 203，龙在 5.4）都会变成
                // "永远爬不到的目标点"，表现就是卡在空中不动。
                double dy = (random.nextDouble() - 0.5) * WANDER_AIR_VERTICAL_SPREAD;
                double lowest = groundLevelBelow() + FLIGHT_CLEARANCE;
                double y = Mth.clamp(position().y + dy,
                        position().y - WANDER_AIR_VERTICAL_SPREAD / 2,
                        position().y + WANDER_AIR_VERTICAL_SPREAD / 2);
                candidate = new Vec3(candidate.x, Math.max(y, lowest), candidate.z);
                return candidate;
            }
            // 地面形态：与自身等高，并且必须真的走得到（地面寻路还是用原版 A*）。
            candidate = new Vec3(candidate.x, position().y, candidate.z);
            var path = getNavigation().createPath(BlockPos.containing(candidate), 1);
            if (path != null && path.canReach()) return candidate;
        }
        return null;
    }

    /** 领地中心：召唤时定下，之后固定不变。 */
    public Vec3 homePosition() {
        return home == null ? position() : home;
    }

    /** 供测试：直接指定领地中心。 */
    public void setHomePosition(Vec3 home) {
        this.home = home;
    }

    /**
     * 飞行时维持离地高度：地形升高时就抬起来，保证"自身离下方地面 ≥ 10 格"。
     * 每 tick 最多抬 {@link #ALTITUDE_CLIMB_RATE} 格，免得猛地跳一下。
     */
    private void maintainFlightAltitude() {
        if (!flying() || pendingForm != null) return;
        double lowest = groundLevelBelow() + FLIGHT_CLEARANCE;
        if (getY() < lowest - 0.05) {
            setPos(getX(), Math.min(lowest, getY() + ALTITUDE_CLIMB_RATE), getZ());
        }
    }

    /**
     * 按"形态 + 是否移动 + 是否在转向"选动画。
     *
     * <p>地面：idle_ground / run / turn_ground_left / turn_ground_right（后两者是一次性动画）。
     * 空中：idle_air / fly，两者之间要走 idle_air_to_fly / fly_to_idle_air 过渡。
     * turn_air_* 是留给后续攻击的（待机 → 转向目标 → 攻击），这里刻意不用。
     */
    private void tickAnimation() {
        if (pendingForm != null) return;

        if (pendingLoopAnimation != null) {
            if (animationSeconds(0) >= GraveDragonPose.duration(animation())) {
                String next = pendingLoopAnimation;
                pendingLoopAnimation = null;
                playAnimation(next, true);
            }
            return;
        }

        // 转向判定用"目标点方向与当前朝向的偏角"：它不依赖 tick 内的赋值时序，比朝向变化率可靠。
        float turn = desiredTurnDegrees();
        boolean turning = !flying() && moving && Math.abs(turn) > TURN_ANIMATION_THRESHOLD;
        String desired;
        if (turning) {
            // 注意命名反直觉：turn_ground_left 其实是**实体右转**的动作，turn_ground_right 才是
            // 实体左转。依据有两条，互相印证：
            //   1) 动画数据里 turn_ground_left 的 head Y 旋转是 -58°、right 是 +58°，而模型空间
            //      绕 Y 正转经 modelToEntity 之后是实体左转；
            //   2) 集成测试实测 yaw 增大（实体右转）时播的就是 turn_ground_left。
            // 美术是按"观众在屏幕上看到的偏向"命名的：龙朝屏幕左边偏就叫 left。
            desired = turn > 0 ? "turn_ground_left" : "turn_ground_right";
        } else {
            desired = moving ? (flying() ? "fly" : "run") : (flying() ? "idle_air" : "idle_ground");
        }
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

    /**
     * 目标点方向与当前朝向的偏角（度）。正数表示需要向右转。
     *
     * <p>用它而不是朝向变化率：变化率依赖 tick 内 yBodyRot 的赋值时序（LivingEntity.tick 内部
     * 会同步 yBodyRotO），很容易恒为 0 或者反号。
     */
    private float desiredTurnDegrees() {
        Vec3 target = wanderTarget;
        if (target == null) return 0;
        Vec3 toTarget = target.subtract(position());
        if (toTarget.x * toTarget.x + toTarget.z * toTarget.z < 1.0E-4) return 0;
        // 与飞行转向同一个约定：yaw = atan2(dx, dz)，yaw 增大 = 实体向右转。
        // （原来写成 atan2(-dx, dz)，左右是反的：目标在 -X 时算出 +90，其实是右转 -90。）
        float wantedYaw = (float) Math.toDegrees(Math.atan2(toTarget.x, toTarget.z));
        return Mth.wrapDegrees(wantedYaw - yBodyRot);
    }

    /** 供测试：直接注入漫游目标点（跳过随机搜索）。 */
    public void setWanderTarget(Vec3 target) {
        this.wanderTarget = target;
        // 注入目标点意味着"从现在开始按这个点走"，所以把计时状态一并清掉：
        // 否则上一次"挑不出目标点"留下的 stallTicks、或待机计时会继续压着 moving=false，
        // 测试就会看到"设了目标却不动"（实测被这里误导过一轮）。
        this.stallTicks = 0;
        this.wanderIdleTicks = 0;
        this.wanderTicks = 0;
    }

    /** 是否已经"到了"目标点：水平 4 格、垂直 3 格以内（空中形态的锚点是个点，不能用球面距离）。 */
    private boolean wanderedTarget() {
        if (wanderTarget == null) return true;
        double horizontal = Math.hypot(wanderTarget.x - getX(), wanderTarget.z - getZ());
        return horizontal < 4.0 && Math.abs(wanderTarget.y - getY()) < 3.0;
    }

    public boolean isWanderIdle() {
        return wanderIdleTicks > 0;
    }

    public int wanderIdleTicks() {
        return wanderIdleTicks;
    }

    /** 供测试：清掉待机计时，好继续验证"到达后换下一个点"的分支。 */
    public void resetWanderIdle() {
        this.wanderIdleTicks = 0;
    }

    /** 供测试：空中待机冷却剩余 tick。 */
    public int airIdleCooldownTicks() {
        return airIdleCooldown;
    }

    /** 供测试诊断：当前漫游目标（null = 没有）。 */
    public Vec3 wanderTarget() {
        return wanderTarget;
    }

    /** 供测试诊断：当前漫游目标是否为空。 */
    public boolean hasWanderTarget() {
        return wanderTarget != null;
    }

    /** 供测试诊断：漫游是否被允许。 */
    public boolean wanderEnabled() {
        return wanderEnabled;
    }

    /** 供测试诊断：单个目标点已经追了多少 tick。 */
    public int wanderTicksElapsed() {
        return wanderTicks;
    }

    /** 供测试：直接设置空中待机冷却，用来验证"冷却期内不再进 idle_air"。 */
    public void setAirIdleCooldown(int ticks) {
        this.airIdleCooldown = ticks;
    }

    private void scheduleNextFormSwitch(long now) {
        this.nextFormSwitchAt = now + FORM_MIN_TICKS + random.nextInt(FORM_RANDOM_TICKS);
    }

    /** 供测试与调试：把下一次形态切换提前到 {@code delayTicks} tick 之后。 */
    public void scheduleFormSwitchIn(int delayTicks) {
        this.nextFormSwitchAt = level().getGameTime() + delayTicks;
    }

    /**
     * 供测试与调试：把当前动画的起点往前挪，模拟"已经播了 {@code seconds} 秒"。
     *
     * <p>集成测试里连续调用 {@code tick()} 并不会推进 {@code level().getGameTime()}（那由服务端
     * 主循环负责），所以光靠连续 tick 无法让过渡动画播完，需要这个钩子。
     */
    public void backdateAnimation(double seconds) {
        entityData.set(ANIMATION_START, level().getGameTime() - (long) (seconds * 20.0));
    }

    /**
     * 供测试与调试：把动画**冻结**在 {@code seconds} 秒处（每 tick 重新对齐，相位不再前进）。
     *
     * <p>用来做"同一动画相位下对比两种姿态"的测量：否则两次测量落在动画的不同相位上，
     * idle_air 自带的头部摆动会混进差值里，分不清是姿态变化还是动画噪声。
     */
    public void freezeAnimationAt(double seconds) {
        backdateAnimation(seconds);
    }

    private void applyFormPhysics(boolean air) {
        this.setNoGravity(air);
        // 换形态就换寻路器。旧导航连同它的路径一起丢弃，新导航是干净的，不会残留上一种形态的路线。
        this.navigation = createNavigation(this.level());
    }

    /**
     * 起飞前的净空检查。
     *
     * <p>沿身体轴取锚点、前后各 {@link #CLEARANCE_PROBE_DISTANCE} 格共三根采样柱，每根要求
     * "从地面一直到 巡航高度 + {@link #CLEARANCE_PROBE_HEIGHT}" 全是空气。
     *
     * <p>曾尝试改成"把 idle_air / fly 的 79 个碰撞箱直接在目标高度摆出来逐块判定"，但那条路
     * 在本项目的几何下不稳定：飞行姿态的身体相对锚点向上下各伸出十几格，同一个高度上
     * "地面姿态判定通过、飞行姿态判定失败"，结果是"有时能起飞、有时永远不能"，比距离判据更糟。
     * 姿态版思路保留在 {@link #poseFitsAt} 里备用，但默认不启用。
     */
    private boolean hasTakeoffClearance() {
        // 起飞后锚点会停在"当前高度 + FLIGHT_CLEARANCE"上。飞行姿态的身体相对锚点向下伸出
        // FLIGHT_BODY_DEPTH 格（实测 fly 的碰撞箱最低点是 −10.72），所以离地高度必须**大于**这个
        // 深度，否则光是把姿态摆到巡航高度就已经插进地面了——这正是之前姿态版判定永远失败的原因。
        if (FLIGHT_CLEARANCE <= FLIGHT_BODY_DEPTH) return false;
        if (!poseFitsAt("fly", FLIGHT_CLEARANCE)) return false;
        if (!poseFitsAt("idle_air", FLIGHT_CLEARANCE)) return false;
        // 起点也要放得下：地面姿态此刻就在地面上。
        return poseFitsAt("idle_ground", 0.0);
    }

    /** 供测试：飞行姿态相对锚点向下的最大深度（格，正数）。 */
    public static double flightBodyDepth() {
        return FLIGHT_BODY_DEPTH;
    }

    /**
     * 沿身体轴偏移 {@code offset} 格处的一根采样柱是否满足起飞净空。
     *
     * <p>**不用 {@code level().clip}**：那个重载会把本实体当作"忽略对象"，而墓龙的身体有
     * 20 多格高，射线刚从锚点就撞上自己的身体，于是任何地方都"净空不足"、永远不起飞
     * （实测 hit 位置就在锚点上方 1 格）。这里直接逐格扫方块，不牵扯实体。
     */
    private boolean clearanceColumn(double offset) {
        double yaw = Math.toRadians(yBodyRot);
        // yaw 0 面向 +Z，此时身体轴也是 ±Z。
        double x = getX() + Math.sin(yaw) * offset;
        double z = getZ() + Math.cos(yaw) * offset;
        int bx = Mth.floor(x), bz = Mth.floor(z);
        // 采样点下方的地面必须先探到；探不到（虚空）不能起飞。
        double ground = groundLevelAt(new Vec3(x, getY(), z));
        if (Double.isNaN(ground)) return false;
        // 从地面一直扫到"巡航高度 + 身体高度"：这一段必须全是空气。
        int from = Mth.floor(ground);
        int to = Mth.floor(ground + FLIGHT_CLEARANCE + CLEARANCE_PROBE_HEIGHT);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = from; y <= to; y++) {
            if (!level().getBlockState(cursor.set(bx, y, bz)).getCollisionShape(level(), cursor).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 把 {@code pose} 姿态摆在"当前位置抬高 {@code lift} 格"处，看有没有碰撞箱插进方块。
     *
     * <p>直接构造 OBB 而不是 {@code level().clip}：那个重载会把墓龙自己当成忽略对象，而它的身体
     * 有 20 多格高，射线一出锚点就撞到自己身上，"地面"永远探在当前高度（实测确认过）。
     */
    private boolean poseFitsAt(String pose, double lift) {
        // 地面姿态的脚底在锚点**上方**约 0.05 格（OBB 表实测），龙站在地面上时脚底方块与
        // 脚掌在浮点上贴在一起，不抬这一点就会把"站在地上"判成穿透。
        Vec3 origin = new Vec3(getX(), getY() + lift + POSE_CLEARANCE_LIFT, getZ());
        double duration = GraveDragonPose.duration(pose);
        // 采样几个相位，覆盖尾巴最低 / 身体最开的时刻。
        for (int step = 0; step < POSE_CLEARANCE_SAMPLES; step++) {
            var frame = GraveDragonPose.sample(pose, duration * step / POSE_CLEARANCE_SAMPLES, false);
            for (int i = 0; i < PART_LABELS.length; i++) {
                var box = GraveDragonPose.box(frame, PART_LABELS[i], PART_BOUNDS[i],
                        GraveDragonPose.modelToEntity(yBodyRot, 0.0F, getScale()), origin);
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
        net.minecraft.world.phys.AABB envelope = box.enclosingAabb();
        double deepest = 0;
        for (var shape : level().getBlockCollisions(this, envelope)) {
            for (var block : shape.toAabbs()) {
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

    /**
     * 某个水平位置的地面高度（最高非空气方块的顶面）。
     *
     * <p>用高度图而不是向下扫方块：{@code level().clip} 会把墓龙自己当成忽略对象，而它的身体有
     * 20 多格高，射线一出锚点就撞到自己身上，于是"地面"永远探在当前高度、起飞/降落全失效
     * （实测确认过）。高度图还顺带解决"龙在平台下方"这类位置关系。
     */
    private double groundLevelAt(Vec3 from) {
        int bx = Mth.floor(from.x), bz = Mth.floor(from.z);
        int start = Mth.floor(from.y);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = start; y >= start - (int) LANDING_PROBE; y--) {
            if (!level().getBlockState(cursor.set(bx, y, bz)).getCollisionShape(level(), cursor).isEmpty()) {
                return y + 1.0;
            }
        }
        return Double.NaN;
    }

    /** 供测试：当前水平位置的地面高度（虚空返回 NaN）。 */
    public double groundLevelForTest() {
        return groundLevelAt(position());
    }

    /**
     * How often collision boxes may advance, in ticks. See {@link #collisionPoseSeconds()}.
     *
     * <p>One tick is the vanilla tick granularity, so the boxes advance every tick with no
     * extra lag and no visible stutter. The guarantee only needs both sides to round to the
     * same value, and the phase is a pure function of two integers they share — the world clock
     * and the synced {@link #ANIMATION_START} — which holds at any step size. What actually broke
     * earlier was the renderer overwriting the boxes with an interpolated frame, not the step.
     *
     * <p>Kept as a named constant rather than inlined so the value can be raised again if a
     * future change ever lets the two clocks drift apart.
     */
    private static final long POSE_QUANTUM_TICKS = 1;

    // ===== OBB 调整表：行号必须与下方 PART_LABELS 一一对应，不要单独增删/换序 =====
    // 每行前 6 项：[宽 X, 高 Y, 长 Z, 中心 X, 中心 Y, 中心 Z]。
    // 尺寸是完整边长，不是半长；单位为方块（Blockbench 的像素坐标/尺寸除以 16）。
    // 中心为模型静止姿态下的绝对坐标，不是相对骨骼 pivot 的偏移，也不是世界坐标。
    // 坐标方向沿用 BB 模型：Y 向上，嘴朝 -Z、尾朝 +Z；左右沿用骨骼 l/r 命名。
    // 前 3 项改大小，后 3 项移位置；动画、实体朝向及 getScale() 缩放由姿态代码统一应用。
    // 可选第 7~9 项：[旋转 X, 旋转 Y, 旋转 Z]，单位为度；不填即 0。
    // 旋转围绕碰撞框中心，矩阵为 Rz * Ry * Rx（对点先 X、再 Y、最后 Z）。
    // 上下嘴（12/78）保持同宽/同高/同长、同中心 X/Z，嘴缝 Y = 119.80492 / 16。
    // 调整嘴高 H 时：下嘴中心 Y = 嘴缝 Y - H/2，上嘴中心 Y = 嘴缝 Y + H/2。
    // 当前 USE_EXTERNAL_PART_CONFIG=false，本表直接生效；修改 Java 后须重新启动游戏。
    private static final float[][] HARD_CODED_PART_BOUNDS = {
            // [00] neck_01：躯干/颈部第 01 节；宽, 高, 长, 中心X, 中心Y, 中心Z
            {2.16125f, 1.959375f, 3.25f, 0f, 7.7351811438f, 14.875f},
            // [01] neck_02：躯干/颈部第 02 节；宽, 高, 长, 中心X, 中心Y, 中心Z
            {2.16125f, 1.959375f, 3.25f, 0f, 7.7351811438f, 11.875f},
            // [02] neck_03：躯干/颈部第 03 节；宽, 高, 长, 中心X, 中心Y, 中心Z
            {2.16125f, 1.959375f, 3.25f, 0f, 7.7351811438f, 8.875f},
            // [03] neck_04：躯干/颈部第 04 节；宽, 高, 长, 中心X, 中心Y, 中心Z
            {2.16125f, 1.959375f, 3.25f, 0f, 7.7351811438f, 5.875f},
            // [04] neck_05：躯干/颈部第 05 节；宽, 高, 长, 中心X, 中心Y, 中心Z
            {2.16125f, 1.959375f, 3.25f, 0f, 7.7351811438f, 2.875f},
            // [05] neck_06：躯干/颈部第 06 节；宽, 高, 长, 中心X, 中心Y, 中心Z
            {2.16125f, 1.959375f, 3.25f, 0f, 7.7351811438f, -0.125f},
            // [06] neck_07：躯干/颈部第 07 节；宽, 高, 长, 中心X, 中心Y, 中心Z
            {2.16125f, 1.959375f, 3.25f, 0f, 7.7351811438f, -3.125f},
            // [07] neck_08：躯干/颈部第 08 节；宽, 高, 长, 中心X, 中心Y, 中心Z
            {2.16125f, 1.959375f, 3.25f, 0f, 7.7351811438f, -6.125f},
            // [08] neck_09：躯干/颈部第 09 节；宽, 高, 长, 中心X, 中心Y, 中心Z
            {2.1715000875f, 1.959375f, 3.27195055f, -0.01626625f, 7.7351811438f, -9.124911875f},
            // [09] neck_10：躯干/颈部第 10 节；宽, 高, 长, 中心X, 中心Y, 中心Z
            {2.1715000875f, 1.959375f, 3.27195055f, -0.01626625f, 7.7351811438f, -12.124911875f},
            // [10] neck_11：躯干/颈部第 11 节；宽, 高, 长, 中心X, 中心Y, 中心Z
            {2.1715000875f, 1.959375f, 3.27195055f, -0.01626625f, 7.7351811438f, -15.124911875f},
            // [11] head：龙头主体（不含嘴、角和鬃毛）；宽, 高, 长, 中心X, 中心Y, 中心Z
            {3.97826388f, 3.53500841f, 4.25518141f, -0.05112175f, 8.30309733f, -19.23630925f},
            // [12] jaw：下嘴（随 jaw 骨骼）；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.53125000f, 1.00000000f, 3.43343688f, -0.04983688f, 6.98780750f, -22.14738031f},
            // [13] tail_01：尾巴第 01 节；宽, 高, 长, 中心X, 中心Y, 中心Z
            {2.16125f, 1.959375f, 3.25f, 0f, 7.7351811438f, 17.875f},
            // [14] tail_02：尾巴第 02 节；宽, 高, 长, 中心X, 中心Y, 中心Z
            {2.16125f, 1.959375f, 3.25f, 0f, 7.7351811438f, 20.875f},
            // [15] tail_03：尾巴第 03 节；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.9925f, 1.815625f, 3.25f, 0f, 7.7508061438f, 23.875f},
            // [16] tail_04：尾巴第 04 节；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.7675f, 1.584375f, 3.25f, 0f, 7.7508061438f, 26.875f},
            // [17] tail_05：尾巴第 05 节；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.48625f, 1.296875f, 3.25f, 0f, 7.7539311438f, 29.875f},
            // [18] tail_06：尾巴第 06 节；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.14875f, 0.95f, 3.25f, 0f, 7.7554936438f, 32.875f},
            // [19] tail_07：尾巴第 07 节；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.81125f, 0.6375f, 3.25f, 0f, 7.7398686438f, 35.875f},
            // [20] tail_tip：尾尖；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.4375f, 0.4375f, 3.125f, 0f, 7.7711186438f, 38.8125f},
            // [21] neck_joint：头颈连接段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {2.0275646813f, 1.959375f, 2.52199655f, -0.047760625f, 7.7351811438f, -17.999735625f},
            // [22] front_l_upper_a：左前肢上肢近身段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.5f, 2.3203126782f, 1.5f, -1.5f, 7.3609623016f, -0.1875f},
            // [23] front_l_upper_b：左前肢上肢远身段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.5f, 2.3203126782f, 1.5f, -1.5f, 5.0406496234f, -0.1875f},
            // [24] front_l_forearm_a：左前肢前臂近身段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.125f, 1.78125f, 1f, -1.500000425f, 3.1377341838f, -0.1875001563f},
            // [25] front_l_forearm_b：左前肢前臂近爪段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.125f, 1.78125f, 1f, -1.500000425f, 1.3564841838f, -0.1875001563f},
            // [26] front_l_hand：左前肢掌部；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.6875f, 0.5625f, 1.3125f, -1.5f, 0.28125f, -0.5f},
            // [27] front_l_digit_1：左前肢第 1 趾根段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.8945518875f, 0.375f, 1.3480108188f, -2.308593125f, 0.1875f, -1.688514375f},
            // [28] front_l_digit_1_tip：左前肢第 1 趾尖段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.761376925f, 0.25f, 1.418891775f, -2.8269226813f, 0.125f, -2.8832537063f},
            // [29] front_l_digit_2：左前肢第 2 趾根段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.375f, 0.375f, 1.5f, -1.5f, 0.1875f, -1.84375f},
            // [30] front_l_digit_2_tip：左前肢第 2 趾尖段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.25f, 0.25f, 1.5f, -1.5f, 0.125f, -3.28125f},
            // [31] front_l_digit_3：左前肢第 3 趾根段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.9737928125f, 0.375f, 1.5179435313f, -0.65178625f, 0.1875f, -1.773480625f},
            // [32] front_l_digit_3_tip：左前肢第 3 趾尖段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.761376925f, 0.25f, 1.4188924f, -0.0938360688f, 0.125f, -3.0531865188f},
            // [33] front_l_thumb：左前肢拇指根段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.375f, 0.375f, 1.5f, -1.46875f, 0.1875f, 0.84375f},
            // [34] front_l_thumb_tip：左前肢拇指尖段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.25f, 0.25f, 1.5f, -1.46875f, 0.125f, 2.28125f},
            // [35] front_r_upper_a：右前肢上肢近身段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.5f, 2.32031245f, 1.5f, 1.5f, 7.3609623063f, -0.1875f},
            // [36] front_r_upper_b：右前肢上肢远身段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.5f, 2.32031245f, 1.5f, 1.5f, 5.0406500813f, -0.1875f},
            // [37] front_r_forearm_a：右前肢前臂近身段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.125f, 1.78125f, 1f, 1.5000000188f, 3.1377344088f, -0.1875000188f},
            // [38] front_r_forearm_b：右前肢前臂近爪段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.125f, 1.78125f, 1f, 1.5000000188f, 1.3564844088f, -0.1875000188f},
            // [39] front_r_hand：右前肢掌部；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.6875f, 0.5625f, 1.3125f, 1.5f, 0.28125f, -0.5f},
            // [40] front_r_digit_1：右前肢第 1 趾根段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.8945518875f, 0.375f, 1.3480108188f, 0.691406875f, 0.1875f, -1.688514375f},
            // [41] front_r_digit_1_tip：右前肢第 1 趾尖段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.761376925f, 0.25f, 1.4188924f, 0.1730773188f, 0.125f, -2.8832540188f},
            // [42] front_r_digit_2：右前肢第 2 趾根段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.375f, 0.375f, 1.5f, 1.5f, 0.1875f, -1.84375f},
            // [43] front_r_digit_2_tip：右前肢第 2 趾尖段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.25f, 0.25f, 1.5f, 1.5f, 0.125f, -3.28125f},
            // [44] front_r_digit_3：右前肢第 3 趾根段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.9737928125f, 0.375f, 1.5179435313f, 2.34821375f, 0.1875f, -1.77348125f},
            // [45] front_r_digit_3_tip：右前肢第 3 趾尖段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.7896990438f, 0.25f, 1.432099225f, 2.8920028688f, 0.125f, -3.0465831063f},
            // [46] front_r_thumb：右前肢拇指根段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.375f, 0.375f, 1.5f, 1.53125f, 0.1875f, 0.84375f},
            // [47] front_r_thumb_tip：右前肢拇指尖段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.25f, 0.25f, 1.5f, 1.53125f, 0.125f, 2.28125f},
            // [48] hind_l_upper_a：左后肢上肢近身段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.5f, 2.3203126782f, 1.5f, -1.5f, 7.3609623016f, 20.8125f},
            // [49] hind_l_upper_b：左后肢上肢远身段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.5f, 2.3203126782f, 1.5f, -1.5f, 5.0406496234f, 20.8125f},
            // [50] hind_l_forearm_a：左后肢前臂近身段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.125f, 1.78125f, 1f, -1.500000425f, 3.1377341838f, 20.8124998438f},
            // [51] hind_l_forearm_b：左后肢前臂近爪段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.125f, 1.78125f, 1f, -1.500000425f, 1.3564841838f, 20.8124998438f},
            // [52] hind_l_hand：左后肢掌部；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.6875f, 0.5625f, 1.3125f, -1.5f, 0.28125f, 20.5f},
            // [53] hind_l_digit_1：左后肢第 1 趾根段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.8945518875f, 0.375f, 1.3480108188f, -2.308593125f, 0.1875f, 19.311485625f},
            // [54] hind_l_digit_1_tip：左后肢第 1 趾尖段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.761376925f, 0.25f, 1.418891775f, -2.8269226813f, 0.125f, 18.1167462938f},
            // [55] hind_l_digit_2：左后肢第 2 趾根段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.375f, 0.375f, 1.5f, -1.5f, 0.1875f, 19.15625f},
            // [56] hind_l_digit_2_tip：左后肢第 2 趾尖段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.25f, 0.25f, 1.5f, -1.5f, 0.125f, 17.71875f},
            // [57] hind_l_digit_3：左后肢第 3 趾根段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.9737928125f, 0.375f, 1.5179435313f, -0.65178625f, 0.1875f, 19.226519375f},
            // [58] hind_l_digit_3_tip：左后肢第 3 趾尖段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.761376925f, 0.25f, 1.4188924f, -0.0938360688f, 0.125f, 17.9468134813f},
            // [59] hind_l_thumb：左后肢拇指根段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.375f, 0.375f, 1.5f, -1.46875f, 0.1875f, 21.84375f},
            // [60] hind_l_thumb_tip：左后肢拇指尖段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.25f, 0.25f, 1.5f, -1.46875f, 0.125f, 23.28125f},
            // [61] hind_r_upper_a：右后肢上肢近身段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.5f, 2.32031245f, 1.5f, 1.5f, 7.3609623063f, 20.8125f},
            // [62] hind_r_upper_b：右后肢上肢远身段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.5f, 2.32031245f, 1.5f, 1.5f, 5.0406500813f, 20.8125f},
            // [63] hind_r_forearm_a：右后肢前臂近身段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.125f, 1.78125f, 1f, 1.5000000188f, 3.1377344088f, 20.8124999813f},
            // [64] hind_r_forearm_b：右后肢前臂近爪段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.125f, 1.78125f, 1f, 1.5000000188f, 1.3564844088f, 20.8124999813f},
            // [65] hind_r_hand：右后肢掌部；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.6875f, 0.5625f, 1.3125f, 1.5f, 0.28125f, 20.5f},
            // [66] hind_r_digit_1：右后肢第 1 趾根段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.8945518875f, 0.375f, 1.3480108188f, 0.691406875f, 0.1875f, 19.311485625f},
            // [67] hind_r_digit_1_tip：右后肢第 1 趾尖段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.761376925f, 0.25f, 1.4188924f, 0.1730773188f, 0.125f, 18.1167459813f},
            // [68] hind_r_digit_2：右后肢第 2 趾根段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.375f, 0.375f, 1.5f, 1.5f, 0.1875f, 19.15625f},
            // [69] hind_r_digit_2_tip：右后肢第 2 趾尖段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.25f, 0.25f, 1.5f, 1.5f, 0.125f, 17.71875f},
            // [70] hind_r_digit_3：右后肢第 3 趾根段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.9737928125f, 0.375f, 1.5179435313f, 2.34821375f, 0.1875f, 19.22651875f},
            // [71] hind_r_digit_3_tip：右后肢第 3 趾尖段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.761376925f, 0.25f, 1.4188924f, 2.9061639313f, 0.125f, 17.9468134813f},
            // [72] hind_r_thumb：右后肢拇指根段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.375f, 0.375f, 1.5f, 1.46875f, 0.1875f, 21.84375f},
            // [73] hind_r_thumb_tip：右后肢拇指尖段；宽, 高, 长, 中心X, 中心Y, 中心Z
            {0.25f, 0.25f, 1.5f, 1.46875f, 0.125f, 23.28125f},
            // [74] horn_l_1：左龙角下段（根部）；宽, 高, 长, 中心X, 中心Y, 中心Z，旋转X°, Y°, Z°
            {0.67814893f, 3.88308897f, 0.67660680f, -2.07643895f, 10.22741913f, -18.03062303f, 43.63387000f, -22.22635000f, 12.52337000f},
            // [75] horn_l_2：左龙角上段（角尖方向）；宽, 高, 长, 中心X, 中心Y, 中心Z，旋转X°, Y°, Z°
            {1.66040069f, 3.99819307f, 1.41199179f, -2.98039116f, 11.89310617f, -16.64814665f, 55.75736000f, -7.08012000f, 3.10512000f},
            // [76] horn_r_1：右龙角下段（根部）；宽, 高, 长, 中心X, 中心Y, 中心Z，旋转X°, Y°, Z°
            {0.67814898f, 3.88308984f, 0.67660685f, 1.97415562f, 10.22650067f, -18.02628364f, 43.63386000f, 22.22636000f, -12.52338000f},
            // [77] horn_r_2：右龙角上段（角尖方向）；宽, 高, 长, 中心X, 中心Y, 中心Z，旋转X°, Y°, Z°
            {1.66039994f, 4.02522898f, 1.41199178f, 2.87989528f, 11.89970810f, -16.63271714f, 55.75736000f, 7.08011000f, -3.10512000f},
            // [78] upper_jaw：上嘴（随 head 骨骼）；宽, 高, 长, 中心X, 中心Y, 中心Z
            {1.53125000f, 1.00000000f, 3.43343688f, -0.04983688f, 7.98780750f, -22.14738031f}
    };
    /** Set true only when deliberately switching back to the live JSON tuner. */
    private static final boolean USE_EXTERNAL_PART_CONFIG = false;
    private static final java.nio.file.Path PART_CONFIG_FILE =
            FMLPaths.CONFIGDIR.get().resolve("panlingre/grave_dragon_parts.json");
    /**
     * Active bounds table. Package-private rather than private so the pose/drift
     * diagnostics in the same package can read the live values.
     */
    static float[][] PART_BOUNDS = HARD_CODED_PART_BOUNDS;
    private static boolean partConfigLoaded;
    private static long partConfigTimestamp = Long.MIN_VALUE;

    public static void reloadPartConfig() {
        PART_BOUNDS = USE_EXTERNAL_PART_CONFIG
                ? MultipartPartConfig.load(PART_CONFIG_FILE, HARD_CODED_PART_BOUNDS)
                : HARD_CODED_PART_BOUNDS;
        if (USE_EXTERNAL_PART_CONFIG && !java.nio.file.Files.exists(PART_CONFIG_FILE)) {
            try { MultipartPartConfig.save(PART_CONFIG_FILE, HARD_CODED_PART_BOUNDS); }
            catch (java.io.IOException ignored) { }
        }
        try { partConfigTimestamp = java.nio.file.Files.getLastModifiedTime(PART_CONFIG_FILE).toMillis(); }
        catch (java.io.IOException ignored) { partConfigTimestamp = Long.MIN_VALUE; }
        partConfigLoaded = true;
    }

    public static void saveCurrentPartConfig() throws java.io.IOException {
        MultipartPartConfig.save(PART_CONFIG_FILE, PART_BOUNDS);
    }
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
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    /** 当前正在看到这条 BossBar 的玩家，供测试与调试使用。 */
    public java.util.Collection<ServerPlayer> bossBarViewers() {
        return this.bossEvent.getPlayers();
    }

    public GraveDragonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        if (!partConfigLoaded) reloadPartConfig();
        if (!level.isClientSide) {
            // 初始为地面形态（走地、idle_ground），并立刻安排一次起飞判断——也就是说召唤出来
            // 只要头顶有空间就会马上起飞，而不是先傻站着。
            entityData.set(ANIMATION_START, level.getGameTime());
            entityData.set(ANIMATION, "idle_ground");
            entityData.set(FORM, FORM_GROUND);
            this.nextFormSwitchAt = level.getGameTime();
        }
        // The tiny root is only a locomotion anchor; OBB parts handle interaction.
        for (int i = 0; i < worldParts.length; i++) {
            worldParts[i] = new GraveDragonPartEntity(this, i);
        }
        // Same id reservation as NeoForge's EnderDragon: clients derive part ids
        // from the root spawn packet, with no independently tracked child entities.
        setId(ENTITY_COUNTER.getAndAdd(worldParts.length + 1) + 1);
        updatePartPose(GraveDragonPose.sample(0), yBodyRot, position());
        updateBodyFootprint();
        // Entity 的构造函数只把 dimensions 设成 EntityType 的默认值，refreshDimensions() 要等
        // pose 同步数据变化才会跑；所以这里显式刷一次，否则 NAME_TAG 挂点还是默认值。
        this.refreshDimensions();
        this.noPhysics = false;
        this.setNoAi(false);
        // 初始是地面形态，先吃重力；起飞过渡开始时才会临时关掉。
        this.setNoGravity(false);
    }

    @Override public boolean isMultipartEntity() { return true; }
    @Override public PartEntity<?>[] getParts() { return worldParts; }
    @Override protected Entity[] multipartParts() { return worldParts; }
    public GraveDragonPartEntity[] getWorldParts() { return worldParts; }

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

    // ===== 寻路体型 =====
    // WalkNodeEvaluator 只用 getBbWidth()/getBbHeight() 给路径节点定尺寸，而主体自己的盒子
    // 只有 1cm，于是 AI 一直在给一个 1cm 的生物规划路线，最后每一步又被 move() 的真实 OBB
    // 判定否掉——龙只会贴着墙反复尝试，不会绕路。
    //
    // 这里不改实体尺寸（那会连带改变挤压、粒子散布、MoveControl 跳跃等一大堆行为），而是给龙
    // 换一个按真实身体判定的 NodeEvaluator，见 GraveDragonPathNavigation。
    //
    // 身体范围**每个 tick 从当前 OBB 现算**（见 updateBodyFootprint），不是固定常量：颈部摆动、
    // 抬头、张嘴都会改变真实轮廓，寻路应该跟着当前姿态走。

    /** 上一 tick 量出的身体范围，坐标系是"相对锚点、已去掉 yBodyRot"的自身坐标系。 */
    private double bodyMinX, bodyMaxX, bodyMinZ, bodyMaxZ, bodyMaxY;
    private boolean bodyFootprintValid;

    public boolean bodyFootprintValid() { return bodyFootprintValid; }

    public double bodyMinX() { return bodyMinX; }

    public double bodyMaxX() { return bodyMaxX; }

    public double bodyMinZ() { return bodyMinZ; }

    public double bodyMaxZ() { return bodyMaxZ; }

    public double bodyMaxY() { return bodyMaxY; }

    @Override
    protected PathNavigation createNavigation(Level level) {
        // 两种形态的寻路器不同：走地用地面 A*，空中用原版的 3D 飞行 A*（路径天生会绕开障碍，
        // 这就是"直线被挡时把路线改成曲线"）。形态切换时会重建，见 applyFormPhysics。
        return flying() ? new GraveDragonFlightNavigation(this, level) : new GraveDragonPathNavigation(this, level);
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
        // Table order: neck_01..11, head, jaw, tail_01..07, tail_tip,
        // neck_joint, four limbs (including digits), horn_l/horn_r.
        return (index == 11 || index == 12 || index == 78) ? 2.0F
                : (index >= 13 && index <= 20 ? 0.6F : 0.8F);
    }

    public boolean hurtPart(int index, DamageSource source, float amount) {
        return hurtSelectedPart(index, source, amount);
    }

    /**
     * Every damage path (melee, projectiles, area skills) funnels through here, so this is
     * the one place to learn which part actually took the hit and how much health left with
     * it. The attacking player is told about it and gets a chat line naming the part, its
     * multiplier and the damage.
     *
     * <p>The health delta is captured right here rather than reconstructed later: the same
     * tick can carry several damage events, so a deferred snapshot compared against the
     * current health reports zero for every hit but the last.
     */
    @Override
    protected boolean hurtSelectedPart(int partIndex, DamageSource source, float amount) {
        float healthBefore = getHealth();
        boolean applied = super.hurtSelectedPart(partIndex, source, amount);
        float dealt = healthBefore - getHealth();
        if (GraveDragonDamageDebug.enabled()) {
            GraveDragonDamageDebug.log("funnel part=" + partIndex
                    + " src=" + source.getMsgId()
                    + " direct=" + (source.getDirectEntity() == null ? "null"
                            : source.getDirectEntity().getClass().getSimpleName())
                    + " in=" + amount
                    + " mult=" + damageMultiplierForPart(partIndex)
                    + " invul=" + invulnerableTime
                    + " lastHurt=" + lastHurt
                    + " applied=" + applied
                    + " hp=" + healthBefore + "->" + getHealth() + " dealt=" + dealt);
        }
        if (applied && dealt > 0) {
            // getEntity() is the attacker for every damage type (melee, thrown items, skills,
            // explosions), whereas getDirectEntity() is the projectile or null for skills, so
            // using the latter silently dropped every non-melee report.
            if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer attacker) {
                pendingReportPart = partIndex;
                pendingReportPlayer = attacker;
                pendingReportDealt = dealt;
                pendingReportRemaining = getHealth();
            }
        }
        return applied;
    }

    private int pendingReportPart = -1;
    private net.minecraft.server.level.ServerPlayer pendingReportPlayer;
    private float pendingReportDealt;
    private float pendingReportRemaining;

    /**
     * Sends the deferred hit report once the health change is final. Runs on the server
     * only, from {@link #tick()}.
     */
    private void flushHitReport() {
        if (pendingReportPart < 0 || pendingReportPlayer == null) return;
        int part = pendingReportPart;
        net.minecraft.server.level.ServerPlayer player = pendingReportPlayer;
        float dealt = pendingReportDealt;
        float remaining = pendingReportRemaining;
        pendingReportPart = -1;
        pendingReportPlayer = null;
        if (dealt <= 0) {
            GraveDragonDamageDebug.log("report skipped, dealt=" + dealt);
            return;
        }
        GraveDragonDamageDebug.log("report part=" + part + " dealt=" + dealt
                + " remaining=" + remaining + " to=" + player.getGameProfile().getName());
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                new icu.icuqalt10.panlingre.network.GraveDragonHitPayload(
                        getId(), part, PART_LABELS[part], damageMultiplierForPart(part), dealt, remaining));
    }

    /**
     * Melee tolerance applied to the oriented boxes when the player's view ray is
     * re-cast on the server, in blocks. It only ever widens a box by a few centimetres
     * to absorb animation drift between the client's frame and the server's tick; it
     * never extends attack range.
     */
    private static final double MELEE_RAY_TOLERANCE = 0.06;

    /**
     * Which part a view ray reaches, measured against the current oriented boxes.
     *
     * <p>Kept for tests and diagnostics only. It is deliberately <em>not</em> used to resolve
     * melee: the client's pick is authoritative, see {@link #resolveMeleeStrike}.
     *
     * <p>Uses the un-interpolated eye position and look angle, because the server player has
     * not moved this tick and interpolating would aim from a position it never occupied.
     *
     * @return the part index, or -1 when the ray reaches no part
     */
    public int pickPartAlongViewRay(Player player) {
        Vec3 eye = player.getEyePosition();
        // Ask for slightly more than the attack range, then validate properly below, so
        // a part just past the raw range can still be measured and rejected on distance.
        Vec3 end = eye.add(player.getLookAngle()
                .scale(player.entityInteractionRange() + 1.0 + MELEE_RAY_TOLERANCE));
        Vec3 direction = player.getLookAngle();
        int exact = bestPartAlongRay(eye, end, direction, 0.0);
        return exact >= 0 ? exact : bestPartAlongRay(eye, end, direction, MELEE_RAY_TOLERANCE);
    }

    /**
     * The part a ray should be credited with, when several boxes overlap.
     *
     * <p>Entry distance alone is the wrong criterion here. A boss this size has overlapping
     * parts — the foreleg box reaches into the neck, the head box covers the neck joint, the
     * finger boxes overlap each other — so "whoever's surface the ray crosses first" picks a
     * neighbour instead of the part under the crosshair. Reported symptoms were exactly that:
     * aiming at one segment damaged the next one, and the last links of a chain could not be
     * hit at all because an earlier neighbour always won.
     *
     * <p>Candidates are therefore ranked by how close the crosshair passes to the part's own
     * centre, with the distance along the ray breaking ties. Aiming at a part's middle then
     * selects that part even when a neighbour's box leans into the line of sight.
     *
     * @param padding expands local faces, to absorb animation drift between frames
     */
    private int bestPartAlongRay(Vec3 from, Vec3 to, Vec3 direction, double padding) {
        int best = -1;
        double bestMissDistance = Double.MAX_VALUE;
        double bestAlongRay = Double.MAX_VALUE;
        for (int i = 0; i < worldParts.length; i++) {
            OrientedBoundingBox box = worldParts[i].getOrientedBox();
            if (box == null) continue;
            if (padding > 0) box = box.inflate(padding, padding, padding);
            var hit = box.clip(from, to);
            if (hit.isEmpty()) continue;

            // How far the ray passes from this part's centre: the part the crosshair is
            // actually pointed at has the smallest value.
            Vec3 offset = box.center.subtract(from);
            Vec3 closestOnRay = from.add(direction.scale(offset.dot(direction)));
            double missDistance = box.center.distanceToSqr(closestOnRay);
            double alongRay = hit.get().distanceToSqr(from);

            if (missDistance < bestMissDistance - 1.0e-6
                    || (Math.abs(missDistance - bestMissDistance) <= 1.0e-6 && alongRay < bestAlongRay)) {
                bestMissDistance = missDistance;
                bestAlongRay = alongRay;
                best = i;
            }
        }
        return best;
    }

    /** Whether a single part is within the player's attack range. */
    public boolean canPlayerReachPart(Player player, int index) {
        if (index < 0 || index >= worldParts.length) return false;
        if (worldParts[index].getOrientedBox() == null) return false;
        return player.canInteractWithEntity(worldParts[index].getBoundingBox(),
                MELEE_RAY_TOLERANCE);
    }

    /**
     * The single place that decides what a player's melee attack hits.
     *
     * <p>Vanilla has already validated reach for the part the client named: the server drops
     * the entire attack packet when {@code Player#canInteractWithEntity} fails for it, so a
     * hurt call can only happen for an in-range part. This method therefore trusts that
     * choice and uses its own ray only to <em>correct</em> it, never to veto it:
     * <ol>
     *   <li>Cast the server's view ray. When it reaches a different part that is itself in
     *       reach, use it. This fixes the wrong-part selections the client makes through
     *       AABB envelopes, where a large part's empty corner steals the pick from the
     *       small part under the crosshair.</li>
     *   <li>Otherwise damage the part the client named.</li>
     * </ol>
     *
     * <p>Both earlier orderings were wrong in opposite ways. Resolving the ray first let a
     * long body part further along the view line (the torso runs several blocks deep) win
     * the ray and then be rejected as out of reach, discarding a valid click on a nearby
     * limb. Re-validating the client's part with a second, differently computed reach check
     * (this one uses {@code interactionRange + 1 + tolerance} against an oriented box, where
     * vanilla uses {@code interactionRange + 1} against the part's bounding box) threw away
     * every click that landed between the two thresholds.
     *
     * @param requested part index named by the client's attack packet
     * @return the part index to damage, or -1 when the attack must be discarded
     */
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

    /** Server collision poses are evaluated once per tick; clients use the same world clock. */
    private void updateDragonParts() {
        // Collision boxes are driven from the quantised phase on both sides, so the client's
        // crosshair and the server's validation measure the very same boxes. Rendering does
        // NOT touch them: an interpolated render frame used to overwrite them, which is what
        // made the two sides disagree.
        updatePartPose(withSpine(GraveDragonPose.sample(animation(), collisionPoseSeconds(), loopingAnimation()),
                yBodyRot, position()), yBodyRot, position());
        updateBodyFootprint();
    }

    // ===== 脊柱链式跟随 =====

    /**
     * 脊柱链式跟随的**总开关**。
     *
     * <p>目前刻意关闭：链的坐标每 {@code GraveDragonSpineSync.SYNC_INTERVAL_TICKS} tick 才同步一次，
     * 而渲染每帧都读同一份快照，于是骨骼每 4 tick 跳一格——表现就是"头部瞬移闪现"，链的惯性又把
     * 这个跳变放大成全身抽搐。要真正可用，必须让两侧各自推进同一条链（而不是同步链坐标），
     * 或者对同步数据做 partialTick 插值。修好之前保持关闭，龙的行为与未接入链时完全一致。
     */
    public static final boolean SPINE_CHAIN_ENABLED = false;

    /** 服务端：每 tick 推进链。只在空中形态生效，地面形态身体本来就该贴着地面走。 */
    private void updateSpineChain() {
        if (!SPINE_CHAIN_ENABLED || !flying() || pendingForm != null) return;
        // 驱动点取**动画里龙首的实际世界位置**：链只负责把身体摆到龙头走过的轨迹后面，
        // 姿态本身仍由动画提供（withSpine 只覆盖脊柱各节的位移与指向，动画的扭动被保留）。
        Vec3 head = animatedHeadPosition(collisionPoseSeconds(), yBodyRot, position());
        if (!spine.initialised()) {
            spine.reset(head, Vec3.directionFromRotation(0, yBodyRot), new Vec3(0, 1, 0));
        } else {
            spine.update(head);
        }
        if (tickCount % GraveDragonSpineSync.SYNC_INTERVAL_TICKS == 0) {
            entityData.set(SPINE, GraveDragonSpineSync.write(localSpineOffsets()));
        }
    }

    /** 供测试/客户端：链的朝向基准（+Z 前方的 yaw 向量）。 */
    private Vec3 animatedHeadPosition(double seconds, float yaw, Vec3 origin) {
        var frame = GraveDragonPose.sample(animation(), seconds, loopingAnimation());
        var matrix = new Matrix4f(GraveDragonPose.modelToEntity(yaw, 0.0F, getScale()))
                .mul(frame.matrices().get("head"));
        Vector3f head = matrix.transformPosition(new Vector3f());
        return origin.add(head.x, head.y, head.z);
    }

    /** 把链节点换算成"相对锚点、去掉 yaw"的偏移，便于同步与插值。 */
    private Vec3[] localSpineOffsets() {
        Vec3[] nodes = new Vec3[GraveDragonPose.spineBones().size()];
        double angle = Math.toRadians(-yBodyRot);
        double cos = Math.cos(angle), sin = Math.sin(angle);
        for (int i = 0; i < nodes.length; i++) {
            Vec3 offset = spine.node(i).subtract(position());
            nodes[i] = new Vec3(offset.x * cos + offset.z * sin, offset.y, -offset.x * sin + offset.z * cos);
        }
        return nodes;
    }

    /**
     * 把同步来的链偏移套到姿态上，产出**渲染与碰撞箱共用**的那一帧。
     *
     * <p>两侧都对同一个基础姿态做同一件事，所以结果逐位一致；链没有数据（地面形态、
     * 刚进世界、或同步还没到）时原样返回基础姿态。
     */
    public GraveDragonPose.Frame withSpine(GraveDragonPose.Frame base, float yaw, Vec3 origin) {
        // 关掉时直接返回纯动画姿态，渲染与碰撞箱都走这一条，
        // 所以"所见即所得"的镜像关系不受影响。
        if (!SPINE_CHAIN_ENABLED) return base;
        Vec3[] offsets = GraveDragonSpineSync.read(entityData.get(SPINE));
        if (offsets == null || offsets.length != GraveDragonPose.spineBones().size()) return base;
        double angle = Math.toRadians(yaw);
        double cos = Math.cos(angle), sin = Math.sin(angle);
        Vec3[] world = new Vec3[offsets.length];
        for (int i = 0; i < offsets.length; i++) {
            Vec3 local = offsets[i];
            world[i] = origin.add(local.x * cos - local.z * sin, local.y, local.x * sin + local.z * cos);
        }
        // 客户端也走同一个 apply：链坐标来自同步，反解是纯函数，所以两侧结果逐位一致。
        spine.setNodes(world);
        GraveDragonPose.Frame chained = spine.apply(base, origin, GraveDragonPose.modelToEntity(yaw, 0.0F, getScale()));
        // 龙头**保留动画自身的旋转**：链只负责把身体摆到龙头走过的轨迹后面，
        // 而"抬头/低头"是动画的表达（用户明确要求头部要自我旋转，而不是被整体平移下去）。
        // 链对 head 的反解旋转会把这个表达抹掉，所以这里用动画的姿态盖回去。
        var headBase = base.bones().get(GraveDragonPose.spineBones().get(0));
        if (headBase == null) return chained;
        var headChained = chained.bones().get(GraveDragonPose.spineBones().get(0));
        java.util.Map<String, GraveDragonPose.BonePose> merged = new java.util.LinkedHashMap<>(chained.bones());
        merged.put(GraveDragonPose.spineBones().get(0),
                new GraveDragonPose.BonePose(headBase.rotation(), headChained.position(), headChained.scale()));
        return GraveDragonPose.rebuild(merged);
    }

    /** 供测试：当前同步的链节点是否可用。 */
    public boolean hasSpineSync() {
        Vec3[] offsets = GraveDragonSpineSync.read(entityData.get(SPINE));
        return offsets != null && offsets.length == GraveDragonPose.spineBones().size();
    }

    /** 供测试：服务端的链是否已经被推进过。 */
    public boolean spineInitialised() {
        return spine.initialised();
    }

    /**
     * 把这一刻的 79 个碰撞箱换算进"相对锚点、已去掉 yBodyRot"的自身坐标系，记下真实占地范围，
     * 供 {@link GraveDragonPathNavigation} 判定路径节点。
     *
     * <p>刻意**不用固定常量**：颈部摆动、抬头、张嘴都会改变身体的真实轮廓，寻路应该跟着当前
     * 姿态走，而不是跟一张量好的表走。
     *
     * <p>OBB 是按 {@code modelToEntity(yaw)} 摆的，也就是 {@code worldOffset = R_y(-yaw) · local}，
     * 所以这里用 {@code R_y(+yaw)} 反解回自身坐标系。缩放已经含在 OBB 里，不必再乘。
     */
    private void updateBodyFootprint() {
        double angle = yBodyRot * Math.PI / 180.0;
        double cos = Math.cos(angle), sin = Math.sin(angle);
        double originX = getX(), originY = getY(), originZ = getZ();
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (GraveDragonPartEntity part : worldParts) {
            OrientedBoundingBox box = part.getOrientedBox();
            if (box == null) continue;
            for (Vec3 corner : box.corners()) {
                double worldX = corner.x - originX, worldZ = corner.z - originZ;
                double localX = worldX * cos + worldZ * sin;
                double localZ = -worldX * sin + worldZ * cos;
                minX = Math.min(minX, localX);
                maxX = Math.max(maxX, localX);
                minZ = Math.min(minZ, localZ);
                maxZ = Math.max(maxZ, localZ);
                maxY = Math.max(maxY, corner.y - originY);
            }
        }
        if (minX > maxX) return;
        bodyMinX = minX;
        bodyMaxX = maxX;
        bodyMinZ = minZ;
        bodyMaxZ = maxZ;
        bodyMaxY = maxY;
        bodyFootprintValid = true;
    }

    private void updatePartPose(GraveDragonPose.Frame frame, float yaw, Vec3 origin) {
        // 俯仰必须和渲染用同一个角，否则龙首一斜，碰撞箱就和模型错开。
        var transform = GraveDragonPose.modelToEntity(yaw, bodyPitch(), getScale());
        for (int i = 0; i < worldParts.length; i++) {
            worldParts[i].setOrientedBox(GraveDragonPose.box(frame, PART_LABELS[i], PART_BOUNDS[i], transform, origin));
        }
    }

    /**
     * Collision pose sampler shared by both sides. The renderer must not call the second
     * overload: an interpolated render frame would move the boxes to a pose the server never
     * computes, which is what previously made the two sides disagree about what the crosshair
     * was on.
     */
    public void updateClientPartPose(float partialTick) {
        if (!level().isClientSide) return;
        updatePartPose(GraveDragonPose.sample(animation(), collisionPoseSeconds(), loopingAnimation()),
                Mth.rotLerp(partialTick, yBodyRotO, yBodyRot), position());
    }

    /** Kept for the pose regression test, which drives poses explicitly. */
    public void updateClientPartPose(float partialTick, GraveDragonPose.Frame frame) {
        if (!level().isClientSide) return;
        Vec3 origin = new Vec3(Mth.lerp(partialTick, xOld, getX()), Mth.lerp(partialTick, yOld, getY()), Mth.lerp(partialTick, zOld, getZ()));
        updatePartPose(frame, Mth.rotLerp(partialTick, yBodyRotO, yBodyRot), origin);
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

    @Override
    protected void registerGoals() {
        // 目前不是敌对生物（还没有攻击动画），所以没有仇恨目标，也不挂原版的随机漫步目标：
        // 移动全部交给 tickWander，这样空中与地面共用同一套"随机可达点"逻辑。
        // turn_air_* 也是留给后续攻击的（待机 → 视角转向目标 → 发动攻击）。
    }

    @Override
    public void tick() {
        super.tick();
        // 先记下上一 tick 的俯仰供渲染插值，再由服务端按新的速度方向更新（客户端用同步值）。
        this.bodyPitchO = bodyPitch();
        if (!this.level().isClientSide) {
            // 领地中心在第一次服务端 tick 定下：之后所有漫游目标点都钳在这个圆内。
            if (home == null) home = position();
            tickForm();
            tickWander();
            tickAnimation();
            maintainFlightAltitude();
            updateBodyPitch();
            updateSpineChain();
        }
        updateDragonParts();
        if (this.level().isClientSide) return;
        flushHitReport();
        if (USE_EXTERNAL_PART_CONFIG && this.tickCount % 20 == 0) {
            try {
                long timestamp = java.nio.file.Files.getLastModifiedTime(PART_CONFIG_FILE).toMillis();
                if (timestamp != partConfigTimestamp) reloadPartConfig();
            } catch (java.io.IOException ignored) { }
        }

        // 同步 BossBar 血量进度
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("AnimationStartedAt", entityData.get(ANIMATION_START));
        tag.putString("Animation", animation());
        tag.putInt("Form", entityData.get(FORM));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AnimationStartedAt")) entityData.set(ANIMATION_START, tag.getLong("AnimationStartedAt"));
        if (tag.contains("Animation")) entityData.set(ANIMATION, tag.getString("Animation"));
        if (tag.contains("Form")) entityData.set(FORM, tag.getInt("Form"));
    }

    // ===== 方法 =====

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        if (GraveDragonPose.APPLY_ANIMATION) {
            // 这个 controller 只负责驱动 GeckoLib 每帧调用 setCustomAnimations；真正的骨骼姿态由
            // GraveDragonModel.setCustomAnimations 按当前动画名自己采样，所以这里播哪个名字不影响
            // 最终结果，跟着当前动画走只是让 GeckoLib 自己的兜底路径也正确。
            controllers.add(new WorldTimeAnimationController<>(this, "dragon",
                    () -> WorldTimeAnimationController.Playback.loop(animation(), entityData.get(ANIMATION_START)),
                    state -> level().getGameTime() + state.getPartialTick()));
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
