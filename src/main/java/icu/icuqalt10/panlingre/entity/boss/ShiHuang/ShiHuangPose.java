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

public final class ShiHuangPose {
    /** 默认待机动作。 */
    public static final String IDLE = "idle";
    /** Idle sole-to-crown height measured in the source model: 68.708265 pixels. */
    public static final float MODEL_SCALE = 44.8F / 68.708265F;

    private static final String GEO = "assets/panlingre/geo/entity/boss/shihuang/shihuang_spirit.geo.json";
    private static final String ANIM = "assets/panlingre/animations/entity/boss/shihuang/shihuang_spirit.animation.json";
    private static final Vec3 ONE = new Vec3(1, 1, 1);
    private static final double RAD = Math.PI / 180;

    // pivot is in GeckoLib model space, in BLOCKS. The GEO importer mirrors pivot.x.
    private record Bone(String parent, Vec3 pivot, Vec3 rotation) {}
    private record Key(double time, Vec3 value, Vec3 pre, boolean spline) {}
    private record Track(List<Key> rotation, List<Key> position, List<Key> scale) {}
    /** 单个动画：自己的轨道表与时长。 */
    private record Animation(Map<String, Track> tracks, double duration) {}
    public record BonePose(Vec3 rotation, Vec3 position, Vec3 scale) {}
    public record Frame(Map<String, BonePose> bones, Map<String, Matrix4f> matrices) {}
    private record Strike(double from,double to,double bend) {}
    private record Definition(Map<String, Bone> bones, Map<String, Animation> animations,Map<String,List<Strike>> strikes) {}

    private static final class Resources {
        private static final Definition DEFINITION = readDefinition();
    }

