package icu.icuqalt10.panlingre.client.models;

import icu.icuqalt10.panlingre.entity.FeiXianJianZhenEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class FeiXianJianZhenModel extends GeoModel<FeiXianJianZhenEntity> {
    @Override
    public ResourceLocation getModelResource(FeiXianJianZhenEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("panlingre", "geo/entity/fei_xian_jian_zhen.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FeiXianJianZhenEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("panlingre", "textures/entity/fei_xian_jian_zhen.png");
    }

    @Override
    public ResourceLocation getAnimationResource(FeiXianJianZhenEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("panlingre", "animations/entity/fei_xian_jian_zhen.animation.json");
    }
}