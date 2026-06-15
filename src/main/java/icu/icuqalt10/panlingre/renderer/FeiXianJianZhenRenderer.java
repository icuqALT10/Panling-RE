package icu.icuqalt10.panlingre.renderer;

import icu.icuqalt10.panlingre.client.models.FeiXianJianZhenModel;
import icu.icuqalt10.panlingre.entity.FeiXianJianZhenEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class FeiXianJianZhenRenderer extends GeoEntityRenderer<FeiXianJianZhenEntity> {
    public FeiXianJianZhenRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new FeiXianJianZhenModel());
    }

    @Override
    public boolean shouldRender(FeiXianJianZhenEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }
}