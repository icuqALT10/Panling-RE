package icu.icuqalt10.panlingre.client.models;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.entity.XingHaiEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class XingHaiModel extends GeoModel<XingHaiEntity> {
    @Override
    public ResourceLocation getModelResource(XingHaiEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "geo/entity/xing_hai.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(XingHaiEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/entity/xing_hai.png");
    }

    @Override
    public ResourceLocation getAnimationResource(XingHaiEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "animations/entity/xing_hai.animation.json");
    }
}