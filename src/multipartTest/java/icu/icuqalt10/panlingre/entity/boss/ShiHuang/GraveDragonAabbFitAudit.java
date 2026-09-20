package icu.icuqalt10.panlingre.entity.boss.ShiHuang;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reports, for every part, the world-space extent of its visible geometry and how much of the
 * enclosing axis-aligned box is actually occupied.
 *
 * <p>A part whose geometry is long and thin needs several small boxes: one box around a thin
 * diagonal shape wastes most of its volume. The "fill" column is that ratio, and the split
 * count column is the smallest uniform split of the longest world axis that brings the
 * wasted volume down to about a quarter. Those two numbers are what decides the budget.
 */
public final class GraveDragonAabbFitAudit {
    private static final String GEO = "assets/panlingre/geo/entity/boss/shihuang/grave_dragon.geo.json";

    public static void main(String[] args) throws Exception {
        var geometry = loadGeometry();
        var transform = GraveDragonIdleAirPose.modelToEntity(0.0F, 1.0F);
        var frame = GraveDragonIdleAirPose.sample(0.0);
        var labels = readLabels();
        var bounds = readBounds();

        System.out.printf("%-22s %7s %7s %7s %8s %8s%n",
                "part", "sizeX", "sizeY", "sizeZ", "boxVol", "needed");

        double totalBox = 0, totalNeeded = 0;
        int counted = 0;
        List<String> wasteful = new ArrayList<>();

        for (int index = 0; index < labels.length; index++) {
            String label = labels[index];
            String bone = GraveDragonIdleAirPose.boneForPart(label);
            List<Vec3> all = geometry.get(bone);
            if (all == null || all.isEmpty()) continue;
            var obb = GraveDragonIdleAirPose.box(frame, label, bounds[index], transform, Vec3.ZERO);
            if (obb.halfExtents.lengthSqr() <= 1.0e-9) continue;

            // Every geometry vertex that this part's oriented box is responsible for.
            List<Vec3> world = new ArrayList<>();
            Matrix4f toModel = new Matrix4f(transform).mul(frame.matrices().get(bone));
            for (Vec3 local : all) {
                var v = new Vector3f((float) local.x, (float) local.y, (float) local.z);
                toModel.transformPosition(v);
                Vec3 p = new Vec3(v.x, v.y, v.z);
                if (obb.contains(p)) world.add(p);
            }
            if (world.size() < 8) continue;
            counted++;

            // What one axis-aligned box costs, versus the sum of tight per-cube boxes.
            double[] extent = extent(world);
            double boxVolume = extent[0] * extent[1] * extent[2];
            double needed = tightVolume(world);
            totalBox += boxVolume;
            totalNeeded += needed;
            double waste = boxVolume / needed;

            if (waste > 1.6) wasteful.add(label + "(" + String.format("%.1f", waste) + "x)");

            System.out.printf("%-22s %7.2f %7.2f %7.2f %8.3f %8.3f   waste %.1fx%n",
                    label, extent[0], extent[1], extent[2], boxVolume, needed, waste);
        }

        System.out.printf("%ntotal box volume %.1f vs tight %.1f  ->  one box per part wastes %.1f%%%n",
                totalBox, totalNeeded, 100.0 * (totalBox - totalNeeded) / totalBox);
        System.out.println("parts wasting most: " + wasteful);
    }

    /** Volume of one tight axis-aligned box per cube, which is the best an AABB tree can do. */
    private static double tightVolume(List<Vec3> points) {
        // Vertices arrive eight per cube, in order, so each consecutive eight form one cube.
        double total = 0;
        for (int i = 0; i + 8 <= points.size(); i += 8) {
            double[] extent = extent(points.subList(i, i + 8));
            total += extent[0] * extent[1] * extent[2];
        }
        return total;
    }

