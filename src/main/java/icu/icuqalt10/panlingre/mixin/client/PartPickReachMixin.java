package icu.icuqalt10.panlingre.mixin.client;

import icu.icuqalt10.panlingre.entity.MultipartEntity;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragonDamageDebug;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragonPartEntity;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
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
            at = @At("RETURN"), cancellable = true)
    private void panlingre$reachPartsWithMargin(Entity camera, double blockRange, double entityRange,
                                                float partialTick,
                                                CallbackInfoReturnable<HitResult> cir) {
        HitResult result = cir.getReturnValue();
        if (result != null && result.getType() == HitResult.Type.ENTITY) return;
        if (!(camera instanceof Player player)) return;

        double margin = GraveDragonPartEntity.REACH_MARGIN;
        double limit = entityRange + margin;
        Vec3 eye = camera.getEyePosition(partialTick);
        Vec3 end = eye.add(camera.getViewVector(partialTick).scale(limit));

        // Only re-resolve when a block hit is far enough away to be irrelevant; anything
        // nearer keeps vanilla's answer so walls still block the crosshair.
        if (result != null && result.getType() == HitResult.Type.BLOCK
                && result.getLocation().distanceToSqr(eye) <= limit * limit) {
            return;
        }

        for (MultipartEntity.OrientedPart part : MultipartEntity.orientedPartsNear(camera.level(), eye, limit)) {
            var clip = part.getOrientedBox().clip(eye, end);
            if (clip.isEmpty()) continue;
            if (GraveDragonDamageDebug.enabled()) {
                GraveDragonDamageDebug.log("pick margin rescued a part at "
                        + clip.get().distanceTo(eye) + " (vanilla limit " + entityRange + ")");
            }
            cir.setReturnValue(new EntityHitResult((Entity) part, clip.get()));
            return;
        }
    }
}
