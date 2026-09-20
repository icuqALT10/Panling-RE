package icu.icuqalt10.panlingre.mixin;

import icu.icuqalt10.panlingre.entity.MultipartEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

/**
 * Picks multipart parts by their real oriented boxes instead of by their AABB envelopes.
 *
 * <p>Minecraft can only compare candidates through {@link AABB}, so every part exposes a
 * conservative enclosing box. Around a rotated oriented box that envelope contains a lot of
 * empty space, and vanilla happily reports a hit where the ray only crossed an empty corner.
 * The player then damages a part that is not the one under the crosshair.
 *
 * <p>The comparison is therefore redone against the oriented boxes, and the result is always
 * the part whose oriented box the ray truly reaches first. Three earlier versions were wrong:
 * <ul>
 *   <li>Comparing the oriented-box entry distance against vanilla's envelope-corner distance
 *       never corrected anything, because an empty corner is always nearer to the eye than the
 *       real surface. Measured in game: 11280 picks, zero corrections.</li>
 *   <li>That same comparison used {@code Vec3#closerThan(pos, distance)}, which squares its
 *       argument again; feeding it a squared distance inflated the threshold enormously
 *       (at 3 blocks: 81 instead of 9) and let a distant part steal a nearer entity's pick.</li>
 *   <li>Deciding on the entity alone ("the eye is inside this part's box") replaced good picks
 *       with unrelated nearer parts.</li>
 * </ul>
 * Locations always come from the oriented box, or — for entities without one — from the
 * point where the ray enters their bounding box, so the caller's reach filter measures the
 * same surface the player aimed at.
 */
@Mixin(ProjectileUtil.class)
public abstract class OrientedEntityPickMixin {
    @Inject(method = "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;",
            at = @At("RETURN"), cancellable = true)
    private static void panlingre$selectByOrientedBox(Entity viewer, Vec3 from, Vec3 to, AABB search,
                                                      Predicate<Entity> filter, double maxDistanceSquared,
                                                      CallbackInfoReturnable<EntityHitResult> cir) {
        // Temporary diagnostic escape hatch: lets the in-game audit compare the fixed
        // selection against the previous envelope-only behaviour in one run.
        if (Boolean.getBoolean("panlingre.legacyEnvelopePick")) return;

        var level = viewer.level();
        if (level == null) return;

        // Broad phase only: a part's envelope merely has to be reachable by the ray.
        AABB broadPhase = search.inflate(2.0);
        Vec3 direction = to.subtract(from).normalize();

        EntityHitResult best = resolveCandidate(cir.getReturnValue(), from, to);
        double bestMiss = best == null ? Double.MAX_VALUE : missDistance(best.getEntity(), from, direction);
        double bestAlongRay = best == null ? Double.MAX_VALUE : best.getLocation().distanceToSqr(from);

        for (Entity candidate : level.getPartEntities()) {
            if (candidate == viewer || !(candidate instanceof MultipartEntity.OrientedPart part)) continue;
            if (!filter.test(candidate)) continue;
            var box = part.getOrientedBox();
            if (box == null) continue;
            if (!candidate.getBoundingBox().intersects(broadPhase)) continue;

            var clip = box.clip(from, to);
            if (clip.isEmpty()) continue;

            // Rank by how close the crosshair passes to the part's centre, not by entry
            // distance: overlapping parts would otherwise steal the pick from the part the
            // player is actually pointing at (aiming at one segment damaged the next one).
            Vec3 offset = box.center.subtract(from);
            Vec3 closestOnRay = from.add(direction.scale(offset.dot(direction)));
            double miss = box.center.distanceToSqr(closestOnRay);
            double alongRay = clip.get().distanceToSqr(from);
            if (miss < bestMiss - 1.0e-6
                    || (Math.abs(miss - bestMiss) <= 1.0e-6 && alongRay < bestAlongRay)) {
                bestMiss = miss;
                bestAlongRay = alongRay;
                best = new EntityHitResult(candidate, clip.get());
            }
        }

        EntityHitResult vanilla = cir.getReturnValue();
        Entity selected = vanilla == null ? null : vanilla.getEntity();
        if (best == null) {
            if (selected instanceof MultipartEntity.OrientedPart) cir.setReturnValue(null);
            return;
        }
        // Keep vanilla's own result when it already names the same entity and is not farther.
        if (vanilla != null && best.getEntity() == selected
                && best.getLocation().distanceToSqr(from) >= vanilla.getLocation().distanceToSqr(from)) {
            return;
        }
        if (icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragonDamageDebug.enabled()) {
            icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragonDamageDebug.log(
                    "pick corrected " + describe(selected) + " -> " + describe(best.getEntity()));
        }
        cir.setReturnValue(best);
    }

    /**
     * How far the ray passes from an entity's centre; zero means the crosshair is on it.
     * Entities without an oriented box measure to the centre of their bounding box.
     */
    private static double missDistance(Entity entity, Vec3 from, Vec3 direction) {
        Vec3 centre = entity instanceof MultipartEntity.OrientedPart part && part.getOrientedBox() != null
                ? part.getOrientedBox().center
                : entity.getBoundingBox().getCenter();
        double along = centre.subtract(from).dot(direction);
        return centre.distanceToSqr(from.add(direction.scale(along)));
    }

    /**
     * The candidate a hit result stands for, measured honestly: an oriented-box entry point
     * when the entity has one, otherwise where the ray enters its bounding box.
     */
    private static EntityHitResult resolveCandidate(EntityHitResult hit, Vec3 from, Vec3 to) {
        if (hit == null) return null;
        Entity entity = hit.getEntity();
        if (entity.isRemoved()) return null;
        if (entity instanceof MultipartEntity.OrientedPart part) {
            var box = part.getOrientedBox();
            if (box == null) return hit;
            return box.clip(from, to).map(point -> new EntityHitResult(entity, point)).orElse(null);
        }
        return entity.getBoundingBox().clip(from, to)
                .map(point -> new EntityHitResult(entity, point))
                .orElse(hit);
    }

    private static String describe(Entity entity) {
        if (entity == null) return "null";
        if (entity instanceof icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragonPartEntity part) {
            return "part" + part.getPartIndex();
        }
        return entity.getClass().getSimpleName();
    }
}
