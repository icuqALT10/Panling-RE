package icu.icuqalt10.panlingre.util;

import icu.icuqalt10.panlingre.entity.MultipartEntity;
import icu.icuqalt10.panlingre.entity.OrientedBoundingBox;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/** A skill target keeps its living owner, selected body part and actual aim point together. */
public final class SkillTargeting {
    public record Target(LivingEntity root, Entity part, Vec3 point) {
        public boolean hurt(DamageSource source, float amount) {
            if (part instanceof MultipartEntity.MultipartPart child && root instanceof MultipartEntity multipart)
                return multipart.hurtPart(child.getPartIndex(), source, amount);
            return part.hurt(source, amount);
        }
    }

    public static OrientedBoundingBox box(Entity part) {
        if (part instanceof MultipartEntity.OrientedPart oriented) return oriented.getOrientedBox();
        AABB bounds = part.getBoundingBox();
        return OrientedBoundingBox.axisAligned(bounds.getCenter(), bounds.getXsize(), bounds.getYsize(), bounds.getZsize());
    }

    public static Vec3 closestPoint(Entity part, Vec3 origin) {
        var box = box(part);
        Vec3 offset = origin.subtract(box.center);
        return box.center.add(box.axisX.scale(Math.clamp(offset.dot(box.axisX), -box.halfExtents.x, box.halfExtents.x)))
                .add(box.axisY.scale(Math.clamp(offset.dot(box.axisY), -box.halfExtents.y, box.halfExtents.y)))
                .add(box.axisZ.scale(Math.clamp(offset.dot(box.axisZ), -box.halfExtents.z, box.halfExtents.z)));
    }

    public static Target nearest(LivingEntity root, Vec3 origin) {
        return nearest(root, origin, part -> true);
    }

    /** Select only parts actually touched by the skill area, measured from its impact. */
    public static Target inArea(LivingEntity root, Vec3 impact, AABB area) {
        return nearest(root, impact, part -> box(part).intersects(area));
    }

    public static boolean hurtInArea(LivingEntity root, Vec3 impact, AABB area, DamageSource source, float amount) {
        Target contact = inArea(root, impact, area);
        return contact != null && contact.hurt(source, amount);
    }

    private static Target nearest(LivingEntity root, Vec3 origin, Predicate<Entity> accepts) {
        Target best = null;
        double distance = Double.POSITIVE_INFINITY;
        for (Entity part : MultipartEntity.targetParts(root)) {
            if (part == null || part.isRemoved() || !accepts.test(part)) continue;
            Vec3 point = closestPoint(part, origin);
            double next = point.distanceToSqr(origin);
            if (next < distance) { best = new Target(root, part, point); distance = next; }
        }
        return best;
    }

    /** First actual contact along a projectile segment, including a nearer blocking wall. */
    public static HitResult projectileHit(Entity projectile, Vec3 from, Vec3 to, Predicate<Entity> canHit) {
        var level = projectile.level();
        HitResult best = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, projectile));
        double distance = best.getType() == HitResult.Type.MISS ? Double.POSITIVE_INFINITY
                : best.getLocation().distanceToSqr(from);
        AABB area = new AABB(from, to).inflate(.3);
        for (LivingEntity root : MultipartEntity.collectTargets(level, area, null)) {
            if (!canHit.test(root)) continue;
            for (Entity part : MultipartEntity.targetParts(root)) {
                if (part == null || part.isRemoved()) continue;
                var hit = box(part).inflate(.3, .3, .3).clip(from, to);
                if (hit.isEmpty()) continue;
                double next = hit.get().distanceToSqr(from);
                if (next < distance) {
                    best = new EntityHitResult(part, hit.get());
                    distance = next;
                }
            }
        }
        return best;
    }

    /** One part per owner, ordered by crosshair angle first and distance second. */
    public static List<Target> aimed(LivingEntity caster, AABB area, Vec3 origin, Vec3 look,
                                     double range, double maxAngle) {
        Vec3 direction = look.normalize();
        Comparator<Target> order = Comparator.comparingDouble((Target t) -> angle(direction, t.point.subtract(origin)))
                .thenComparingDouble(t -> t.point.distanceToSqr(origin));
        List<Target> targets = new ArrayList<>();
        for (LivingEntity root : MultipartEntity.collectTargets(caster.level(), area, caster)) {
            if (!SkillHelper.combatTargetFilter(caster).test(root)) continue;
            Target best = null;
            for (Entity part : MultipartEntity.targetParts(root)) {
                if (part == null || part.isRemoved() || !box(part).intersects(area)) continue;
                Vec3 point = aimPoint(part, origin, direction, range);
                if (point.distanceToSqr(origin) > range * range || angle(direction, point.subtract(origin)) > maxAngle) continue;
                Target candidate = new Target(root, part, point);
                if (best == null || order.compare(candidate, best) < 0) best = candidate;
            }
            if (best != null) targets.add(best);
        }
        targets.sort(order);
        return targets;
    }

    private static double angle(Vec3 view, Vec3 offset) {
        return offset.lengthSqr() < 1E-12 ? 0 : Math.acos(Math.clamp(view.dot(offset.normalize()), -1, 1)) * 180 / Math.PI;
    }

    // A ray hit has zero angular error. Otherwise the minimum lies on a box edge;
    // evaluate its endpoints and the stationary point of dot(view, p) / length(p).
    public static Vec3 aimPoint(Entity part, Vec3 origin, Vec3 view, double range) {
        var shape = box(part);
        var hit = shape.clip(origin, origin.add(view.scale(range)));
        if (hit.isPresent()) return hit.get();
        Vec3[] corners = shape.corners();
        Vec3 best = closestPoint(part, origin);
        double bestAngle = angle(view, best.subtract(origin));
        for (int i = 0; i < 8; i++) for (int bit : new int[]{1, 2, 4}) {
            int j = i ^ bit;
            if (j < i) continue;
            Vec3 a = corners[i].subtract(origin), b = corners[j].subtract(corners[i]);
            double av = a.dot(view), bv = b.dot(view), ab = a.dot(b);
            double denominator = bv * ab - av * b.lengthSqr();
            double stationary = Math.abs(denominator) < 1E-12 ? 0 : (av * ab - bv * a.lengthSqr()) / denominator;
            for (double t : new double[]{0, 1, Math.clamp(stationary, 0, 1)}) {
                Vec3 point = corners[i].add(b.scale(t));
                double error = angle(view, point.subtract(origin));
                if (error < bestAngle || Math.abs(error - bestAngle) < 1E-9 && point.distanceToSqr(origin) < best.distanceToSqr(origin)) {
                    best = point;
                    bestAngle = error;
                }
            }
        }
        return best;
    }

    private SkillTargeting() { }
}
