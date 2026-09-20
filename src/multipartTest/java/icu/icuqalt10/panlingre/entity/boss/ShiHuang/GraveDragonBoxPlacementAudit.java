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
 * Compares each configured part box against the bounds of that bone's geo cubes.
 *
 * <p><b>KNOWN BROKEN — do not trust its output.</b> The geometry this tool builds is mirrored
 * along Z relative to the space the configured centres live in: it reports {@code tail_tip} at
 * Z -39.4 while the table says +38.8, and every neck at negative Z while the table says
 * positive. Magnitudes agree, signs do not, so every part shows a large bogus offset. The
 * authoritative check is {@link GraveDragonPickMappingTest}, which drives the real production
 * code instead of re-deriving the transform, and that one reports every part resolving to
 * itself. Fix the transform here before using this audit for anything.
 *
 * <p>Kept only so the mistake is not repeated: re-deriving the bone chain by hand is how the
 * earlier AABB fatness numbers were produced, and those need re-measuring with the production
 * transform before any decision rests on them.
 */
public final class GraveDragonBoxPlacementAudit {
    private static final String GEO = "assets/panlingre/geo/entity/boss/shihuang/grave_dragon.geo.json";

    public static void main(String[] args) throws Exception {
        var geometry = loadGeometry();
        var transform = GraveDragonIdleAirPose.modelToEntity(0.0F, 1.0F);
        var frame = GraveDragonIdleAirPose.sample(0.0);
        var labels = readLabels();
        var bounds = readBounds();

        System.out.printf("%-22s %-24s %-24s %8s %8s%n",
                "part", "configured centre", "geometry centre", "offset", "sizeErr");

        List<String> worst = new ArrayList<>();
        double worstOffset = 0;
        for (int index = 0; index < labels.length; index++) {
            String label = labels[index];
            String bone = GraveDragonIdleAirPose.boneForPart(label);
            List<Vec3> local = geometry.get(bone);
            if (local == null || local.isEmpty()) continue;

            Matrix4f toModel = new Matrix4f(transform).mul(frame.matrices().get(bone));
            List<Vec3> world = new ArrayList<>(local.size());
            for (Vec3 p : local) {
                var v = new Vector3f((float) p.x, (float) p.y, (float) p.z);
                toModel.transformPosition(v);
                world.add(new Vec3(v.x, v.y, v.z));
            }

            float[] b = bounds[index];
            Vec3 configured = new Vec3(b[3], b[4], b[5]);
            Vec3 geometryCentre = centre(world);
            double offset = configured.distanceTo(geometryCentre);
            double[] extent = extent(world);
            double sizeError = Math.max(Math.abs(extent[0] - b[0]),
                    Math.max(Math.abs(extent[1] - b[1]), Math.abs(extent[2] - b[2])));

            if (offset > 0.5) worst.add(label + "(" + String.format("%.2f", offset) + ")");
            worstOffset = Math.max(worstOffset, offset);

            System.out.printf("%-22s (%6.2f,%6.2f,%6.2f)   (%6.2f,%6.2f,%6.2f)   %8.2f %8.2f%s%n",
                    label, configured.x, configured.y, configured.z,
                    geometryCentre.x, geometryCentre.y, geometryCentre.z,
                    offset, sizeError,
                    offset > 0.5 ? "   <-- OFFSET" : "");
        }

        System.out.printf("%nworst offset %.2f%n", worstOffset);
        System.out.println("parts offset by more than 0.5: " + worst);
    }

    private static Vec3 centre(List<Vec3> points) {
        double x = 0, y = 0, z = 0;
        for (Vec3 p : points) { x += p.x; y += p.y; z += p.z; }
        int n = points.size();
        return new Vec3(x / n, y / n, z / n);
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
        try (var in = GraveDragonBoxPlacementAudit.class.getClassLoader().getResourceAsStream(GEO)) {
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
