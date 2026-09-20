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

/** The rendered GeoBones and server hitboxes share this idle_air sampler. */
public final class GraveDragonIdleAirPose {
    public static final boolean APPLY_ANIMATION = true;
    private static final String GEO = "assets/panlingre/geo/entity/boss/shihuang/grave_dragon.geo.json";
    private static final String ANIM = "assets/panlingre/animations/entity/boss/shihuang/grave_dragon.animation.json";
    private static final Vec3 ONE = new Vec3(1, 1, 1);
    private static final double RAD = Math.PI / 180;

    // pivot is in GeckoLib model space, in BLOCKS. The GEO importer mirrors pivot.x.
    private record Bone(String parent, Vec3 pivot, Vec3 rotation) {}
    private record Key(double time, Vec3 value, boolean spline) {}
    private record Track(List<Key> rotation, List<Key> position, List<Key> scale) {}
    public record BonePose(Vec3 rotation, Vec3 position, Vec3 scale) {}
    public record Frame(Map<String, BonePose> bones, Map<String, Matrix4f> matrices) {}
    private record Definition(Map<String, Bone> bones, Map<String, Track> tracks, double duration) {}

    private static final class Resources {
        private static final Definition DEFINITION = readDefinition();
    }

    private static JsonObject readJson(String path) {
        var stream = GraveDragonIdleAirPose.class.getClassLoader().getResourceAsStream(path);
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
        JsonObject animation = readJson(ANIM).getAsJsonObject("animations").getAsJsonObject("idle_air");
        Map<String, Track> tracks = new LinkedHashMap<>();
        for (var entry : animation.getAsJsonObject("bones").entrySet()) {
            JsonObject bone = entry.getValue().getAsJsonObject();
            if (!bones.containsKey(entry.getKey())) throw new IllegalStateException("Unknown animated bone: " + entry.getKey());
            tracks.put(entry.getKey(), new Track(keys(bone.get("rotation")), keys(bone.get("position")), keys(bone.get("scale"))));
        }
        double duration = animation.get("animation_length").getAsDouble();
        if (!(duration > 0)) throw new IllegalStateException("Invalid idle_air duration");
        return new Definition(Map.copyOf(bones), Map.copyOf(tracks), duration);
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

    public static Frame sample(double seconds) {
        Definition definition = Resources.DEFINITION;
        double time = ((seconds % definition.duration) + definition.duration) % definition.duration;
        Map<String, BonePose> poses = new LinkedHashMap<>();
        for (var entry : definition.bones.entrySet()) {
            Bone bone = entry.getValue();
            Track track = APPLY_ANIMATION ? definition.tracks.get(entry.getKey()) : null;
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

    public static Matrix4f modelToEntity(float bodyYaw, float scale) {
        // Matches GeoEntityRenderer: native scale, 180 - yBodyRot, then +0.01 Y.
        return new Matrix4f().scale(scale).rotate(new Quaternionf().rotationY((180 - bodyYaw) * (float)RAD)).translate(0, 0.01f, 0);
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
