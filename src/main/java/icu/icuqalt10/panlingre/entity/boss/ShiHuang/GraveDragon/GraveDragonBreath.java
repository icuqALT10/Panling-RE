package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

import icu.icuqalt10.panlingre.entity.OrientedBoundingBox;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Shared head transform, jet and ground spread for server damage and client effects. */
public final class GraveDragonBreath {
    public record Geometry(Vec3 mouth, Vec3 direction, double range, double width) {
        public double radiusAt(double distance) { return width * (.2 + distance * .20); }
        public Vec3 pointAt(double distance) { return mouth.add(direction.scale(distance)); }
        public boolean contains(Vec3 point) {
            Vec3 toPoint = point.subtract(mouth);
            double along = toPoint.dot(direction);
            return along > 0 && along <= range
                    && toPoint.subtract(direction.scale(along)).lengthSqr() <= radiusAt(along) * radiusAt(along);
        }
    }

    public record Flow(Geometry geometry, Vec3 ground, double spreadRadius) {
        public boolean containsGround(Vec3 point) {
            if (ground == null || spreadRadius <= 0 || Math.abs(point.y - ground.y) >= 2.5) return false;
            double x = point.x - ground.x, z = point.z - ground.z;
            return x * x + z * z <= spreadRadius * spreadRadius;
        }
    }

    public static Geometry geometry(GraveDragonEntity dragon) {
        return geometry(dragon, dragon.animation());
    }

    public static Geometry geometry(GraveDragonEntity dragon, String action) {
        var parts = dragon.getWorldParts();
        return geometry(parts[11].getOrientedBox(), parts[78].getOrientedBox(),
                parts[12].getOrientedBox(), action);
    }

    static Geometry geometry(GraveDragonPose.Frame frame, Matrix4f transform, Vec3 origin, String action) {
        return geometry(GraveDragonPose.box(frame, GraveDragonEntity.PART_LABELS[11],
                        GraveDragonEntity.PART_BOUNDS[11], transform, origin),
                GraveDragonPose.box(frame, GraveDragonEntity.PART_LABELS[78],
                        GraveDragonEntity.PART_BOUNDS[78], transform, origin),
                GraveDragonPose.box(frame, GraveDragonEntity.PART_LABELS[12],
                        GraveDragonEntity.PART_BOUNDS[12], transform, origin), action);
    }

    private static Geometry geometry(OrientedBoundingBox head, OrientedBoundingBox upper,
                                     OrientedBoundingBox lower, String action) {
        Vec3 direction = head.axisZ.scale(-1).normalize();
        Vec3 upperLip = upper.center.subtract(upper.axisY.scale(upper.halfExtents.y))
                .subtract(upper.axisZ.scale(upper.halfExtents.z));
        Vec3 lowerLip = lower.center.add(lower.axisY.scale(lower.halfExtents.y))
                .subtract(lower.axisZ.scale(lower.halfExtents.z));
        Vec3 mouth = upperLip.lerp(lowerLip, .5);
        double range = action.equals("phase50_firefield") ? 80 : action.equals("ground_turn_sweep") ? 20 : 30;
        double width = action.equals("ground_turn_breath") ? 2 :
                action.equals("ground_sweep_breath") ? 1.5 : 1;
        return new Geometry(mouth, direction, range, width);
    }

    public static Flow flow(GraveDragonEntity dragon, String action, double seconds) {
        return flow(dragon, action, seconds, geometry(dragon, action));
    }

    private static Flow flow(GraveDragonEntity dragon, String action, double seconds, Geometry source) {
        GraveDragonActions.Window window = GraveDragonActions.get(action).windows().getFirst();
        double elapsedTicks = Math.max(0, (seconds - window.from()) * 20 + 1);
        boolean field = action.equals("phase50_firefield");
        boolean spread = field || action.equals("ground_turn_sweep") || action.equals("ground_turn_breath")
                || action.equals("ground_sweep_breath");
        double front = spread ? elapsedTicks * 6 : Math.min(source.range(), elapsedTicks * 6);
        var hit = dragon.level().clip(new ClipContext(source.mouth(), source.pointAt(front),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dragon));
        double distance = hit.getType() == HitResult.Type.MISS ? front
                : source.mouth().distanceTo(hit.getLocation());
        Geometry jet = new Geometry(source.mouth(), source.direction(), distance, source.width());
        if (!spread || hit.getType() == HitResult.Type.MISS || hit.getDirection() != Direction.UP)
            return new Flow(jet, null, 0);
        // The phase field uses its first impact to build a cached flood fill.
        if (field) return new Flow(jet, hit.getLocation(), 0);
        double contactTicks = Math.max(1, Math.ceil(distance / 6));
        double radius = Math.min(15, Math.max(0, elapsedTicks - contactTicks + 1) * (15.0 / 3));
        return new Flow(jet, hit.getLocation(), radius);
    }

    /** Test the actual attack poses without changing the live animation or OBBs. */
    public static boolean canReach(GraveDragonEntity dragon, String action, Player target, float yaw) {
        var window = GraveDragonActions.get(action).windows().getFirst();
        var transform = GraveDragonPose.modelToEntity(yaw, dragon.bodyPitch(), dragon.getScale());
        int ticks = (int) Math.ceil((window.to() - window.from()) * 20);
        for (int tick = 0; tick < ticks; tick += 3) {
            double seconds = window.from() + tick / 20.0;
            var frame = GraveDragonPose.sample(action, seconds, false);
            if (hits(dragon, flow(dragon, action, seconds,
                    geometry(frame, transform, dragon.position(), action)), target)) return true;
        }
        return false;
    }

    public static boolean hits(GraveDragonEntity dragon, Flow flow, Player target) {
        Vec3 eye = target.getEyePosition();
        return flow.geometry().contains(eye) && clearLine(dragon, flow.geometry().mouth(), eye)
                || flow.containsGround(target.position()) && clearLine(dragon, flow.ground().add(0, .2, 0), eye);
    }

    /** Translate the sampled head jet onto the locked target while keeping the dragon on its floor. */
    static Vec3 firingPosition(GraveDragonEntity dragon, String action, double seconds,
                               float yaw, Player target) {
        var frame = GraveDragonPose.sample(action, seconds, false);
        Geometry source = geometry(frame,
                GraveDragonPose.modelToEntity(yaw, dragon.bodyPitch(), dragon.getScale()), dragon.position(), action);
        Vec3 aim = target.getEyePosition();
        double along = source.direction().y < -.01
                ? (aim.y - source.mouth().y) / source.direction().y : source.range() * .75;
        if (along <= 0) return null;
        Vec3 shift = aim.subtract(source.pointAt(along)).multiply(1, 0, 1);
        return hits(dragon, flow(dragon, action, seconds,
                new Geometry(source.mouth().add(shift), source.direction(), source.range(), source.width())), target)
                ? dragon.position().add(shift) : null;
    }

    private static boolean clearLine(GraveDragonEntity dragon, Vec3 from, Vec3 to) {
        return dragon.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, dragon)).getType() == HitResult.Type.MISS;
    }

    private GraveDragonBreath() { }
}
