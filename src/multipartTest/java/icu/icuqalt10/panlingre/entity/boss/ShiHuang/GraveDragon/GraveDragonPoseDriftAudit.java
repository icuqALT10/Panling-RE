package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

import icu.icuqalt10.panlingre.entity.OrientedBoundingBox;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonPose;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Measures how far a part's oriented box travels over the idle animation cycle, and how
 * far a one-tick phase difference between client and server moves it relative to the
 * box's own size.
 *
 * <p>This quantifies the risk of driving collision boxes from the animation clock: if the
 * client renders one phase while the server validates against another, the boxes leave
 * the visible part, so hits become intermittent and side dependent. The bounds table is
 * parsed from source so this stays a pure offline diagnostic.
 */
public final class GraveDragonPoseDriftAudit {
    private static final Path ENTITY = Path.of(
            "src/main/java/icu/icuqalt10/panlingre/entity/boss/ShiHuang/GraveDragonEntity.java");
    private static final int[] PARTS = {0, 4, 6, 11, 12, 13, 22, 24, 27, 35, 37, 40, 48, 50, 53, 65, 74, 78};
    private static final double[] PHASES = {0.0, 0.4, 0.8, 1.2, 1.6, 2.0, 2.4, 2.8, 3.2, 3.6};

    public static void main(String[] args) throws Exception {
        String source = Files.readString(ENTITY, StandardCharsets.UTF_8);
        List<String> labels = new ArrayList<>();
        Matcher labelMatcher = Pattern.compile("\"([^\"]+)\"")
                .matcher(section(source, "String[] PART_LABELS = {", "};"));
        while (labelMatcher.find()) labels.add(labelMatcher.group(1));
        List<float[]> bounds = new ArrayList<>();
        Matcher rowMatcher = Pattern.compile("\\{([^{}]+)}")
                .matcher(section(source, "HARD_CODED_PART_BOUNDS = {", "\n    };"));
        while (rowMatcher.find()) {
            String[] values = rowMatcher.group(1).split(",");
            float[] row = new float[values.length];
            for (int i = 0; i < values.length; i++) {
                row[i] = Float.parseFloat(values[i].trim().replace("f", ""));
            }
            bounds.add(row);
        }
        if (labels.size() != bounds.size()) {
            throw new IllegalStateException("labels=" + labels.size() + " rows=" + bounds.size());
        }

        var transform = GraveDragonPose.modelToEntity(0.0F, 1.0F);
        System.out.printf("%-6s %-22s %9s %9s %10s %8s%n",
                "part", "label", "travel", "boxSize", "1tickErr", "ratio");
        double worstRatio = 0;
        String worstName = "";
        for (int index : PARTS) {
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
            double maxTickError = 0, size = 0;
            for (double phase : PHASES) {
                OrientedBoundingBox box = boxAt(transform, labels.get(index), bounds.get(index), phase);
                minX = Math.min(minX, box.center.x); maxX = Math.max(maxX, box.center.x);
                minY = Math.min(minY, box.center.y); maxY = Math.max(maxY, box.center.y);
                minZ = Math.min(minZ, box.center.z); maxZ = Math.max(maxZ, box.center.z);
                size = Math.max(size, Math.max(box.halfExtents.x * 2,
                        Math.max(box.halfExtents.y * 2, box.halfExtents.z * 2)));
                OrientedBoundingBox next = boxAt(transform, labels.get(index), bounds.get(index),
                        phase + 1.0 / 20.0);
                maxTickError = Math.max(maxTickError, box.center.distanceTo(next.center));
            }
            double travel = Math.sqrt(Math.pow(maxX - minX, 2) + Math.pow(maxY - minY, 2) + Math.pow(maxZ - minZ, 2));
            double ratio = maxTickError / Math.max(0.001, size);
            if (ratio > worstRatio) {
                worstRatio = ratio;
                worstName = labels.get(index);
            }
            System.out.printf("%-6d %-22s %9.3f %9.3f %10.3f %7.2fx%n",
                    index, labels.get(index), travel, size, maxTickError, ratio);
        }
        System.out.printf("%nworst one-tick mismatch / box size: %.2fx (%s)%n", worstRatio, worstName);
        System.out.println("ratio near or above 1.0 means the box leaves the visible part entirely.");
    }

    private static OrientedBoundingBox boxAt(org.joml.Matrix4f transform, String label,
                                             float[] bounds, double phase) {
        return GraveDragonPose.box(GraveDragonPose.sample(phase), label, bounds,
                transform, Vec3.ZERO);
    }

    private static String section(String source, String from, String to) {
        int start = source.indexOf(from);
        int end = source.indexOf(to, start);
        if (start < 0 || end < 0) throw new IllegalStateException("cannot locate " + from);
        return source.substring(start, end);
    }
}
