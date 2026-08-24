package icu.icuqalt10.panlingre.client.renderer.boss.ShiHuang;

import icu.icuqalt10.panlingre.client.models.boss.ShiHuang.GraveDragonModel;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragonEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GraveDragonRenderer extends GeoEntityRenderer<GraveDragonEntity> {

    public GraveDragonRenderer(EntityRendererProvider.Context context) {
        super(context, new GraveDragonModel());
        this.shadowRadius = 0F;
    }

    @Override
    public boolean shouldRender(GraveDragonEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }
}