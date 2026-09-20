package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import com.google.gson.*;
import icu.icuqalt10.panlingre.entity.OrientedBoundingBox;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 墓龙全部动画的姿态采样器：渲染用的骨骼姿态和碰撞箱共用同一份采样结果。
 *
 * <p>OBB 调整表（{@code GraveDragonEntity.HARD_CODED_PART_BOUNDS}）记的是**静止姿态**下的绝对
 * 坐标，由骨骼矩阵带到当前动画的位置，所以同一张表对全部动画都成立，不需要按动画各标一份。
 * 各个动画只在根骨 {@code head} 的位移基准上不同（例如 {@code idle_air} 约 249/330、
 * {@code fly} 约 2/2），衔接由 takeoff / land / idle_air_to_fly / fly_to_idle_air 这些过渡
 * 动画负责，采样器只需要按当前动画和时刻取值。
 *
 * <p>动画里没有轨道的骨骼会退回静止姿态（而不是沿用上一帧），因此姿态是确定性的、两份
 * 采样结果可以逐位对齐——这正是碰撞箱与渲染能一致的前提。
 */
public final class GraveDragonPose {
    public static final boolean APPLY_ANIMATION = true;

    /** 默认动画，也是整条龙的骨骼基准。 */
    public static final String IDLE_AIR = "idle_air";

    private static final String GEO = "assets/panlingre/geo/entity/boss/shihuang/grave_dragon.geo.json";
    private static final String ANIM = "assets/panlingre/animations/entity/boss/shihuang/grave_dragon.animation.json";
    private static final Vec3 ONE = new Vec3(1, 1, 1);
    private static final double RAD = Math.PI / 180;

    // pivot is in GeckoLib model space, in BLOCKS. The GEO importer mirrors pivot.x.
    private record Bone(String parent, Vec3 pivot, Vec3 rotation) {}
    private record Key(double time, Vec3 value, boolean spline) {}
    private record Track(List<Key> rotation, List<Key> position, List<Key> scale) {}
    /** 单个动画：自己的轨道表与时长。 */
    private record Animation(Map<String, Track> tracks, double duration) {}
    public record BonePose(Vec3 rotation, Vec3 position, Vec3 scale) {}
    public record Frame(Map<String, BonePose> bones, Map<String, Matrix4f> matrices) {}
    private record Definition(Map<String, Bone> bones, Map<String, Animation> animations) {}

    private static final class Resources {
        private static final Definition DEFINITION = readDefinition();
    }

