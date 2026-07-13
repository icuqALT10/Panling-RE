package icu.icuqalt10.panlingre.client.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.client.models.FireTornadoModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class FireTornadoWindLayer<T extends Entity, M extends FireTornadoModel<T>> extends RenderLayer<T, M> {
    // 替换成你的贴图路径 (橙红色风暴贴图)
    private static final ResourceLocation WIND_TEXTURE = ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/entity/fire_tornado_wind.png");

    public static final ModelLayerLocation FIRE_TORNADO_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "fire_tornado"), "main"
    );

    public FireTornadoWindLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {

        // 控制贴图 UV 流动的速度
        float uScroll = ageInTicks * 0.02F;
        float vScroll = ageInTicks * 0.005F;

        // 获取原版半透明流动渲染类型
        RenderType renderType = RenderType.breezeWind(WIND_TEXTURE, uScroll % 1.0F, vScroll % 1.0F);
        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);

        // 设置全亮光源，让火焰发光
        int fullBrightLight = 15728880;

        // 渲染覆盖层
        this.getParentModel().renderToBuffer(poseStack, vertexConsumer, fullBrightLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    }
}