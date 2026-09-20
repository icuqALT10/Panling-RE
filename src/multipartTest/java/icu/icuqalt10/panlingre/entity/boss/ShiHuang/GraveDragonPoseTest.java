package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import icu.icuqalt10.panlingre.entity.OrientedBoundingBox;
import icu.icuqalt10.panlingre.entity.OrientedHitbox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.RenderUtil;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

/** Numerical regression checks against the REAL GeckoLib pose-stack operations. */
public final class GraveDragonPoseTest {
    private static final Map<String, JsonObject> geo = new LinkedHashMap<>();
    private static final Map<String, String> bbParents = new HashMap<>();
    private static final Map<String, JsonObject> groups = new HashMap<>();
    private static int checks;

    private static JsonObject read(String path) throws Exception {
        return JsonParser.parseString(Files.readString(Path.of(path))).getAsJsonObject();
    }
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static void near(double a, double b, double epsilon, String message) {
        check(Math.abs(a - b) <= epsilon, message + ": " + a + " != " + b);
    }
    private static float component(JsonArray a, int i) { return a == null ? 0 : a.get(i).getAsFloat(); }
    private static void visit(JsonArray nodes, String parent) {
        for (JsonElement e : nodes) {
            if (!e.isJsonObject()) continue;
            JsonObject node = e.getAsJsonObject();
            String name = groups.get(node.get("uuid").getAsString()).get("name").getAsString();
            bbParents.put(name, parent);
            visit(node.getAsJsonArray("children"), name);
        }
    }

    private static void verifyExports(JsonObject bb, JsonObject animation) {
        for (JsonElement e : bb.getAsJsonArray("groups")) groups.put(e.getAsJsonObject().get("uuid").getAsString(), e.getAsJsonObject());
        visit(bb.getAsJsonArray("outliner"), null);
        for (JsonObject group : groups.values()) {
            String name = group.get("name").getAsString();
            JsonObject bone = geo.get(name);
            check(bone != null, "Missing exported bone " + name);
            String parent = bone.has("parent") ? bone.get("parent").getAsString() : null;
            check(Objects.equals(parent, bbParents.get(name)), "Parent differs " + name);
            for (int i = 0; i < 3; i++)
                near(component(bone.getAsJsonArray("pivot"), i) * (i == 0 ? -1 : 1),
                        component(group.getAsJsonArray("origin"), i), 0.0001, "BB/GEO pivot " + name);
        }
        JsonObject idle = null;
        for (JsonElement e : bb.getAsJsonArray("animations"))
            if (e.getAsJsonObject().get("name").getAsString().equals("idle_air")) idle = e.getAsJsonObject();
        check(idle != null, "BB idle_air missing");
        for (var entry : idle.getAsJsonObject("animators").entrySet()) {
            JsonObject animator = entry.getValue().getAsJsonObject();
            if (!animator.has("name") || !animation.has(animator.get("name").getAsString())) continue;
            String name = animator.get("name").getAsString();
            for (JsonElement e : animator.getAsJsonArray("keyframes")) {
                JsonObject key = e.getAsJsonObject();
                String channel = key.get("channel").getAsString();
                JsonObject channelJson = animation.getAsJsonObject(name).getAsJsonObject(channel);
                if (channelJson == null) continue;
                double time = key.get("time").getAsDouble();
                JsonElement exported = channelJson.entrySet().stream()
                        .filter(en -> Math.abs(Double.parseDouble(en.getKey()) - time) < 1e-6)
                        .map(Map.Entry::getValue).findFirst().orElseThrow();
                JsonArray vector = exported.getAsJsonObject().getAsJsonObject("post").getAsJsonArray("vector");
                JsonObject point = key.getAsJsonArray("data_points").get(0).getAsJsonObject();
                for (int i = 0; i < 3; i++) {
                    double sign = (channel.equals("rotation") && i < 2) || (channel.equals("position") && i == 0) ? -1 : 1;
                    near(vector.get(i).getAsDouble(), point.get(new String[]{"x", "y", "z"}[i]).getAsDouble() * sign,
                            0.00002, "BB/animation key " + name + " " + channel + "@" + time);
                }
            }
        }
    }

