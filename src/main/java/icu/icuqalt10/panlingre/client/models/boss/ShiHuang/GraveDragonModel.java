package icu.icuqalt10.panlingre.client.models.boss.ShiHuang;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragonEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.animation.AnimationState;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragonIdleAirPose;

public class GraveDragonModel extends GeoModel<GraveDragonEntity> {
    @Override
    public void setCustomAnimations(GraveDragonEntity dragon, long instanceId, AnimationState<GraveDragonEntity> state) {
        // Apply the very same spline sample and world time as server OBBs. A controller's
        // first-render time is client-local and cannot serve as the collision clock.
        var frame = GraveDragonIdleAirPose.sample(dragon.idleAirSeconds(state.getPartialTick()));
        for (var entry : frame.bones().entrySet()) {
            getBone(entry.getKey()).ifPresent(bone -> {
                var pose = entry.getValue();
                bone.updateRotation((float)pose.rotation().x, (float)pose.rotation().y, (float)pose.rotation().z);
                bone.updatePosition((float)pose.position().x, (float)pose.position().y, (float)pose.position().z);
                bone.updateScale((float)pose.scale().x, (float)pose.scale().y, (float)pose.scale().z);
            });
        }
        // The visible model interpolates smoothly. Collision boxes deliberately do NOT follow
        // this frame: they come from the quantised pose both sides compute identically and are
        // refreshed by the entity tick. Overwriting them here with an interpolated render frame
        // made the client's crosshair disagree with the server's validation.
    }

    @Override
    public ResourceLocation getModelResource(GraveDragonEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "geo/entity/boss/shihuang/grave_dragon.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GraveDragonEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/entity/boss/shihuang/grave_dragon.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GraveDragonEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "animations/entity/boss/shihuang/grave_dragon.animation.json");
    }
}
