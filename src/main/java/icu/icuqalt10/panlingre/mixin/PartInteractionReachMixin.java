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
 * Gives multipart boss parts a little extra interaction range, and only those parts.
 *
 * <p>Vanilla validates reach twice for a left click: the client filters its pick through
 * {@code entityInteractionRange}, and the server drops the whole attack packet when
 * ServerGamePacketListenerImpl disagrees. Both call this method. The threshold sits exactly
 * where a player fighting a large animated boss operates, so the latency between the
 * client's click and the server handling it is enough to push a legitimate click just past
 * the limit and discard it silently — measurements showed rejected attacks landing between
 * 0.17 and 1.6 blocks beyond the limit.
 *
 * <p>Scoping the margin to part entities keeps ordinary interactions (villagers, chests,
 * other mobs) exactly as vanilla defines them.
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