    // Independent renderer reference: actual GeoBone and RenderUtil, not evaluator matrices.
    private static Matrix4f reference(String name, GraveDragonIdleAirPose.Frame frame, float yaw, float scale) {
        Deque<String> chain = new ArrayDeque<>();
        for (String n = name; n != null; ) {
            chain.addFirst(n);
            JsonObject b = geo.get(n);
            n = b.has("parent") ? b.get("parent").getAsString() : null;
        }
        PoseStack stack = new PoseStack();
        stack.scale(scale, scale, scale);
        stack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180 - yaw));
        stack.translate(0, 0.01f, 0);
        GeoBone parent = null;
        for (String n : chain) {
            GeoBone bone = new GeoBone(parent, n, false, null, false, false);
            JsonArray p = geo.get(n).getAsJsonArray("pivot");
            bone.updatePivot(-component(p, 0), component(p, 1), component(p, 2));
            var pose = frame.bones().get(n);
            bone.updateRotation((float)pose.rotation().x, (float)pose.rotation().y, (float)pose.rotation().z);
            bone.updatePosition((float)pose.position().x, (float)pose.position().y, (float)pose.position().z);
            bone.updateScale((float)pose.scale().x, (float)pose.scale().y, (float)pose.scale().z);
            RenderUtil.prepMatrixForBone(stack, bone);
            parent = bone;
        }
        return new Matrix4f(stack.last().pose());
    }

    private static void verifyHitDetection() {
        double q = Math.sqrt(0.5);
        OrientedBoundingBox obb = new OrientedBoundingBox(Vec3.ZERO,
                new Vec3(q, 0, q), new Vec3(0, 1, 0), new Vec3(-q, 0, q), new Vec3(4, 0.5, 0.5));
        AABB hitbox = new OrientedHitbox(obb);
        Vec3 empty = new Vec3(2.5, 0, -2.5);
        check(obb.enclosingAabb().contains(empty), "Regression ray must enter the old AABB");
        check(!hitbox.contains(empty), "Empty envelope corner is pickable");
        check(hitbox.distanceToSqr(empty) > 1, "Reach still measures envelope distance");
        Vec3 from = empty.add(0, 2, 0), to = empty.add(0, -2, 0);
        check(hitbox.clip(from, to).isEmpty(), "Melee ray hits empty corner");
        check(hitbox.inflate(0.3).clip(from, to).isEmpty(), "Projectile margin restores AABB false hit");
        check(!hitbox.intersects(AABB.ofSize(empty, 0.2, 0.2, 0.2)), "Area/block falsely hits envelope");
        check(hitbox.intersects(AABB.ofSize(Vec3.ZERO, 0.1, 0.1, 0.1)), "Actual OBB overlap missed");
        Vec3 entry = hitbox.clip(new Vec3(0, 2, 0), new Vec3(0, -2, 0)).orElseThrow();
        near(entry.y, 0.5, 1e-9, "Wrong OBB entry face");
        check(hitbox.clip(Vec3.ZERO, new Vec3(0, 2, 0)).orElseThrow().equals(Vec3.ZERO), "Inside start missed");
        check(hitbox.clip(empty, empty).isEmpty(), "Zero-length outside ray hits");
        check(hitbox.clip(Vec3.ZERO, Vec3.ZERO).isPresent(), "Zero-length inside ray misses");
        check(hitbox.clip(new Vec3(0, 2, 0), new Vec3(0, 1, 0)).isEmpty(), "Ray exceeded segment length");
        check(hitbox.clip(new Vec3(-10, 0, 0), new Vec3(10, 0, 0)).isPresent(), "Fast projectile tunnels");
        Vec3 shift = new Vec3(100, 20, -50);
        AABB moved = hitbox.move(shift).inflate(0.3);
        check(moved.clip(from.add(shift), to.add(shift)).isEmpty(), "Translated box lost OBB semantics");
        check(moved.contains(shift), "Translated center missed");
        check(hitbox.inflate(0).clip(from, to).isEmpty(), "Zero pick radius loses OBB");
        check(hitbox.move(new net.minecraft.core.BlockPos(100, 20, -50)).clip(from.add(shift), to.add(shift)).isEmpty(),
                "BlockPos translation loses OBB");
        // The nearest intersection must be the true face, not the enclosing box.
        near(hitbox.inflate(0.3).clip(new Vec3(0, 2, 0), new Vec3(0, -2, 0)).orElseThrow().y,
                0.8, 1e-9, "Projectile margin not applied to local faces");
    }

    /** Independently rotate actual BB cube vertices, then require the fitted boxes to contain them. */
    private static void verifyModelFits(JsonObject bb, List<String> labels, List<float[]> bounds) {
        float[] lower = bounds.get(labels.indexOf("jaw")), upper = bounds.get(labels.indexOf("upper_jaw"));
        for (int axis = 0; axis < 3; axis++) near(lower[axis], upper[axis], 1e-7, "Mouth halves differ in size");
        near(lower[3], upper[3], 1e-7, "Mouth halves differ in X");
        near(lower[5], upper[5], 1e-7, "Mouth halves differ in Z");
        double seam = component(geo.get("jaw").getAsJsonArray("pivot"), 1) / 16.0;
        near(lower[4] + lower[1]/2, seam, 1e-6, "Lower mouth misses hinge seam");
        near(upper[4] - upper[1]/2, seam, 1e-6, "Upper mouth misses hinge seam");
        Map<String, Integer> covered = new HashMap<>();
        for (JsonElement element : bb.getAsJsonArray("elements")) {
            JsonObject cube = element.getAsJsonObject();
            String name = cube.get("name").getAsString(), label = null;
            if (name.startsWith("new_cranium") || name.startsWith("new_eye_socket") || name.startsWith("new_brow")
                    || name.startsWith("new_heavy_brow") || name.equals("throat_bridge")) label = "head";
            // Interlocking teeth cross the seam: the two equal halves must cover their union.
            if (name.startsWith("new_nose") || name.startsWith("new_muzzle_bridge") || name.startsWith("upper_teeth")
                    || name.equals("upper_front_teeth") || name.startsWith("new_mouth_shadow")
                    || name.startsWith("new_lower_jaw") || name.startsWith("new_chin_armour")
                    || name.startsWith("new_lower_lip") || name.startsWith("new_tongue")
                    || name.startsWith("lower_teeth") || name.startsWith("lower_front_teeth")) label = "mouth_pair";
            if (name.startsWith("new_antler") || name.startsWith("new_horn_root")) {
                int suffix = Integer.parseInt(name.substring(name.lastIndexOf('_') + 1));
                boolean left = suffix >= 733;
                int segment = suffix - (left ? 733 : 719);
                label = "horn_" + (left ? "l_" : "r_") + (segment == 0 || segment == 7 ? "1" : "2");
            }
            if (label == null) continue;
            covered.merge(label, 1, Integer::sum);
            float[] b = label.equals("mouth_pair")
                    ? new float[]{upper[0], upper[1] + lower[1], upper[2], upper[3], (float)seam, upper[5]}
                    : bounds.get(labels.indexOf(label));
            Matrix4f inverse = new Matrix4f();
            if (b.length == 9) inverse.rotateZ((float)Math.toRadians(b[8])).rotateY((float)Math.toRadians(b[7]))
                    .rotateX((float)Math.toRadians(b[6]));
            inverse.invert();
            JsonArray lo = cube.getAsJsonArray("from"), hi = cube.getAsJsonArray("to"), pivot = cube.getAsJsonArray("origin");
            JsonArray rotation = cube.getAsJsonArray("rotation");
            Matrix4f transform = new Matrix4f().translate(component(pivot, 0), component(pivot, 1), component(pivot, 2))
                    .rotateZ((float)Math.toRadians(component(rotation, 2)))
                    .rotateY((float)Math.toRadians(component(rotation, 1)))
                    .rotateX((float)Math.toRadians(component(rotation, 0)))
                    .translate(-component(pivot, 0), -component(pivot, 1), -component(pivot, 2));
            for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
                Vector3f v = transform.transformPosition(new Vector3f(component(x == 0 ? lo : hi, 0),
                        component(y == 0 ? lo : hi, 1), component(z == 0 ? lo : hi, 2))).div(16).sub(b[3], b[4], b[5]);
                inverse.transformDirection(v);
                check(Math.abs(v.x) <= b[0]/2 + 1e-5 && Math.abs(v.y) <= b[1]/2 + 1e-5 && Math.abs(v.z) <= b[2]/2 + 1e-5,
                        "Fitted " + label + " misses model cube " + name);
            }
        }
        check(covered.size() == 6, "Expected head, paired mouth and four antler boxes");
        check(GraveDragonIdleAirPose.boneForPart("upper_jaw").equals("head"), "Upper jaw follows animated lower jaw");
        for (String side : new String[]{"l", "r"}) {
            float[] a = bounds.get(labels.indexOf("horn_" + side + "_1")), b = bounds.get(labels.indexOf("horn_" + side + "_2"));
            check(Math.abs(a[4] - b[4]) > 1, "Horn segments still share a center");
        }
    }

    public static void main(String[] args) throws Exception {
        icu.icuqalt10.panlingre.animation.WorldTimeAnimationControllerTest.run();
        String assets = "src/main/resources/assets/panlingre/";
        JsonObject model = read(assets + "geo/entity/boss/shihuang/grave_dragon.geo.json");
        for (JsonElement e : model.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones"))
            geo.put(e.getAsJsonObject().get("name").getAsString(), e.getAsJsonObject());
        JsonObject anim = read(assets + "animations/entity/boss/shihuang/grave_dragon.animation.json").getAsJsonObject("animations").getAsJsonObject("idle_air");
        verifyExports(read("杂项/美术资源/models/boss/ShiHuang/dragon_v4.bbmodel"), anim.getAsJsonObject("bones"));
        for (var bone : anim.getAsJsonObject("bones").entrySet()) {
            for (String channel : new String[]{"position", "rotation"}) {
                JsonObject track = bone.getValue().getAsJsonObject().getAsJsonObject(channel);
                if (track == null) continue;
                for (var key : track.entrySet()) {
                    double time = Double.parseDouble(key.getKey());
                    var pose = GraveDragonIdleAirPose.sample(time).bones().get(bone.getKey());
                    Vec3 actual = channel.equals("position") ? pose.position() : pose.rotation();
                    JsonArray values = key.getValue().getAsJsonObject().getAsJsonObject("post").getAsJsonArray("vector");
                    for (int axis = 0; axis < 3; axis++) {
                        double expected = values.get(axis).getAsDouble();
                        if (channel.equals("rotation")) expected = Math.toRadians(expected) * (axis < 2 ? -1 : 1);
                        near(axis == 0 ? actual.x : axis == 1 ? actual.y : actual.z, expected, 1e-6,
                                "Sampler differs from exported key: " + bone.getKey() + " " + channel + "@" + time);
                    }
                }
            }
        }

        // Read real labels without initializing Entity/registries in this standalone test.
        String java = Files.readString(Path.of("src/main/java/icu/icuqalt10/panlingre/entity/boss/ShiHuang/GraveDragonEntity.java"));
        String labelBlock = java.substring(java.indexOf("String[] PART_LABELS"));
        labelBlock = labelBlock.substring(0, labelBlock.indexOf("};"));
        List<String> labels = new ArrayList<>();
        var matcher = Pattern.compile("\"([^\"]+)\"").matcher(labelBlock);
        while (matcher.find()) labels.add(matcher.group(1));
        String boundBlock = java.substring(java.indexOf("float[][] HARD_CODED_PART_BOUNDS"));
        boundBlock = boundBlock.substring(0, boundBlock.indexOf("};"));
        List<float[]> bounds = new ArrayList<>();
        var rows = Pattern.compile("\\{([^{}]+)\\}").matcher(boundBlock);
        while (rows.find()) {
            String[] values = rows.group(1).split(",");
            float[] b = new float[values.length];
            for (int i = 0; i < values.length; i++) b[i] = Float.parseFloat(values[i].trim().replace("f", ""));
            bounds.add(b);
        }
        check(labels.size() == bounds.size(), "Hardcoded bounds/label count mismatch");
        verifyHitDetection();
        verifyModelFits(read("杂项/美术资源/models/boss/ShiHuang/dragon_v4.bbmodel"), labels, bounds);
        var first = GraveDragonIdleAirPose.sample(0);
        near(first.bones().get("head").position().y, 248.96428, 1e-5, "Head translation must NOT subtract first frame");
        Vec3 origin = new Vec3(526, 24, -1845);
        double maxError = 0;
        for (float yaw : new float[]{0, 37, 90, -123, 180}) {
            float scale = yaw == 37 ? 1.4f : 1;
            Matrix4f root = GraveDragonIdleAirPose.modelToEntity(yaw, scale);
            for (int step = 0; step <= 256; step++) {
                double time = step / 80.0;
                var frame = GraveDragonIdleAirPose.sample(time);
                Map<String, Matrix4f> references = new HashMap<>();
                for (int i = 0; i < labels.size(); i++) {
                    String label = labels.get(i), bone = GraveDragonIdleAirPose.boneForPart(label);
                    Matrix4f ref = references.computeIfAbsent(bone, n -> reference(n, frame, yaw, scale));
                    float[] b = bounds.get(i);
                    OrientedBoundingBox box = GraveDragonIdleAirPose.box(frame, label, b, root, origin);
                    Vec3[] corners = box.corners();
                    int corner = 0;
                    for (int x : new int[]{-1, 1}) for (int y : new int[]{-1, 1}) for (int z : new int[]{-1, 1}) {
                        Vector3f offset = new Vector3f(x*b[0]/2, y*b[1]/2, z*b[2]/2);
                        if (b.length == 9) new Matrix4f().rotateZ((float)Math.toRadians(b[8]))
                                .rotateY((float)Math.toRadians(b[7])).rotateX((float)Math.toRadians(b[6]))
                                .transformDirection(offset);
                        Vector3f v = ref.transformPosition(offset.add(b[3], b[4], b[5]));
                        Vec3 expected = origin.add(v.x, v.y, v.z);
                        Vec3 actual = corners[corner++];
                        double error = expected.distanceTo(actual);
                        maxError = Math.max(maxError, error);
                        check(error < 0.00015, "Renderer/OBB mismatch " + label + "@" + time + ": " + error);
                        check(box.enclosingAabb().inflate(1e-8).contains(actual), "Envelope misses its own OBB corner " + label);
                        // Parent-first accumulation vs cached model matrices differs in float rounding.
                        // Allow the same floating-point tolerance as the reference corner check.
                        check(box.enclosingAabb().inflate(0.00015).contains(expected), "Envelope misses rendered corner " + label);
                    }
                    near(box.distanceToSqr(box.center), 0, 1e-8, "Center outside OBB");
                }
            }
        }
        Matrix4f root = GraveDragonIdleAirPose.modelToEntity(0, 1);
        for (int i = 0; i < labels.size(); i++) {
            var zero = GraveDragonIdleAirPose.box(first, labels.get(i), bounds.get(i), root, origin);
            for (int cycle = 1; cycle <= 10; cycle++) {
                var repeat = GraveDragonIdleAirPose.box(GraveDragonIdleAirPose.sample(cycle * 3.2), labels.get(i), bounds.get(i), root, origin);
                check(zero.center.distanceTo(repeat.center) < 0.0001, "Loop mutated key data " + labels.get(i));
            }
            var before = GraveDragonIdleAirPose.box(GraveDragonIdleAirPose.sample(3.2 - 1e-6), labels.get(i), bounds.get(i), root, origin);
            var after = GraveDragonIdleAirPose.box(GraveDragonIdleAirPose.sample(3.2 + 1e-6), labels.get(i), bounds.get(i), root, origin);
            check(before.center.distanceTo(after.center) < 0.001, "Loop discontinuity " + labels.get(i));
        }
        System.out.println("PASS: " + checks + " checks; BB/GEO/animation exports agree; " + labels.size() + " parts, 257 times, 5 yaws.");
        System.out.println("Max OBB corner deviation from GeckoLib RenderUtil: " + maxError + " blocks.");
    }
}
