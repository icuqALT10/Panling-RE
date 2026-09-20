package icu.icuqalt10.panlingre.mixin;

import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragonDamageDebug;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Opt-in tracing of the vanilla server interaction guard, enabled with
 * {@code -Dpanlingre.damageDebug=true}.
 *
 * <p>When a melee click produces no damage, the attack packet can be discarded before it
 * ever reaches a part entity. This reports what the packet resolved to, and whether the
 * server's own position and reach agree that the target is close enough, which
 * distinguishes "the client named an entity the server cannot resolve" from "the server's
 * idea of the player's position disagrees with the client's".
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerInteractDebugMixin {
    @Shadow public ServerPlayer player;

    @Redirect(
            method = "handleInteract",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/game/ServerboundInteractPacket;getTarget(Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/entity/Entity;"
            )
    )
    private Entity panlingre$traceResolvedTarget(ServerboundInteractPacket packet, ServerLevel level) {
        Entity entity = packet.getTarget(level);
        if (!GraveDragonDamageDebug.enabled()) return entity;
        if (entity == null) {
            GraveDragonDamageDebug.log("packet target=null (server cannot resolve the id)");
            return null;
        }
        AABB box = entity.getBoundingBox();
        double reach = this.player.entityInteractionRange() + 1.0;
        double distance = Math.sqrt(box.distanceToSqr(this.player.getEyePosition()));
        GraveDragonDamageDebug.log("packet target=" + entity.getClass().getSimpleName()
                + " id=" + entity.getId()
                + " removed=" + entity.isRemoved()
                + " reach=" + String.format("%.3f", reach)
                + " distance=" + String.format("%.3f", distance)
                + " acceptedByVanilla=" + this.player.canInteractWithEntity(box, 1.0)
                + " gameMode=" + this.player.gameMode.getGameModeForPlayer()
                + " atkDamage=" + this.player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                + " charge=" + String.format("%.3f", this.player.getAttackStrengthScale(0.5F))
                + " mainHand=" + this.player.getMainHandItem().getItem()
                + " serverEye=" + this.player.getEyePosition()
                + " box=" + box);
        return entity;
    }
}
