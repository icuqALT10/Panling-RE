package icu.icuqalt10.panlingre.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.client.layer.FireTornadoWindLayer;
import icu.icuqalt10.panlingre.client.models.FireTornadoModel;
import icu.icuqalt10.panlingre.entity.Ys3JinTornadoEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

public class Ys3JinTornadoRenderer extends EntityRenderer<Ys3JinTornadoEntity> {
    private static final ResourceLocation TORNADO_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            PanlingRE.MODID, "textures/entity/ys3_jin_entity.png");
    private final FireTornadoModel<Ys3JinTornadoEntity> tornadoModel;

    public Ys3JinTornadoRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.tornadoModel = new FireTornadoModel<>(
                context.bakeLayer(FireTornadoWindLayer.FIRE_TORNADO_LAYER));
    }

    @Override public void render(Ys3JinTornadoEntity entity, float yaw, float partialTick,
                                 PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        float flatten = smoothStep(entity.getFlattenProgress(partialTick));
        float expand = smoothStep(entity.getExpandProgress(partialTick));
        float spinAge = Math.max(0.0F, entity.tickCount + partialTick - 5.0F);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F * flatten));
        pose.mulPose(Axis.ZP.rotationDegrees(spinAge * 7.2F));
        float itemScale = 1.0F + 6.5F * expand;
        float thicknessScale = 1.0F + 3.0F * expand;
        pose.scale(itemScale, itemScale, thicknessScale);
        Minecraft.getInstance().getItemRenderer().renderStatic(entity.getItem(), ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());
        pose.popPose();

        if (entity.isTornadoVisible()) {
            renderTornado(entity, partialTick, smoothStep(entity.getExpandProgress(partialTick)), pose, buffers);
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    private void renderTornado(Ys3JinTornadoEntity entity, float partialTick, float expand,
                               PoseStack pose, MultiBufferSource buffers) {
        float age = entity.tickCount + partialTick;
        tornadoModel.setupAnim(entity, 0.0F, 0.0F, age, 0.0F, 0.0F);

        pose.pushPose();
        // Match LivingEntityRenderer's model-space transform, at half the fire tornado's scale.
        pose.scale(-3.0F * expand, -4.5F * expand, 3.0F * expand);
        pose.translate(0.0D, -1.501D, 0.0D);

        VertexConsumer base = buffers.getBuffer(RenderType.entityTranslucent(TORNADO_TEXTURE));
        tornadoModel.renderToBuffer(pose, base, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        float uScroll = age * 0.02F;
        float vScroll = age * 0.005F;
        VertexConsumer wind = buffers.getBuffer(RenderType.breezeWind(
                TORNADO_TEXTURE, uScroll % 1.0F, vScroll % 1.0F));
        tornadoModel.renderToBuffer(pose, wind, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        pose.popPose();
    }

    @Override public ResourceLocation getTextureLocation(Ys3JinTornadoEntity entity) {
        return TORNADO_TEXTURE;
    }

    @Override
    public boolean shouldRender(Ys3JinTornadoEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }
}
