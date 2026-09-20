package icu.icuqalt10.panlingre.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;

/**
 * Vanilla API adapter: min/max are only a broad-phase envelope. ProjectileUtil
 * calls inflate/clip/contains and player reach calls distanceToSqr; those
 * operations all retain the actual OBB, including after moving or inflating.
 * Methods used to construct search regions (expandTowards/minmax) intentionally
 * inherit the ordinary conservative AABB implementation.
 */
public final class OrientedHitbox extends AABB {
    private final OrientedBoundingBox box;

    public OrientedHitbox(OrientedBoundingBox box) {
        this(box, box.enclosingAabb());
    }

    private OrientedHitbox(OrientedBoundingBox box, AABB envelope) {
        super(envelope.minX, envelope.minY, envelope.minZ, envelope.maxX, envelope.maxY, envelope.maxZ);
        this.box = box;
    }

    @Override public Optional<Vec3> clip(Vec3 from, Vec3 to) { return box.clip(from, to); }
    @Override public boolean contains(double x, double y, double z) { return box.contains(new Vec3(x, y, z)); }
    @Override public double distanceToSqr(Vec3 point) { return box.distanceToSqr(point); }
    @Override public boolean intersects(double x1, double y1, double z1, double x2, double y2, double z2) {
        return box.intersects(new AABB(x1, y1, z1, x2, y2, z2));
    }
    @Override public AABB inflate(double x, double y, double z) {
        return new OrientedHitbox(box.inflate(x, y, z));
    }
    @Override public AABB move(double x, double y, double z) {
        return new OrientedHitbox(box.move(new Vec3(x, y, z)));
    }
    @Override public AABB move(BlockPos offset) { return move(offset.getX(), offset.getY(), offset.getZ()); }
}
