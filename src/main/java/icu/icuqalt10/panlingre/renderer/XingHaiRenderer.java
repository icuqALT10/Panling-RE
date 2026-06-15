package icu.icuqalt10.panlingre.renderer;

import icu.icuqalt10.panlingre.client.models.XingHaiModel;
import icu.icuqalt10.panlingre.entity.XingHaiEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class XingHaiRenderer extends GeoEntityRenderer<XingHaiEntity> {
    public XingHaiRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new XingHaiModel());
    }

    @Override
    public boolean shouldRender(XingHaiEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }
}