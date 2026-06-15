package icu.icuqalt10.panlingre.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import icu.icuqalt10.panlingre.block.ldl.ldlEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class ldlRenderer implements BlockEntityRenderer<ldlEntity> {
    private final BlockRenderDispatcher dispatcher;

    public ldlRenderer(BlockEntityRendererProvider.Context context) {
        this.dispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(ldlEntity entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        poseStack.scale(3.0f, 3.0f, 3.0f);
        poseStack.translate(-0.5, 0, -0.5);

        BlockState state = entity.getBlockState();
        this.dispatcher.renderSingleBlock(
                state,
                poseStack,
                buffer,
                combinedLight,
                combinedOverlay,
                ModelData.EMPTY,
                null
        );
        poseStack.popPose();
    }
}