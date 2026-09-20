package icu.icuqalt10.panlingre.mixin;

import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragonPartEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives multipart boss parts extra interaction range on the <b>server</b>, and only those parts.
 *
 * <p>Two independent reach gates exist in vanilla and they do not share an implementation:
 * <ul>
 *   <li>Server: {@code ServerGamePacketListenerImpl.handleInteract} calls
 *       {@code Player#canInteractWithEntity} and silently drops the whole attack packet when it
 *       fails. That method is the one hooked here.</li>
 *   <li>Client: {@code GameRenderer.pick} does <b>not</b> use it. It filters through
 *       {@code filterHitResult(hit, eye, entityInteractionRange)} against a hard 3 blocks, and
 *       {@code Minecraft.startAttack} turns a miss into an empty swing without sending a packet.
 *       The client side is widened separately by
 *       {@code client.PartPickReachMixin}.</li>
 * </ul>
 * Both are needed: without the client half a part beyond 3 blocks is never even reported, and
 * without the server half the packet is dropped once a moving player's position lags.
 *
 * <p>Scoping the margin to part entities keeps ordinary interactions (villagers, chests, other
 * mobs) exactly as vanilla defines them.
 */
@Mixin(Player.class)
public abstract class PartInteractionReachMixin {
    @Inject(method = "canInteractWithEntity(Lnet/minecraft/world/phys/AABB;D)Z",
            at = @At("HEAD"), cancellable = true)
    private void panlingre$extendPartReach(AABB box, double distance,
                                           CallbackInfoReturnable<Boolean> cir) {
        if (!GraveDragonPartEntity.isPartHitbox(box)) return;
        Player self = (Player) (Object) this;
        double limit = self.entityInteractionRange() + distance + GraveDragonPartEntity.REACH_MARGIN;
        if (box.distanceToSqr(self.getEyePosition()) < limit * limit) cir.setReturnValue(true);
    }

    @Inject(method = "canInteractWithEntity(Lnet/minecraft/world/entity/Entity;D)Z",
            at = @At("HEAD"), cancellable = true)
    private void panlingre$extendPartReachEntity(Entity entity, double distance,
                                                 CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof GraveDragonPartEntity)) return;
        Player self = (Player) (Object) this;
        double limit = self.entityInteractionRange() + distance + GraveDragonPartEntity.REACH_MARGIN;
        if (entity.getBoundingBox().distanceToSqr(self.getEyePosition()) < limit * limit) {
            cir.setReturnValue(true);
        }
    }
}