    private static JsonObject readJson(String path) {
        var stream = GraveDragonPose.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) throw new IllegalStateException("Missing dragon resource: " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read dragon resource: " + path, e);
        }
    }

    private static Definition readDefinition() {
        Map<String, Bone> bones = new LinkedHashMap<>();
        for (JsonElement element : readJson(GEO).getAsJsonArray("minecraft:geometry")
                .get(0).getAsJsonObject().getAsJsonArray("bones")) {
            JsonObject o = element.getAsJsonObject();
            Vec3 pivot = vector(o.get("pivot"), Vec3.ZERO).multiply(-1.0 / 16, 1.0 / 16, 1.0 / 16);
            Vec3 rotation = vector(o.get("rotation"), Vec3.ZERO).multiply(-RAD, -RAD, RAD);
            bones.put(o.get("name").getAsString(), new Bone(
                    o.has("parent") ? o.get("parent").getAsString() : null, pivot, rotation));
        }

        Map<String, Animation> animations = new LinkedHashMap<>();
        for (var animationEntry : readJson(ANIM).getAsJsonObject("animations").entrySet()) {
            String name = animationEntry.getKey();
            JsonObject animation = animationEntry.getValue().getAsJsonObject();
            Map<String, Track> tracks = new LinkedHashMap<>();
            for (var entry : animation.getAsJsonObject("bones").entrySet()) {
                JsonObject bone = entry.getValue().getAsJsonObject();
                if (!bones.containsKey(entry.getKey())) throw new IllegalStateException("Unknown animated bone: " + entry.getKey());
                tracks.put(entry.getKey(), new Track(keys(bone.get("rotation")), keys(bone.get("position")), keys(bone.get("scale"))));
            }
            double duration = animation.get("animation_length").getAsDouble();
            if (!(duration > 0)) throw new IllegalStateException("Invalid duration for animation: " + name);
            animations.put(name, new Animation(Map.copyOf(tracks), duration));
        }
        if (!animations.containsKey(IDLE_AIR)) throw new IllegalStateException("Missing animation: " + IDLE_AIR);
        return new Definition(Map.copyOf(bones), Map.copyOf(animations));
    }

    private static Vec3 vector(JsonElement element, Vec3 fallback) {
        if (element == null) return fallback;
        if (element.isJsonPrimitive()) {
            double v = element.getAsDouble();
            return new Vec3(v, v, v);
        }
        if (element.isJsonObject()) {
            JsonObject o = element.getAsJsonObject();
            return vector(o.has("post") ? o.get("post") : o.get("vector"), fallback);
        }
        JsonArray a = element.getAsJsonArray();
        return new Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble());
    }

    private static List<Key> keys(JsonElement element) {
        if (element == null) return List.of();
        if (!element.isJsonObject() || element.getAsJsonObject().has("vector"))
            return List.of(new Key(0, vector(element, Vec3.ZERO), false));
        List<Key> keys = new ArrayList<>();
        for (var entry : element.getAsJsonObject().entrySet()) {
            JsonElement v = entry.getValue();
            boolean spline = v.isJsonObject() && v.getAsJsonObject().has("lerp_mode")
                    && "catmullrom".equals(v.getAsJsonObject().get("lerp_mode").getAsString());
            keys.add(new Key(Double.parseDouble(entry.getKey()), vector(v, Vec3.ZERO), spline));
        }
        keys.sort(Comparator.comparingDouble(Key::time));
        return List.copyOf(keys);
    }

    private static Vec3 sample(List<Key> keys, double time, Vec3 fallback) {
        if (keys.isEmpty()) return fallback;
        if (time <= keys.getFirst().time) return keys.getFirst().value;
        for (int i = 1; i < keys.size(); i++) {
            Key a = keys.get(i - 1), b = keys.get(i);
            if (time > b.time) continue;
            double t = (time - a.time) / (b.time - a.time);
            if (!a.spline && !b.spline) return a.value.lerp(b.value, t);
            boolean closed = keys.size() > 2 && keys.getFirst().value.distanceToSqr(keys.getLast().value) < 1e-10;
            Vec3 before = keys.get(i > 1 ? i - 2 : closed ? keys.size() - 2 : 0).value;
            Vec3 after = keys.get(i + 1 < keys.size() ? i + 1 : closed ? 1 : i).value;
            return a.value.scale(2).add(b.value.subtract(before).scale(t))
                    .add(before.scale(2).subtract(a.value.scale(5)).add(b.value.scale(4)).subtract(after).scale(t * t))
                    .add(before.scale(-1).add(a.value.scale(3)).subtract(b.value.scale(3)).add(after).scale(t * t * t))
                    .scale(0.5);
        }
        return keys.getLast().value;
    }

    /** 全部动画名，供状态机与测试枚举。 */
    public static Set<String> animationNames() {
        return Resources.DEFINITION.animations.keySet();
    }

    /**
     * 脊柱主链：{@code head → tail_tip}，21 节。
     *
     * <p>{@code neck_10..neck_01} 与 {@code tail_01..tail_tip} 的节距都是 3.00 格，
     * 所以链式跟随退化成"沿龙头轨迹每 3 格取一点"，不必处理不均匀骨长。四肢分别挂在
     * {@code neck_06} 与 {@code tail_02} 上，脊柱一动它们自动跟着走。
     */
    private static final List<String> SPINE = List.of(
            "head", "neck_joint", "neck_11", "neck_10", "neck_09", "neck_08", "neck_07", "neck_06",
            "neck_05", "neck_04", "neck_03", "neck_02", "neck_01",
            "tail_01", "tail_02", "tail_03", "tail_04", "tail_05", "tail_06", "tail_07", "tail_tip");

    public static List<String> spineBones() {
        return SPINE;
    }

    /** 骨骼在静止姿态下的 pivot（模型空间，单位格）。链式反解要用它算节距。 */
    public static Vec3 bonePivot(String bone) {
        Bone b = Resources.DEFINITION.bones.get(bone);
        if (b == null) throw new IllegalArgumentException("Unknown dragon bone: " + bone);
        return b.pivot;
    }

    /** 骨骼的静止旋转，弧度。 */
    public static Vec3 boneRestRotation(String bone) {
        Bone b = Resources.DEFINITION.bones.get(bone);
        if (b == null) throw new IllegalArgumentException("Unknown dragon bone: " + bone);
        return b.rotation;
    }

    /**
     * 用链式求解出的脊柱位姿覆盖基础姿态，产出新的帧（并重算全部骨骼矩阵）。
     *
     * <p>没被覆盖的骨骼保持基础姿态，所以动画的扭动、四肢与鬃毛都还在，只是脊柱的整体位置
     * 与朝向改由链式求解决定。
     */
    public static Frame withSpine(Frame base, String[] bones, BonePose[] poses) {
        Map<String, BonePose> merged = new LinkedHashMap<>(base.bones());
        for (int i = 0; i < bones.length; i++) merged.put(bones[i], poses[i]);
        Map<String, Matrix4f> matrices = new HashMap<>();
        for (String name : merged.keySet()) matrix(name, merged, matrices, new HashSet<>());
        return new Frame(Map.copyOf(merged), Map.copyOf(matrices));
    }

    /** 某个动画的时长（秒）。 */
    public static double duration(String animation) {
        Animation anim = Resources.DEFINITION.animations.get(animation);
        if (anim == null) throw new IllegalArgumentException("Unknown dragon animation: " + animation);
        return anim.duration;
    }

    /**
     * 按动画名与秒数采样。
     *
     * @param loop true 用于循环动画，对时长取模；false 用于 takeoff/land/turn 这类一次性动画，
     *             超出时长后钳制在末帧（也就是播完停住），所以调用方可以直接给一个不断增长的秒数。
     */
    public static Frame sample(String animation, double seconds, boolean loop) {
        Definition definition = Resources.DEFINITION;
        Animation anim = definition.animations.get(animation);
        if (anim == null) throw new IllegalArgumentException("Unknown dragon animation: " + animation);
        double time = loop
                ? ((seconds % anim.duration) + anim.duration) % anim.duration
                : Math.max(0, Math.min(seconds, anim.duration));

        Map<String, BonePose> poses = new LinkedHashMap<>();
        for (var entry : definition.bones.entrySet()) {
            Bone bone = entry.getValue();
            Track track = APPLY_ANIMATION ? anim.tracks.get(entry.getKey()) : null;
            Vec3 rotation = bone.rotation, position = Vec3.ZERO, scale = ONE;
            if (track != null) {
                rotation = rotation.add(sample(track.rotation, time, Vec3.ZERO).multiply(-RAD, -RAD, RAD));
                // Animation position is an OFFSET, including the large first-frame value.
                // GeckoLib uses it directly. Subtracting frame 0 shifts every descendant.
                position = sample(track.position, time, Vec3.ZERO);
                scale = sample(track.scale, time, ONE);
            }
            poses.put(entry.getKey(), new BonePose(rotation, position, scale));
        }
        Map<String, Matrix4f> matrices = new HashMap<>();
        for (String name : poses.keySet()) matrix(name, poses, matrices, new HashSet<>());
        return new Frame(Map.copyOf(poses), Map.copyOf(matrices));
    }

    /** 循环采样，等价于 {@code sample(animation, seconds, true)}。 */
    public static Frame sample(String animation, double seconds) {
        return sample(animation, seconds, true);
    }

    /** 旧调用方式，等价于采样 {@link #IDLE_AIR}。 */
    public static Frame sample(double seconds) {
        return sample(IDLE_AIR, seconds);
    }

    private static Matrix4f matrix(String name, Map<String, BonePose> poses, Map<String, Matrix4f> cache, Set<String> visiting) {
        if (cache.containsKey(name)) return cache.get(name);
        Bone bone = Resources.DEFINITION.bones.get(name);
        if (bone == null || !visiting.add(name)) throw new IllegalStateException("Invalid bone parent chain: " + name);
        BonePose pose = poses.get(name);
        Vec3 p = pose.position, r = pose.rotation, s = pose.scale, pivot = bone.pivot;
        Matrix4f result = bone.parent == null ? new Matrix4f() : new Matrix4f(matrix(bone.parent, poses, cache, visiting));
        // Same as RenderUtil.prepMatrixForBone. Absolute model pivots are correct here:
        // the parent's translate-away step has already restored model coordinates.
        result.translate((float)-p.x / 16, (float)p.y / 16, (float)p.z / 16)
                .translate((float)pivot.x, (float)pivot.y, (float)pivot.z)
                .rotate(new Quaternionf().rotationZ((float)r.z))
                .rotate(new Quaternionf().rotationY((float)r.y))
                .rotate(new Quaternionf().rotationX((float)r.x))
                .scale((float)s.x, (float)s.y, (float)s.z)
                .translate((float)-pivot.x, (float)-pivot.y, (float)-pivot.z);
        visiting.remove(name);
        cache.put(name, result);
        return result;
    }

    /** Split arm boxes share their original bone. Horn geometry belongs to head. */
    public static String boneForPart(String label) {
        String name = (label.startsWith("horn_") || label.equals("upper_jaw")) ? "head" : label.replaceFirst("_(a|b)$", "");
        if (!Resources.DEFINITION.bones.containsKey(name)) throw new IllegalArgumentException("Unmapped dragon part: " + label);
        return name;
    }

    /** 无俯仰的版本，行为与加重载之前完全一致。 */
    public static Matrix4f modelToEntity(float bodyYaw, float scale) {
        return modelToEntity(bodyYaw, 0.0F, scale);
    }

    /**
     * 渲染变换的镜像：{@code GeoEntityRenderer.actuallyRender} 做的是
     * {@code scale → applyRotations(rotateY(180 - yBodyRot)) → translate(0, 0.01, 0)}。
     *
     * <p>俯仰是我们自己加的一步（GeckoLib 只给活着的实体应用偏航），所以这里必须与
     * {@code GraveDragonRenderer.applyRotations} 里的旋转**顺序和符号完全一致**，否则碰撞箱会
     * 与模型错开——这正是"所见即所得"的前提。
     *
     * <p>顺序是 {@code Ry · Rx}，也就是 {@code Rx} 作用在**实体坐标系**里（偏航之后的那一层），
     * 这正是"抬头/低头"该有的轴。反过来的 {@code Rx · Ry} 会让俯仰作用在模型坐标系里，
     * 而模型空间的 −Z 是龙首，于是"抬尾"退化成"整条龙向右横滚"——爬升时看起来像侧躺。
     *
     * <p>符号：实体坐标系里龙首的局部方向是 +Z（前方），绕 +X 转 {@code +θ} 会把 +Z 压向
     * <b>−Y</b>（低头），所以这里要取 {@code −bodyPitch}，让
     * {@code bodyPitch > 0 = 爬升 = 抬头}，与 {@code updateBodyPitch} 的
     * {@code atan2(vy, 水平速度)} 一致。
     */
    public static Matrix4f modelToEntity(float bodyYaw, float bodyPitch, float scale) {
        return new Matrix4f().scale(scale)
                .rotate(new Quaternionf().rotationY((180 - bodyYaw) * (float)RAD))
                .rotate(new Quaternionf().rotationX(-bodyPitch * (float)RAD))
                .translate(0, 0.01f, 0);
    }

    public static OrientedBoundingBox box(Frame frame, String label, float[] bounds, Matrix4f modelToEntity, Vec3 entityPosition) {
        Matrix4f transform = new Matrix4f(modelToEntity).mul(frame.matrices.get(boneForPart(label)));
        // Config centers are already in Blockbench/GeckoLib model space, in BLOCKS.
        // Never multiply by 16 here or mirror X again.
        Vector3f center = transform.transformPosition(new Vector3f(bounds[3], bounds[4], bounds[5]));
        // A fitted antler has an additional bind-space rotation around its center.
        if (bounds.length >= 9) transform.rotateZ((float)Math.toRadians(bounds[8]))
                .rotateY((float)Math.toRadians(bounds[7])).rotateX((float)Math.toRadians(bounds[6]));
        Vector3f x = transform.transformDirection(new Vector3f(1, 0, 0));
        Vector3f y = transform.transformDirection(new Vector3f(0, 1, 0));
        Vector3f z = transform.transformDirection(new Vector3f(0, 0, 1));
        return new OrientedBoundingBox(entityPosition.add(center.x, center.y, center.z), vec(x), vec(y), vec(z),
                new Vec3(bounds[0] * x.length() / 2, bounds[1] * y.length() / 2, bounds[2] * z.length() / 2));
    }

    private static Vec3 vec(Vector3f v) { return new Vec3(v.x, v.y, v.z); }
}
