package icu.icuqalt10.panlingre.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import icu.icuqalt10.panlingre.client.ClientSkillCastState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Raises the casting hand before vanilla first-person item transforms are applied. */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    private static final float CASTING_PITCH_DEGREES = 4.0F;
    private static final float CASTING_YAW_DEGREES = 3.0F;
    private static final float CASTING_ROLL_DEGREES = 48.0F;
    private static final float CASTING_CENTER_OFFSET = 0.20F;
    private static final float CASTING_DOWN_OFFSET = 0.50F;
    private static final float CASTING_DEPTH_OFFSET = 0.30F;
    private static final float RELEASE_SIDE_OFFSET = 0.35F;
    private static final float RELEASE_DOWN_OFFSET = 0.40F;

    @Inject(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
                    shift = At.Shift.AFTER
            )
    )
    private void panlingre$applyCastingPose(
            AbstractClientPlayer player, float partialTicks, float pitch,
            InteractionHand hand, float swingProgress, ItemStack stack,
            float equippedProgress, PoseStack poseStack,
            MultiBufferSource buffer, int combinedLight, CallbackInfo callbackInfo) {
        ClientSkillCastState.AnimationView animation =
                ClientSkillCastState.getAnimationView(player);
        if (animation == null || animation.hand() != hand) return;

        HumanoidArm arm = hand == InteractionHand.MAIN_HAND
                ? player.getMainArm()
                : player.getMainArm().getOpposite();
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float raise = animation.raiseProgress();
        float throwProgress = animation.throwProgress();

        poseStack.translate(
                -side * CASTING_CENTER_OFFSET * raise
                        + side * RELEASE_SIDE_OFFSET * throwProgress,
                -CASTING_DOWN_OFFSET * raise
                        - RELEASE_DOWN_OFFSET * throwProgress,
                -CASTING_DEPTH_OFFSET * raise);
        poseStack.mulPose(Axis.XP.rotationDegrees(
                CASTING_PITCH_DEGREES * raise - 6.0F * throwProgress));
        poseStack.mulPose(Axis.YP.rotationDegrees(
                side * (CASTING_YAW_DEGREES * raise - 2.0F * throwProgress)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(
                side * (CASTING_ROLL_DEGREES * raise - 30.0F * throwProgress)));
    }
}
