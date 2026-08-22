package icu.icuqalt10.panlingre.mixin.client;

import icu.icuqalt10.panlingre.client.ClientSkillCastState;
import icu.icuqalt10.panlingre.client.SkillCastClientEvents;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the synchronized raise, forward throw, and return to third-person player arms. */
@Mixin(PlayerModel.class)
public abstract class PlayerModelCastingMixin<T extends LivingEntity> extends HumanoidModel<T> {
    @Shadow @Final public ModelPart leftSleeve;
    @Shadow @Final public ModelPart rightSleeve;

    protected PlayerModelCastingMixin(ModelPart root) {
        super(root);
    }

    @Inject(
            method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
            at = @At("TAIL")
    )
    private void panlingre$applyCastingAnimation(
            T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch, CallbackInfo callbackInfo) {
        if (!(entity instanceof Player player)
                || !SkillCastClientEvents.isRenderingThirdPerson(player)) {
            return;
        }

        ClientSkillCastState.AnimationView animation =
                ClientSkillCastState.getAnimationView(player);
        if (animation == null) return;

        HumanoidArm castingArm = animation.hand() == InteractionHand.MAIN_HAND
                ? player.getMainArm()
                : player.getMainArm().getOpposite();
        boolean right = castingArm == HumanoidArm.RIGHT;
        ModelPart arm = right ? this.rightArm : this.leftArm;
        ModelPart sleeve = right ? this.rightSleeve : this.leftSleeve;

        float raise = animation.raiseProgress();
        float throwProgress = animation.throwProgress();
        float raisedAngle = Mth.lerp(throwProgress, 1.92F, 1.30F);
        float inwardAngle = Mth.lerp(throwProgress, 0.20F, 0.05F);
        float targetX = this.head.xRot - raisedAngle;
        float targetY = this.head.yRot + (right ? -inwardAngle : inwardAngle);
        float targetZ = (right ? -0.08F : 0.08F) * (1.0F - throwProgress);

        arm.xRot = Mth.lerp(raise, arm.xRot, targetX);
        arm.yRot = Mth.lerp(raise, arm.yRot, targetY);
        arm.zRot = Mth.lerp(raise, arm.zRot, targetZ);
        sleeve.copyFrom(arm);
    }
}
