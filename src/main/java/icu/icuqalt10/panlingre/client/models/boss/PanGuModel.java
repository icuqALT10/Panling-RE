package icu.icuqalt10.panlingre.client.models.boss;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.entity.boss.PanGuEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class PanGuModel extends GeoModel<PanGuEntity> {
    @Override
    public ResourceLocation getModelResource(PanGuEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "geo/entity/boss/pangu.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PanGuEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/entity/boss/pangu_small.png");
    }

    @Override
    public ResourceLocation getAnimationResource(PanGuEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "animations/entity/boss/pangu.animation.json");
    }

    @Override
    public void setCustomAnimations(PanGuEntity animatable, long instanceId, AnimationState<PanGuEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        GeoBone hand = getAnimationProcessor().getBone("hand_right");
        if (hand != null) {
            hand.setHidden(true);
        }
        GeoBone jin = getAnimationProcessor().getBone("jin");
        if (jin != null) {
            jin.setHidden(true);
        }
        GeoBone mu = getAnimationProcessor().getBone("mu");
        if (mu != null) {
            mu.setHidden(true);
        }
        GeoBone shui = getAnimationProcessor().getBone("shui");
        if (shui != null) {
            shui.setHidden(true);
        }
        GeoBone huo = getAnimationProcessor().getBone("huo");
        if (huo != null) {
            huo.setHidden(true);
        }
        GeoBone tu = getAnimationProcessor().getBone("tu");
        if (tu != null) {
            tu.setHidden(true);
        }
    }
}