    private static JsonElement readJson(String path) {
        var stream = ShiHuangPose.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) throw new IllegalStateException("Missing emperor resource: " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read emperor resource: " + path, e);
        }
    }

    private static Definition readDefinition() {
        Map<String, Bone> bones = new LinkedHashMap<>();
        for (JsonElement element : readJson(GEO).getAsJsonObject().getAsJsonArray("minecraft:geometry")
                .get(0).getAsJsonObject().getAsJsonArray("bones")) {
            JsonObject o = element.getAsJsonObject();
            Vec3 pivot = vector(o.get("pivot"), Vec3.ZERO).multiply(-1.0 / 16, 1.0 / 16, 1.0 / 16);
            Vec3 rotation = vector(o.get("rotation"), Vec3.ZERO).multiply(-RAD, -RAD, RAD);
            bones.put(o.get("name").getAsString(), new Bone(
                    o.has("parent") ? o.get("parent").getAsString() : null, pivot, rotation));
        }

        Map<String, Animation> animations = new LinkedHashMap<>();
        for (var animationEntry : readJson(ANIM).getAsJsonObject().getAsJsonObject("animations").entrySet()) {
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
        if (!animations.containsKey(IDLE)) throw new IllegalStateException("Missing animation: " + IDLE);
        Map<String,List<Strike>> strikes=new HashMap<>();
        for(var value:readJson("assets/panlingre/shihuang/shihuang_actions.json").getAsJsonArray()){
            var action=value.getAsJsonObject();String name=action.get("clip").getAsString();
            if(!name.startsWith("combo_")&&!name.startsWith("counter_"))continue;
            List<Strike> windows=new ArrayList<>();
            for(var element:action.getAsJsonArray("sword_windows")){
                var window=element.getAsJsonObject();
                double bend=switch(window.get("stroke").getAsString()){case "M","N","W"->1;case "A"->.5;default->0;};
                windows.add(new Strike(window.get("from").getAsDouble(),window.get("to").getAsDouble(),bend));
            }
            strikes.put(name,List.copyOf(windows));
        }
        return new Definition(Map.copyOf(bones), Map.copyOf(animations),Map.copyOf(strikes));
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
            return List.of(new Key(0, vector(element, Vec3.ZERO), vector(element, Vec3.ZERO), false));
        List<Key> keys = new ArrayList<>();
        for (var entry : element.getAsJsonObject().entrySet()) {
            JsonElement v = entry.getValue();
            boolean spline = v.isJsonObject() && v.getAsJsonObject().has("lerp_mode")
                    && "catmullrom".equals(v.getAsJsonObject().get("lerp_mode").getAsString());
            keys.add(new Key(Double.parseDouble(entry.getKey()), vector(v, Vec3.ZERO), v.isJsonObject() && v.getAsJsonObject().has("pre") ? vector(v.getAsJsonObject().get("pre"),Vec3.ZERO) : vector(v,Vec3.ZERO), spline));
        }
        keys.sort(Comparator.comparingDouble(Key::time));
        return List.copyOf(keys);
    }

    private static Vec3 sample(List<Key> keys, double time, Vec3 fallback) {
        if (keys.isEmpty()) return fallback;
        if (time <= keys.getFirst().time) return keys.getFirst().value;
        if (time > keys.getLast().time) return keys.getLast().value;
        // Find the first key at or after time, preserving the interpolation at exact key times.
        int low = 1, high = keys.size() - 1;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (time > keys.get(middle).time) low = middle + 1;
            else high = middle;
        }
        int i = low;
        Key a = keys.get(i - 1), b = keys.get(i);
        if (time == b.time) return b.value;
        double t = (time - a.time) / (b.time - a.time);
        if (!a.spline && !b.spline) return a.value.lerp(b.pre, t);
        boolean closed = keys.size() > 2 && keys.getFirst().value.distanceToSqr(keys.getLast().value) < 1e-10;
        Vec3 before = keys.get(i > 1 ? i - 2 : closed ? keys.size() - 2 : 0).value;
        Vec3 after = keys.get(i + 1 < keys.size() ? i + 1 : closed ? 1 : i).value;
        return a.value.scale(2).add(b.value.subtract(before).scale(t))
                .add(before.scale(2).subtract(a.value.scale(5)).add(b.value.scale(4)).subtract(after).scale(t * t))
                .add(before.scale(-1).add(a.value.scale(3)).subtract(b.value.scale(3)).add(after).scale(t * t * t))
                .scale(0.5);
    }

    /** 全部动画名，供动作同步时核对。 */
    public static Set<String> animationNames() {
        return Resources.DEFINITION.animations.keySet();
    }

    /** 用同一组局部位姿重建渲染与 OBB 的完整骨骼矩阵。 */
    public static Frame rebuild(Map<String, BonePose> merged) {
        Map<String, Matrix4f> matrices = new HashMap<>();
        Set<String> visiting = new HashSet<>();
        for (String name : merged.keySet()) matrix(name, merged, matrices, visiting);
        return new Frame(Map.copyOf(merged), Map.copyOf(matrices));
    }

    /** 某个动画的时长（秒）。 */
    public static double duration(String animation) {
        Animation anim = Resources.DEFINITION.animations.get(animation);
        if (anim == null) throw new IllegalArgumentException("Unknown emperor animation: " + animation);
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
        if (anim == null) throw new IllegalArgumentException("Unknown emperor animation: " + animation);
        double time = loop
                ? ((seconds % anim.duration) + anim.duration) % anim.duration
                : Math.max(0, Math.min(seconds, anim.duration));

        Map<String, BonePose> poses = new LinkedHashMap<>();
        for (var entry : definition.bones.entrySet()) {
            Bone bone = entry.getValue();
            Track track = anim.tracks.get(entry.getKey());
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
        Frame frame=rebuild(poses);
        double bend=0;
        for(var strike:definition.strikes.getOrDefault(animation,List.of())){
            double start=Math.max(0,strike.from-.18);
            double weight=Math.min(smooth((time-start)/(strike.from-start)),smooth((strike.to+.22-time)/.22));
            bend=Math.max(bend,weight*strike.bend);
        }
        if(bend>0){
            // Bend toward the player's torso in model-forward space, independent of the authored torso twist.
            frame=bendForward(frame,"waist",10*bend);
            frame=bendForward(frame,"arm_r_upper",12*bend);
        }
        return frame;
    }

    private static double smooth(double t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    private static Frame bendForward(Frame frame,String name,double degrees){
        var pose=frame.bones.get(name);String parent=Resources.DEFINITION.bones.get(name).parent;
        var parentRotation=frame.matrices.get(parent).getUnnormalizedRotation(new Quaternionf());
        var local=new Quaternionf().rotationZYX((float)pose.rotation.z,(float)pose.rotation.y,(float)pose.rotation.x);
        var rotation=new Quaternionf(parentRotation).invert().mul(new Quaternionf().rotationX((float)(-degrees*RAD)))
                .mul(parentRotation).mul(local).getEulerAnglesZYX(new Vector3f());
        var poses=new LinkedHashMap<>(frame.bones);
        poses.put(name,new BonePose(vec(rotation),pose.position,pose.scale));
        return rebuild(poses);
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

    public static Matrix4f modelToEntity(float yaw) {
        return new Matrix4f().scale(MODEL_SCALE).rotateY((float)Math.toRadians(180-yaw)).translate(0,.01f,0);
    }

    public static Vec3 point(Frame frame, String bone, Vec3 model, float yaw, Vec3 position) {
        Vector3f p=new Matrix4f(modelToEntity(yaw)).mul(frame.matrices.get(bone))
                .transformPosition(new Vector3f((float)model.x,(float)model.y,(float)model.z));
        return position.add(p.x,p.y,p.z);
    }

    public static OrientedBoundingBox box(Frame frame, String label, float[] bounds, Matrix4f modelToEntity, Vec3 entityPosition) {
        Matrix4f transform = new Matrix4f(modelToEntity).mul(frame.matrices.get(label));
        // Config centers are already in Blockbench/GeckoLib model space, in BLOCKS.
        // Never multiply by 16 here or mirror X again.
        Vector3f center = transform.transformPosition(new Vector3f(bounds[3], bounds[4], bounds[5]));
        // Fitted limbs, fingers and antlers retain their bind-space cube orientation.
        if (bounds.length >= 9) transform.rotateZ((float)Math.toRadians(bounds[8]))
                .rotateY((float)Math.toRadians(bounds[7])).rotateX((float)Math.toRadians(bounds[6]));
        Vector3f x = transform.transformDirection(new Vector3f(1, 0, 0));
        Vector3f y = transform.transformDirection(new Vector3f(0, 1, 0));
        Vector3f z = transform.transformDirection(new Vector3f(0, 0, 1));
        Vec3 axisX = vec(x).normalize(), axisY = vec(y).normalize(), axisZ = vec(z).normalize();
        Vec3 half = new Vec3(bounds[0] * x.length() / 2, bounds[1] * y.length() / 2, bounds[2] * z.length() / 2);
        if (Math.abs(axisX.dot(axisY)) > 1e-5 || Math.abs(axisX.dot(axisZ)) > 1e-5
                || Math.abs(axisY.dot(axisZ)) > 1e-5) {
            // Animated non-uniform scale can shear a rotated cube. SAT needs orthogonal axes;
            // project its three transformed half-edges onto an orthonormal enclosing frame.
            Vec3 edgeX = vec(x).scale(bounds[0] / 2), edgeY = vec(y).scale(bounds[1] / 2), edgeZ = vec(z).scale(bounds[2] / 2);
            axisY = axisY.subtract(axisX.scale(axisY.dot(axisX))).normalize();
            axisZ = axisX.cross(axisY).normalize();
            half = new Vec3(projectedExtent(axisX, edgeX, edgeY, edgeZ),
                    projectedExtent(axisY, edgeX, edgeY, edgeZ), projectedExtent(axisZ, edgeX, edgeY, edgeZ));
        }
        return new OrientedBoundingBox(entityPosition.add(center.x, center.y, center.z), axisX, axisY, axisZ, half);
    }

    private static double projectedExtent(Vec3 axis, Vec3 x, Vec3 y, Vec3 z) {
        return Math.abs(axis.dot(x)) + Math.abs(axis.dot(y)) + Math.abs(axis.dot(z));
    }

    private static Vec3 vec(Vector3f v) { return new Vec3(v.x, v.y, v.z); }
}
