package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import icu.icuqalt10.panlingre.entity.PanLingEntities;
import icu.icuqalt10.panlingre.animation.WorldTimeAnimationController;
import icu.icuqalt10.panlingre.entity.MultipartEntity;
import icu.icuqalt10.panlingre.entity.MultipartPartConfig;
import icu.icuqalt10.panlingre.entity.OrientedBoundingBox;
import icu.icuqalt10.panlingre.init.ModEffects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.entity.PartEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Objects;
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
        builder.define(FORM, FORM_AIR);
        builder.define(BODY_PITCH, 0.0F);
    }

    /** 地面 / 空中两种形态。 */
    public enum Form { GROUND, AIR }

    private static final int FORM_GROUND = 0;
    private static final int FORM_AIR = 1;

    /** 空中形态要求的最小离地高度，也是起飞前要检查的垂直净空（格）。 */
    private static final double FLIGHT_CLEARANCE = 10.0;
    /** 降落时向下探测地面的最大距离（格）。 */
    private static final double LANDING_PROBE = 160.0;
    /** 形态切换区间：1~3 分钟。 */
    private static final int FORM_MIN_TICKS = 20 * 60;
    private static final int FORM_RANDOM_TICKS = 20 * 120;
    /** 净空不足时推迟多久再试一次。 */
    private static final int TAKEOFF_RETRY_TICKS = 20 * 5;

    /** 循环播放的动画；其余按一次性处理（播完停在末帧，由 takeoff/land 这类过渡用）。 */
    private static final Set<String> LOOPING_ANIMATIONS = Set.of(
            "idle_air", "idle_ground", "fly", "run", "turn_fly_left_loop", "turn_fly_right_loop");

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
            pendingForm = Form.GROUND;
            beginVerticalTransition(false);
            playAnimation("land", true);
        } else if (hasTakeoffClearance()) {
            pendingForm = Form.AIR;
            beginVerticalTransition(true);
            playAnimation("takeoff", true);
        } else {
            // 头顶空间放不下起飞过程：保持地面形态，过几秒再试（龙走到开阔地就能起飞）。
            nextFormSwitchAt = now + TAKEOFF_RETRY_TICKS;
        }
    }

    /**
     * 记下过渡的起止高度，并临时关掉重力——否则插值抬升会和下落叠加。
     * 过渡结束后由 {@link #applyFormPhysics} 决定最终的重力状态。
     */
    private void beginVerticalTransition(boolean ascending) {
        this.transitionFromY = getY();
        this.transitionToY = ascending ? getY() + FLIGHT_CLEARANCE : groundLevelBelow();
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
    private double groundLevelBelow() {
        Vec3 from = position();
        Vec3 to = from.subtract(0, LANDING_PROBE, 0);
        HitResult hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS ? getY() : hit.getLocation().y;
    }

    /** 墓龙是飞行生物：落地、以及过渡期间的程序化位移都不该造成摔落伤害。 */
    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
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

    private void applyFormPhysics(boolean air) {
        this.setNoGravity(air);
        if (air) this.getNavigation().stop();
    }

    /**
     * 起飞前的垂直净空检查：从锚点向上 {@link #FLIGHT_CLEARANCE} 格内不能有实心方块。
     *
     * <p>这里的"垂直空间"是**向上**要的（身体要从地面升起来）；飞行之后向下离地的距离由
     * 飞行逻辑维持同样的 10 格。空间不足时保持地面形态。
     */
    private boolean hasTakeoffClearance() {
        Vec3 from = position();
        Vec3 to = from.add(0, FLIGHT_CLEARANCE, 0);
        HitResult hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS;
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
        return new GraveDragonPathNavigation(this, level);
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

    /** Prevent movement when any real body part would collide with blocks. */
    @Override
    public void move(MoverType type, Vec3 delta) {
        if (!delta.equals(Vec3.ZERO) && !level().isClientSide && !noPhysics) {
            for (GraveDragonPartEntity part : worldParts) {
                // Only blocks stop the dragon. Other child hitboxes belong to
                // this same dragon and must never cancel movement/knockback.
                var box = part.getOrientedBox().move(delta);
                for (var shape : level().getBlockCollisions(this, box.enclosingAabb())) {
                    for (var blockBox : shape.toAabbs()) {
                        if (box.intersects(blockBox)) {
                            super.move(type, Vec3.ZERO);
                            return;
                        }
                    }
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
        updatePartPose(GraveDragonPose.sample(animation(), collisionPoseSeconds(), loopingAnimation()),
                yBodyRot, position());
        updateBodyFootprint();
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
        int priority = 0;
        this.goalSelector.addGoal(priority++, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(priority, new RandomStrollGoal(this, 0.8D));
        this.targetSelector.addGoal(priority, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        // 先记下上一 tick 的俯仰供渲染插值，再由服务端按新的速度方向更新（客户端用同步值）。
        this.bodyPitchO = bodyPitch();
        if (!this.level().isClientSide) {
            tickForm();
            updateBodyPitch();
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