    private static double[] extent(List<Vec3> points) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (Vec3 p : points) {
            minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);
            minZ = Math.min(minZ, p.z); maxZ = Math.max(maxZ, p.z);
        }
        return new double[]{maxX - minX, maxY - minY, maxZ - minZ};
    }

    private static java.util.Map<String, List<Vec3>> loadGeometry() throws Exception {
        JsonObject geo;
        try (var in = GraveDragonAabbFitAudit.class.getClassLoader().getResourceAsStream(GEO)) {
            if (in == null) throw new IllegalStateException("missing " + GEO);
            geo = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        var result = new java.util.LinkedHashMap<String, List<Vec3>>();
        for (var element : geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                .getAsJsonArray("bones")) {
            var bone = element.getAsJsonObject();
            String name = bone.get("name").getAsString();
            if (!bone.has("cubes")) continue;
            List<Vec3> points = new ArrayList<>();
            for (var cubeElement : bone.getAsJsonArray("cubes")) {
                var cube = cubeElement.getAsJsonObject();
                if (!cube.has("origin") || !cube.has("size")) continue;
                var origin = cube.getAsJsonArray("origin");
                var size = cube.getAsJsonArray("size");
                var pivot = cube.has("pivot") ? cube.getAsJsonArray("pivot") : null;
                var rotation = cube.has("rotation") ? cube.getAsJsonArray("rotation") : null;
                Matrix4f local = new Matrix4f();
                if (pivot != null) local.translate(pivot.get(0).getAsFloat(), pivot.get(1).getAsFloat(), pivot.get(2).getAsFloat());
                if (rotation != null) {
                    local.rotateZ((float) Math.toRadians(rotation.get(2).getAsFloat()))
                            .rotateY((float) Math.toRadians(rotation.get(1).getAsFloat()))
                            .rotateX((float) Math.toRadians(rotation.get(0).getAsFloat()));
                }
                if (pivot != null) local.translate(-pivot.get(0).getAsFloat(), -pivot.get(1).getAsFloat(), -pivot.get(2).getAsFloat());
                for (int sx = 0; sx < 2; sx++) for (int sy = 0; sy < 2; sy++) for (int sz = 0; sz < 2; sz++) {
                    var v = new Vector3f(
                            origin.get(0).getAsFloat() + sx * size.get(0).getAsFloat(),
                            origin.get(1).getAsFloat() + sy * size.get(1).getAsFloat(),
                            origin.get(2).getAsFloat() + sz * size.get(2).getAsFloat());
                    local.transformPosition(v).mul(1.0F / 16.0F);
                    points.add(new Vec3(v.x, v.y, v.z));
                }
            }
            if (!points.isEmpty()) result.put(name, points);
        }
        return result;
    }

    private static final java.nio.file.Path ENTITY_SOURCE = java.nio.file.Path.of(
            "src/main/java/icu/icuqalt10/panlingre/entity/boss/ShiHuang/GraveDragonEntity.java");

    private static String[] readLabels() throws Exception {
        String source = java.nio.file.Files.readString(ENTITY_SOURCE, StandardCharsets.UTF_8);
        int start = source.indexOf("String[] PART_LABELS = {");
        int end = source.indexOf("};", start);
        var matcher = java.util.regex.Pattern.compile("\"([^\"]+)\"").matcher(source.substring(start, end));
        List<String> labels = new ArrayList<>();
        while (matcher.find()) labels.add(matcher.group(1));
        return labels.toArray(new String[0]);
    }

    private static float[][] readBounds() throws Exception {
        String source = java.nio.file.Files.readString(ENTITY_SOURCE, StandardCharsets.UTF_8);
        int start = source.indexOf("HARD_CODED_PART_BOUNDS = {");
        int end = source.indexOf("\n    };", start);
        var matcher = java.util.regex.Pattern.compile("\\{([^{}]+)}").matcher(source.substring(start, end));
        List<float[]> rows = new ArrayList<>();
        while (matcher.find()) {
            String[] values = matcher.group(1).split(",");
            float[] row = new float[values.length];
            for (int i = 0; i < values.length; i++) row[i] = Float.parseFloat(values[i].trim().replace("f", ""));
            rows.add(row);
        }
        return rows.toArray(new float[0][]);
    }
}
