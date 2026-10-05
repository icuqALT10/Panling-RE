package icu.icuqalt10.panlingre.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonRockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.data.ModelData;

public final class GraveDragonRockRenderer extends EntityRenderer<GraveDragonRockEntity> {
    private final BlockRenderDispatcher blocks;

    public GraveDragonRockRenderer(EntityRendererProvider.Context context) {
        super(context);
        blocks = context.getBlockRenderDispatcher();
        shadowRadius = 2F;
    }

    @Override public void render(GraveDragonRockEntity rock, float yaw, float partialTick,
                                 PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0, 2, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        float distance = rock.travel() + partialTick * (float) rock.getDeltaMovement().length();
        pose.mulPose(Axis.XP.rotationDegrees(distance * 360 / (4 * (float) Math.PI)));
        pose.scale(4, 4, 4);
        pose.translate(-.5, -.5, -.5);
        blocks.renderSingleBlock(Blocks.DIRT.defaultBlockState(), pose, buffers,
                light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
        pose.popPose();
        super.render(rock, yaw, partialTick, pose, buffers, light);
    }

    @Override public ResourceLocation getTextureLocation(GraveDragonRockEntity rock) { return TextureAtlas.LOCATION_BLOCKS; }
}
