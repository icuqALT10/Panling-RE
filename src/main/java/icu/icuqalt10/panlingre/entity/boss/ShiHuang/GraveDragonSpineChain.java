package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * 墓龙的脊柱链式跟随：让 21 节身体沿龙头的轨迹蜿蜒，而不是整条龙刚体平移。
 *
 * <p>为什么需要它：动画只能提供**局部扭动**（各节相对父级的旋转）。整条龙怎么在世界上走线，
 * 动画表达不了——真龙是"龙头带着身体游"，转弯时身体留在后面甩出一条弧。这里就是那个"身体"：
 * 龙头每 tick 走到目标点，其余各节按"保持与前一节的距离、并指向前一节"逐节跟随。
 *
 * <p>节点在世界空间维护（直观且不需要坐标系技巧），只有在反解成骨骼位姿时才换到模型空间。
 *
 * <h2>反解</h2>
 * 设第 i 节的骨骼 pivot 为 {@code p_i}（模型空间），本地变换
 * {@code L_i = T(-pos_i) · T(p_i) · R_i · T(-p_i)}，父级累积矩阵 {@code M_parent}：
 * <ul>
 *   <li>{@code M_i · p_i = M_parent · (p_i - pos_i)}，与旋转无关，所以
 *       {@code pos_i = p_i - M_parent⁻¹ · Q_i} 就能把本节 pivot 精确放到目标点 {@code Q_i}。</li>
 *   <li>{@code M_i · p_{i+1} = Q_i + M_parent · R_i · d_i}（其中 {@code d_i = p_{i+1} - p_i}），
 *       要让子节点落在 {@code Q_{i+1}} 就要求 {@code R_i · d_i = M_parent⁻¹ · (Q_{i+1} - Q_i)}，
 *       也就是把 {@code d_i} 转到该方向的旋转。</li>
 * </ul>
 * 动画的扭动 {@code A_i} 作为局部偏移叠加：{@code R_i = rotationTo(A_i · d_i, v) · A_i}，
 * 这样 {@code R_i · d_i = v} 仍然成立，而各节相对父级的扭动被保留。
 */
public final class GraveDragonSpineChain {
    /** 速度保留比例。越大越"甩鞭"，越小越跟手。 */
    private static final double DAMPING = 0.80;
    /** 逐节距离约束的收敛容差（格）。 */
    private static final double LENGTH_EPSILON = 1.0E-4;

    private final List<String> bones = GraveDragonPose.spineBones();
    private final int count = bones.size();
    /** 各节在世界空间的位置。 */
    private final Vec3[] positions = new Vec3[count];
    /** verlet 的上一帧位置（用来推速度）。 */
    private final Vec3[] previous = new Vec3[count];
    /** 各节相对前一节的静止距离（格）。 */
    private final double[] spacing = new double[count];

    private boolean initialised;

    public GraveDragonSpineChain() {
        for (int i = 1; i < count; i++) {
            spacing[i] = GraveDragonPose.bonePivot(bones.get(i))
                    .subtract(GraveDragonPose.bonePivot(bones.get(i - 1))).length();
        }
    }

    /** 总长度（格），头部到尾部。 */
    public double totalLength() {
        double total = 0;
        for (int i = 1; i < count; i++) total += spacing[i];
        return total;
    }

    public boolean initialised() {
        return initialised;
    }

    public Vec3 node(int index) {
        return positions[index];
    }

    /** 把整条链摆到指定姿态上：{@code head} 在 {@code headPosition}，其余沿模型静止姿态的方向排开。 */
    public void reset(Vec3 headPosition, Vec3 forward, Vec3 up) {
        Vec3 axis = forward.normalize();
        if (axis.lengthSqr() < 1.0E-8) axis = new Vec3(0, 0, 1);
        for (int i = 0; i < count; i++) {
            double distance = 0;
            for (int j = 1; j <= i; j++) distance += spacing[j];
            // 身体沿 -forward 排开（龙头在前）。
            positions[i] = headPosition.subtract(axis.scale(distance));
            previous[i] = positions[i];
        }
        initialised = true;
    }

    /**
     * 推进一帧：龙头走到 {@code headTarget}，其余各节带惯性跟随并维持节距。
     *
     * <p>确定性：只依赖上一帧的链状态与本次的龙头目标，两侧只要初始状态相同就会一直保持一致。
     */
    public void update(Vec3 headTarget) {
        if (!initialised) {
            reset(headTarget, new Vec3(0, 0, 1), new Vec3(0, 1, 0));
            return;
        }

        // 龙头直接跟随目标，不参与惯性与距离约束——它是驱动点。
        previous[0] = positions[0];
        positions[0] = headTarget;

        for (int i = 1; i < count; i++) {
            Vec3 velocity = positions[i].subtract(previous[i]).scale(DAMPING);
            Vec3 next = positions[i].add(velocity);
            previous[i] = positions[i];

            Vec3 offset = next.subtract(positions[i - 1]);
            double length = offset.length();
            // 距离约束：把本节拉回"距前一节恰好 spacing[i]"的位置。
            next = length < LENGTH_EPSILON
                    ? positions[i - 1].add(0, -spacing[i], 0)
                    : positions[i - 1].add(offset.scale(spacing[i] / length));
            positions[i] = next;
        }
    }

