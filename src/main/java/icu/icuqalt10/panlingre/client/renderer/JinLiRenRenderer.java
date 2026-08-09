package icu.icuqalt10.panlingre.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.entity.JinLiRenEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class JinLiRenRenderer extends EntityRenderer<JinLiRenEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            PanlingRE.MODID, "textures/entity/jin_liren.png");
    private static final float HALF_SIZE = 0.45F;

    public JinLiRenRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(JinLiRenEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(
                Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(
                Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        // In the source sprite the top-right corner is the blade point.
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));

        VertexConsumer vertices = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        vertex(vertices, pose, -HALF_SIZE, -HALF_SIZE, 0.0F, 0.0F, 1.0F, packedLight);
        vertex(vertices, pose,  HALF_SIZE, -HALF_SIZE, 0.0F, 1.0F, 1.0F, packedLight);
        vertex(vertices, pose,  HALF_SIZE,  HALF_SIZE, 0.0F, 1.0F, 0.0F, packedLight);
        vertex(vertices, pose, -HALF_SIZE,  HALF_SIZE, 0.0F, 0.0F, 0.0F, packedLight);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose,
                               float x, float y, float z, float u, float v, int light) {
        vertices.addVertex(pose, x, y, z)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(JinLiRenEntity entity) {
        return TEXTURE;
    }
}
