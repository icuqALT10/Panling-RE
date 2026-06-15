package icu.icuqalt10.panlingre.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

// 这是一个客户端唯一的类
public class ldlCurioRenderer implements ICurioRenderer {

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack,
            SlotContext slotContext,
            PoseStack poseStack,
            RenderLayerParent<T, M> renderLayerParent,
            MultiBufferSource renderTypeBuffer,
            int light,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {

        M parentModel = renderLayerParent.getModel();

        if (parentModel instanceof HumanoidModel<?> humanoidModel) {
            ICurioRenderer.followBodyRotations(slotContext.entity(), (HumanoidModel<LivingEntity>) humanoidModel);
        }

        poseStack.pushPose();

        poseStack.mulPose(Axis.ZP.rotationDegrees(180f));
        poseStack.translate(-0.95, 0.75, 0.75);
        poseStack.scale(1.25f, 1.25f, 1.25f);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                light,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                renderTypeBuffer,
                slotContext.entity().level(),
                0
        );

        poseStack.popPose();
    }
}