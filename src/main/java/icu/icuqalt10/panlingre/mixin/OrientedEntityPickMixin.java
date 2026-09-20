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
 * Multipart hit selection by true oriented boxes.
 *
 * <p>Minecraft can only compare candidates through {@link AABB}, so every multipart
 * part has to expose a conservative enclosing box to the engine. Around a rotated
 * oriented box that envelope contains a lot of empty space, especially in its
 * corners. The vanilla loop reports a hit wherever the ray merely crossed such an
 * empty corner and keeps the nearest envelope distance, so a large part (torso,
 * limb, horn) steals the selection from the small part actually under the
 * crosshair. The swing then damages something else, or nothing visible at all.
 *
 * <p>This hook re-runs the comparison with each candidate's real oriented box, so
 * the part the ray truly reaches first wins. The returned hit location always lies
 * on that oriented box and therefore inside the part's own envelope, which keeps
 * the caller's reach filter measuring the surface the player aimed at.
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

        EntityHitResult selected = cir.getReturnValue();
        Entity selectedEntity = selected == null ? null : selected.getEntity();
        // The camera already sits inside a real oriented box: that is a zero-distance
        // hit, and nothing can legitimately be nearer than zero.
        if (selectedEntity instanceof MultipartEntity.OrientedPart picked) {
            var pickedBox = picked.getOrientedBox();
            if (pickedBox != null && pickedBox.contains(from)) {
                cir.setReturnValue(new EntityHitResult(selectedEntity, from));
                return;
            }
        }

        // Broad phase only: a part's envelope merely has to be reachable by the ray.
        // The precise comparison below always uses the oriented box.
        AABB broadPhase = search.inflate(2.0);

        // Whichever part the ray truly reaches first, ignoring empty envelope corners.
        Entity partEntity = null;
        Vec3 partLocation = null;
        double partDistance = Double.MAX_VALUE;
        for (Entity candidate : level.getPartEntities()) {
            if (candidate == viewer || !(candidate instanceof MultipartEntity.OrientedPart part)) continue;
            var box = part.getOrientedBox();
            if (box == null || !filter.test(candidate)) continue;
            if (!candidate.getBoundingBox().intersects(broadPhase)) continue;
            if (box.contains(from)) {
                // The camera is inside this part's real box.
                cir.setReturnValue(new EntityHitResult(candidate, from));
                return;
            }
            var clip = box.clip(from, to);
            if (clip.isEmpty()) continue;
            double distance = clip.get().distanceToSqr(from);
            if (distance < partDistance) {
                partDistance = distance;
                partEntity = candidate;
                partLocation = clip.get();
            }
        }

        if (partEntity == null) return;
        if (selected == null) {
            cir.setReturnValue(new EntityHitResult(partEntity, partLocation));
            return;
        }
        if (partEntity == selectedEntity && partDistance >= selected.getLocation().distanceToSqr(from)) return;
        // Another entity was picked through its envelope. Replace it only when the real
        // oriented box is genuinely nearer, so ordinary entities keep vanilla ordering.
        if (partEntity != selectedEntity && !partLocation.closerThan(from, selected.getLocation().distanceToSqr(from))) return;
        cir.setReturnValue(new EntityHitResult(partEntity, partLocation));
    }
}
