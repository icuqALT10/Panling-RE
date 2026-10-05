package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** A single ground-connected flood fill, shared by damage and particle placement. */
public final class GraveDragonFireField {
    public static final int RADIUS = 200;
    public static final double HEIGHT = 1.5;
    private static final int WIDTH = RADIUS * 2 + 1;
    private final BlockPos origin;
    private final float[] floors = new float[WIDTH * WIDTH];
    private final List<Vec3> cells = new ArrayList<>();

    public GraveDragonFireField(BlockPos origin) {
        this.origin = origin;
        Arrays.fill(floors, Float.NaN);
    }

    public BlockPos origin() { return origin; }
    public List<Vec3> cells() { return cells; }

    private int index(int x, int z) {
        int dx = x - origin.getX(), dz = z - origin.getZ();
        return Math.abs(dx) <= RADIUS && Math.abs(dz) <= RADIUS
                ? (dx + RADIUS) * WIDTH + dz + RADIUS : -1;
    }

    public void add(int x, int z, float floor) {
        int index = index(x, z);
        if (index < 0 || !Float.isNaN(floors[index])) return;
        floors[index] = floor;
        cells.add(new Vec3(x + .5, floor, z + .5));
    }

    public boolean intersects(AABB box) {
        for (int x = (int)Math.floor(box.minX); x < (int)Math.ceil(box.maxX); x++)
            for (int z = (int)Math.floor(box.minZ); z < (int)Math.ceil(box.maxZ); z++) {
                int index = index(x, z);
                if (index < 0) continue;
                float floor = floors[index];
                if (!Float.isNaN(floor) && box.maxY > floor && box.minY < floor + HEIGHT) return true;
            }
        return false;
    }

    public static GraveDragonFireField spread(Level level, Entity source, Vec3 impact) {
        GraveDragonFireField field = new GraveDragonFireField(BlockPos.containing(impact));
        Vec3 start = surface(level, source, field.origin.getX(), field.origin.getZ(), impact.y);
        if (start == null) return field;
        field.add(field.origin.getX(), field.origin.getZ(), (float)start.y);
        for (int cursor = 0; cursor < field.cells.size(); cursor++) {
            Vec3 cell = field.cells.get(cursor);
            int x = (int)Math.floor(cell.x), z = (int)Math.floor(cell.z);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                int nx = x + dx, nz = z + dz;
                int rx = nx - field.origin.getX(), rz = nz - field.origin.getZ();
                if (rx * rx + rz * rz > RADIUS * RADIUS) continue;
                int index = field.index(nx, nz);
                if (!Float.isNaN(field.floors[index])) continue;
                Vec3 next = surface(level, source, nx, nz, cell.y);
                if (next == null) continue;
                // Diagonals cannot squeeze through two touching wall corners.
                if (dx != 0 && dz != 0 && (surface(level, source, nx, z, cell.y) == null
                        || surface(level, source, x, nz, cell.y) == null)) continue;
                field.add(nx, nz, (float)next.y);
            }
        }
        return field;
    }

    private static Vec3 surface(Level level, Entity source, int x, int z, double ceiling) {
        if (!level.hasChunkAt(BlockPos.containing(x, ceiling, z))) return null;
        // Flow goes along the floor or down a step, never up a wall or onto the roof.
        AABB entry = new AABB(x + .05, ceiling + .02, z + .05,
                x + .95, ceiling + HEIGHT, z + .95);
        if (level.getBlockCollisions(source, entry).iterator().hasNext()) return null;
        Vec3 from = new Vec3(x + .5, ceiling + .02, z + .5);
        // A long ray can report a point inside the top block; probe nearby floor first.
        var hit = level.clip(new ClipContext(from, new Vec3(x + .5, ceiling - 2, z + .5),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
        if (hit.getType() == HitResult.Type.MISS)
            hit = level.clip(new ClipContext(from, new Vec3(x + .5, level.getMinBuildHeight(), z + .5),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) return null;
        double floor = hit.getLocation().y;
        if (floor < ceiling - .01 && level.getBlockCollisions(source,
                new AABB(x + .05, floor + .02, z + .05, x + .95, floor + HEIGHT, z + .95)).iterator().hasNext()) return null;
        return hit.getLocation();
    }
}
