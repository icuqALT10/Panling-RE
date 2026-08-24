package icu.icuqalt10.panlingre.client.models.boss.ShiHuang;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragonEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GraveDragonModel extends GeoModel<GraveDragonEntity> {
    @Override
    public ResourceLocation getModelResource(GraveDragonEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "geo/entity/boss/shihuang/dragon.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GraveDragonEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/entity/boss/shihuang/mu_dragon.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GraveDragonEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "animations/entity/boss/shihuang/dragon.animation.json");
    }
}