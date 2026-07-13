package icu.icuqalt10.panlingre.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.client.layer.FireTornadoWindLayer;
import icu.icuqalt10.panlingre.client.models.FireTornadoModel;
import icu.icuqalt10.panlingre.entity.FireTornadoEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class FireTornadoRenderer extends MobRenderer<FireTornadoEntity, FireTornadoModel<FireTornadoEntity>> {

    private static final ResourceLocation BASE_TEXTURE = ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/entity/fire_tornado_wind.png");

    public FireTornadoRenderer(EntityRendererProvider.Context context) {
        super(context, new FireTornadoModel<>(context.bakeLayer(FireTornadoWindLayer.FIRE_TORNADO_LAYER)), 1.5F);

        this.addLayer(new FireTornadoWindLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(FireTornadoEntity entity) {
        return BASE_TEXTURE;
    }

    @Override
    protected void scale(FireTornadoEntity entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(3.0F, 4.5F, 3.0F);
    }
}