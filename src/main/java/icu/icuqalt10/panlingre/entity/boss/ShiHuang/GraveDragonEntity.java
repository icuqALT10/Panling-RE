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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
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

import java.util.Objects;
import net.neoforged.fml.loading.FMLPaths;

public class GraveDragonEntity extends MultipartEntity implements GeoEntity, PanLingEntities {
    private static final EntityDataAccessor<Long> IDLE_START = SynchedEntityData.defineId(GraveDragonEntity.class, EntityDataSerializers.LONG);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IDLE_START, -1L);
    }

    /** Shared world clock survives client tracking/retracking without restarting the animation. */
    public double idleAirSeconds(float partialTick) {
        long start = entityData.get(IDLE_START);
        return start < 0 ? 0 : Math.max(0, level().getGameTime() - start + partialTick) / 20.0;
    }

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

    public GraveDragonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        if (!partConfigLoaded) reloadPartConfig();
        if (!level.isClientSide) entityData.set(IDLE_START, level.getGameTime());
        // The tiny root is only a locomotion anchor; OBB parts handle interaction.
        for (int i = 0; i < worldParts.length; i++) {
            worldParts[i] = new GraveDragonPartEntity(this, i);
        }
        // Same id reservation as NeoForge's EnderDragon: clients derive part ids
        // from the root spawn packet, with no independently tracked child entities.
        setId(ENTITY_COUNTER.getAndAdd(worldParts.length + 1) + 1);
        updatePartPose(GraveDragonIdleAirPose.sample(0), yBodyRot, position());
        this.noPhysics = false;
        this.setNoAi(false);
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
     * Which part the attacking player's view ray actually reaches first, measured
     * against the server's own current oriented boxes.
     *
     * <p>This is the authoritative melee resolution. The server must not trust the
     * part named by the client because the client picks through AABB envelopes whose
     * empty corners select the wrong body part, and because the animated pose moves
     * between the client's frame and the server's tick. Re-casting the ray against the
     * real boxes removes both problems at once.
     *
     * <p>Deliberately uses the un-interpolated eye position and look angle: the server
     * player has not moved this tick, so interpolating would aim from a position the
     * player never occupied.
     *
     * @return the part index, or -1 when the ray reaches no part
     */
    public int pickPartAlongViewRay(Player player) {
        Vec3 eye = player.getEyePosition();
        // Ask for slightly more than the attack range, then validate properly below, so
        // a part just past the raw range can still be measured and rejected on distance.
        Vec3 end = eye.add(player.getLookAngle()
                .scale(player.entityInteractionRange() + 1.0 + MELEE_RAY_TOLERANCE));
        int exact = nearestPartAlongRay(eye, end, 0.0);
        return exact >= 0 ? exact : nearestPartAlongRay(eye, end, MELEE_RAY_TOLERANCE);
    }

    /** Nearest part whose oriented box the segment enters; padding expands local faces. */
    private int nearestPartAlongRay(Vec3 from, Vec3 to, double padding) {
        int best = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < worldParts.length; i++) {
            OrientedBoundingBox box = worldParts[i].getOrientedBox();
            if (box == null) continue;
            if (padding > 0) box = box.inflate(padding, padding, padding);
            var hit = box.clip(from, to);
            if (hit.isEmpty()) continue;
            double distance = hit.get().distanceToSqr(from);
            if (distance < bestDistance) {
                bestDistance = distance;
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
        // Anti-cheat clamp only. The part the client named is normally reach-validated by
        // ServerGamePacketListenerImpl, which drops the whole attack packet when
        // Player#canInteractWithEntity fails. That guard does not cover code paths which
        // call hurt() directly, so an obviously distant part is still refused here. The
        // margin is deliberately wide: re-validating at the real threshold with our own
        // slightly different formula is what silently discarded valid clicks.
        double vanillaLimit = player.entityInteractionRange() + 1.0;
        double hardLimit = vanillaLimit + MELEE_OUT_OF_RANGE_MARGIN;
        if (!canPlayerReachPartWithin(player, requested, hardLimit)) return -1;

        int ray = pickPartAlongViewRay(player);
        boolean rayWins = ray >= 0 && ray != requested && canPlayerReachPart(player, ray);
        int struck = rayWins ? ray : requested;
        if (GraveDragonDamageDebug.enabled()) {
            GraveDragonDamageDebug.log("melee requested=" + requested + " ray=" + ray
                    + " rayWins=" + rayWins + " -> struck=" + struck
                    + " range=" + player.entityInteractionRange());
        }
        return struck;
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
        if (level().isClientSide) {
            updateClientPartPose(0);
            return;
        }
        if (entityData.get(IDLE_START) < 0) entityData.set(IDLE_START, level().getGameTime());
        updatePartPose(GraveDragonIdleAirPose.sample(idleAirSeconds(0)), yBodyRot, position());
    }

    private void updatePartPose(GraveDragonIdleAirPose.Frame frame, float yaw, Vec3 origin) {
        var transform = GraveDragonIdleAirPose.modelToEntity(yaw, getScale());
        for (int i = 0; i < worldParts.length; i++) {
            worldParts[i].setOrientedBox(GraveDragonIdleAirPose.box(frame, PART_LABELS[i], PART_BOUNDS[i], transform, origin));
        }
    }

    public void updateClientPartPose(float partialTick) {
        if (!level().isClientSide) return;
        updateClientPartPose(partialTick, GraveDragonIdleAirPose.sample(idleAirSeconds(partialTick)));
    }

    public void updateClientPartPose(float partialTick, GraveDragonIdleAirPose.Frame frame) {
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
        tag.putLong("IdleAirStartedAt", entityData.get(IDLE_START));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("IdleAirStartedAt")) entityData.set(IDLE_START, tag.getLong("IdleAirStartedAt"));
    }

    // ===== 方法 =====

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        if (GraveDragonIdleAirPose.APPLY_ANIMATION) {
            controllers.add(new WorldTimeAnimationController<>(this, "idle_air",
                    () -> WorldTimeAnimationController.Playback.loop("idle_air", entityData.get(IDLE_START)),
                    state -> level().getGameTime() + state.getPartialTick()));
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
