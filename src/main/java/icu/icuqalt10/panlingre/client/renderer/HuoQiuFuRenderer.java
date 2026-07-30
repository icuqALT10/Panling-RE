package icu.icuqalt10.panlingre.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import icu.icuqalt10.panlingre.entity.HuoQiuFuEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.data.ModelData;

public class HuoQiuFuRenderer extends EntityRenderer<HuoQiuFuEntity> {
    private static final float BLOCK_SCALE = 1.5F;
    private static final float DIAMOND_ANGLE = 45.0F;
    private static final float ROTATION_SPEED = 6.0F;

    private final BlockRenderDispatcher blockRenderer;

    public HuoQiuFuRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.75F;
    }

    @Override
    public void render(
            HuoQiuFuEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        float spin = (entity.tickCount + partialTick) * ROTATION_SPEED;

        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(DIAMOND_ANGLE));
        poseStack.mulPose(Axis.XP.rotationDegrees(spin));
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.scale(BLOCK_SCALE, BLOCK_SCALE, BLOCK_SCALE);
        poseStack.translate(-0.5, -0.5, -0.5);

        this.blockRenderer.renderSingleBlock(
                Blocks.MAGMA_BLOCK.defaultBlockState(),
                poseStack,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY,
                null
        );
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(HuoQiuFuEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
