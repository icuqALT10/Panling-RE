package icu.icuqalt10.panlingre.mixin.client;

import icu.icuqalt10.panlingre.entity.MultipartEntity;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonEntity;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonPartEntity;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the client's crosshair reach boss parts that the server already allows.
 *
 * <p>Vanilla validates reach on two independent paths. The server drops the attack packet when
 * {@code Player#canInteractWithEntity} fails, and that is the only place the part-scoped reach
 * margin applies. The client never calls it: {@code GameRenderer.pick} ends in
 * {@code filterHitResult(hit, eye, entityInteractionRange)}, a hard 3-block limit, and
 * {@code Minecraft.startAttack} then treats the resulting MISS as an empty swing — <b>no attack
 * packet is sent at all</b>.
 *
 * <p>So a part the server would happily accept stays unhittable whenever the player is between
 * that 3-block limit and the server's allowance, which is exactly the "I am touching the model
 * and nothing happens" case for a boss this large. This hook re-runs the pick with the same
 * margin the server uses, and only when vanilla found nothing: a block or entity closer than
 * the limit keeps priority, so occlusion is unaffected.
 *
 * @see GraveDragonPartEntity#REACH_MARGIN
 */
@Mixin(GameRenderer.class)
public abstract class PartPickReachMixin {
    @Inject(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At("HEAD"))
    private void panlingre$updateDisplayedPartPose(Entity camera, double blockRange, double entityRange,
                                                   float partialTick,
                                                   CallbackInfoReturnable<HitResult> cir) {
        for (GraveDragonEntity dragon : camera.level().getEntitiesOfClass(GraveDragonEntity.class,
                camera.getBoundingBox().inflate(128.0D))) {
            float frameTick = camera.level().tickRateManager().isEntityFrozen(dragon) ? 1.0F : partialTick;
            dragon.updateClientPartPose(frameTick);
        }
    }

    @Inject(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At("RETURN"), cancellable = true)
    private void panlingre$reachPartsWithMargin(Entity camera, double blockRange, double entityRange,
                                                float partialTick,
                                                CallbackInfoReturnable<HitResult> cir) {
        HitResult result = cir.getReturnValue();
        if (result != null && result.getType() == HitResult.Type.ENTITY) return;
        if (!(camera instanceof Player)) return;

        double margin = GraveDragonPartEntity.REACH_MARGIN;
        double limit = entityRange + margin;
        Vec3 eye = camera.getEyePosition(partialTick);
        Vec3 end = eye.add(camera.getViewVector(partialTick).scale(limit));

        HitResult block = camera.pick(limit, partialTick, false);
        double cutoff = block.getType() == HitResult.Type.BLOCK
                ? block.getLocation().distanceToSqr(eye) : limit * limit;
        Vec3 direction = end.subtract(eye).normalize();
        EntityHitResult best = null;
        double bestMiss = Double.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;

        for (MultipartEntity.OrientedPart part : MultipartEntity.orientedPartsNear(camera.level(), eye, limit)) {
            Entity entity = (Entity) part;
            if (entity.isRemoved() || !entity.isPickable() || entity.isSpectator()) continue;
            var clip = part.getOrientedBox().clip(eye, end);
            if (clip.isEmpty()) continue;
            double distance = clip.get().distanceToSqr(eye);
            if (distance >= cutoff) continue;
            Vec3 center = part.getOrientedBox().center;
            double miss = center.distanceToSqr(eye.add(direction.scale(center.subtract(eye).dot(direction))));
            if (miss < bestMiss - 1.0e-6
                    || (Math.abs(miss - bestMiss) <= 1.0e-6 && distance < bestDistance)) {
                best = new EntityHitResult(entity, clip.get());
                bestMiss = miss;
                bestDistance = distance;
            }
        }
        if (best != null) cir.setReturnValue(best);
    }
}