    /**
     * 把链的形状反解成脊柱各节的骨骼位姿，并与基础姿态合并。
     *
     * @param origin        实体锚点（链的世界坐标都是绝对坐标）
     * @param modelToEntity {@code GraveDragonPose.modelToEntity(yaw, pitch, scale)}
     */
    public GraveDragonPose.Frame apply(GraveDragonPose.Frame base, Vec3 origin, Matrix4f modelToEntity) {
        Matrix4f worldToModel = new Matrix4f(modelToEntity).invert();
        Matrix4f parent = new Matrix4f();
        Matrix4f parentInverse = new Matrix4f();

        String[] names = bones.toArray(new String[0]);
        GraveDragonPose.BonePose[] poses = new GraveDragonPose.BonePose[count];

        for (int i = 0; i < count; i++) {
            Vec3 pivot = GraveDragonPose.bonePivot(names[i]);
            Vec3 target = toModel(worldToModel, origin, positions[i]);

            parentInverse.set(parent).invert();
            // 本节的 pivot 位置只由平移决定、与旋转无关：
            //   M_i · p_i = M_parent · (p_i + shift) = Q_i  =>  shift = M_parent⁻¹ · Q_i - p_i
            Vector3f local = parentInverse.transformPosition(
                    new Vector3f((float) target.x, (float) target.y, (float) target.z));
            Vec3 shift = new Vec3(local.x, local.y, local.z).subtract(pivot);

            Quaternionf rotation;
            if (i + 1 < count) {
                Vec3 segment = GraveDragonPose.bonePivot(names[i + 1]).subtract(pivot);
                Vec3 wanted = toModel(worldToModel, origin, positions[i + 1]).subtract(target);
                Vector3f localWanted = parentInverse.transformDirection(
                        new Vector3f((float) wanted.x, (float) wanted.y, (float) wanted.z));
                rotation = alignWithAnimation(base, names[i], segment,
                        new Vec3(localWanted.x, localWanted.y, localWanted.z));
            } else {
                // 末节没有子节点，直接用动画/静止的旋转。
                rotation = euler(restOrAnimated(base, names[i]));
            }

            // BonePose.position 存的是模型像素，且 matrix() 用 (-x, +y, +z)/16 施加，
            // 所以按同样的编码写回去。
            // 关键：parent 必须用**同一组欧拉角**重建旋转（而不是原始四元数），否则这里的
            // 累积矩阵会和 GraveDragonPose.matrix() 算出的骨骼矩阵分歧，并沿链逐节放大。
            Vec3 euler = eulerToVec(rotation);
            poses[i] = new GraveDragonPose.BonePose(euler,
                    new Vec3(-shift.x, shift.y, shift.z).scale(16.0),
                    new Vec3(1, 1, 1));
            parent.mul(new Matrix4f()
                    .translate((float) shift.x, (float) shift.y, (float) shift.z)
                    .translate((float) pivot.x, (float) pivot.y, (float) pivot.z)
                    .rotate(new Quaternionf().rotationZ((float) euler.z))
                    .rotate(new Quaternionf().rotationY((float) euler.y))
                    .rotate(new Quaternionf().rotationX((float) euler.x))
                    .translate((float) -pivot.x, (float) -pivot.y, (float) -pivot.z));
        }
        return GraveDragonPose.withSpine(base, names, poses);
    }

    /**
     * {@code R_i = rotationTo(A_i · d_i, v) · A_i}：先让动画的扭动作用在本节方向上，再把结果
     * 转到链式要求的方向。这样既满足了链的几何约束，又保住了动画的扭动。
     */
    private static Quaternionf alignWithAnimation(GraveDragonPose.Frame base, String bone, Vec3 segment, Vec3 wanted) {
        Quaternionf animation = euler(restOrAnimated(base, bone));
        if (wanted.lengthSqr() < 1.0E-10 || segment.lengthSqr() < 1.0E-10) return animation;
        Vector3f from = animation.transform(new Vector3f((float) segment.x, (float) segment.y, (float) segment.z));
        Vector3f to = new Vector3f((float) wanted.x, (float) wanted.y, (float) wanted.z).normalize();
        Quaternionf align = new Quaternionf().rotationTo(from, to);
        return align.mul(animation);
    }

    private static Vec3 restOrAnimated(GraveDragonPose.Frame base, String bone) {
        GraveDragonPose.BonePose pose = base.bones().get(bone);
        return pose != null ? pose.rotation() : GraveDragonPose.boneRestRotation(bone);
    }

    /** 与 {@code GraveDragonPose.matrix} 一致：Rz · Ry · Rx。 */
    private static Quaternionf euler(Vec3 radians) {
        return new Quaternionf()
                .rotationZ((float) radians.z)
                .rotateY((float) radians.y)
                .rotateX((float) radians.x);
    }

    /** 反向取出欧拉角（模型空间用不到精确还原，链式求解只用它作局部偏移，这里返回一个等价表示）。 */
    private static Vec3 eulerToVec(Quaternionf rotation) {
        Vector3f euler = new Vector3f();
        rotation.getEulerAnglesZYX(euler);
        return new Vec3(euler.x, euler.y, euler.z);
    }

    private static Vec3 toModel(Matrix4f worldToModel, Vec3 origin, Vec3 worldPosition) {
        Vector3f v = worldToModel.transformPosition(new Vector3f(
                (float) (worldPosition.x - origin.x),
                (float) (worldPosition.y - origin.y),
                (float) (worldPosition.z - origin.z)));
        return new Vec3(v.x, v.y, v.z);
    }
}
