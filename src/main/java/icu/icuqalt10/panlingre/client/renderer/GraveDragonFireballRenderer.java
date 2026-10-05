package icu.icuqalt10.panlingre.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonFireballEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.data.ModelData;

public final class GraveDragonFireballRenderer extends EntityRenderer<GraveDragonFireballEntity> {
    private static final ResourceLocation BLUE = ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID,
            "textures/entity/grave_dragon_blue_magma.png");
    private final BlockRenderDispatcher blocks;

    public GraveDragonFireballRenderer(EntityRendererProvider.Context context) {
        super(context);
        blocks = context.getBlockRenderDispatcher();
        shadowRadius = 3F;
    }

    @Override public void render(GraveDragonFireballEntity ball, float yaw, float partialTick,
                                 PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        float spin = (ball.tickCount + partialTick) * 6;
        pose.mulPose(Axis.XP.rotationDegrees(spin));
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        pose.scale(6, 6, 6);
        if (ball.blue()) {
            VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(BLUE));
            face(vertices, pose.last(), light, -.5F, -.5F,  .5F,  1, 0, 0,  0, 1, 0,  0, 0, 1);
            face(vertices, pose.last(), light,  .5F, -.5F, -.5F, -1, 0, 0,  0, 1, 0,  0, 0,-1);
            face(vertices, pose.last(), light,  .5F, -.5F,  .5F,  0, 0,-1,  0, 1, 0,  1, 0, 0);
            face(vertices, pose.last(), light, -.5F, -.5F, -.5F,  0, 0, 1,  0, 1, 0, -1, 0, 0);
            face(vertices, pose.last(), light, -.5F,  .5F,  .5F,  1, 0, 0,  0, 0,-1,  0, 1, 0);
            face(vertices, pose.last(), light, -.5F, -.5F, -.5F,  1, 0, 0,  0, 0, 1,  0,-1, 0);
        } else {
            pose.translate(-.5, -.5, -.5);
            blocks.renderSingleBlock(Blocks.MAGMA_BLOCK.defaultBlockState(), pose, buffers,
                    light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
        }
        pose.popPose();
        super.render(ball, yaw, partialTick, pose, buffers, light);
    }

    private static void face(VertexConsumer vertices, PoseStack.Pose pose, int light,
                             float x, float y, float z, float ux, float uy, float uz,
                             float vx, float vy, float vz, float nx, float ny, float nz) {
        vertex(vertices, pose, light, x, y, z, 0, 0, nx, ny, nz);
        vertex(vertices, pose, light, x + ux, y + uy, z + uz, 1, 0, nx, ny, nz);
        vertex(vertices, pose, light, x + ux + vx, y + uy + vy, z + uz + vz, 1, 1, nx, ny, nz);
        vertex(vertices, pose, light, x + vx, y + vy, z + vz, 0, 1, nx, ny, nz);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, int light,
                               float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        vertices.addVertex(pose, x, y, z).setColor(255, 255, 255, 255).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
    }

    @Override public ResourceLocation getTextureLocation(GraveDragonFireballEntity ball) { return BLUE; }
}
