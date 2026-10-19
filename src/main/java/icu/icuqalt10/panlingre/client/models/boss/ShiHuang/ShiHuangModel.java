package icu.icuqalt10.panlingre.client.models.boss.ShiHuang;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.animation.AnimationState;

public class ShiHuangModel extends GeoModel<ShiHuangEntity> {
    @Override
    public void setCustomAnimations(ShiHuangEntity emperor, long instanceId, AnimationState<ShiHuangEntity> state) {
        var frame = emperor.pose(state.getPartialTick());
        applyPose(frame);
        emperor.updateParts(frame, net.minecraft.util.Mth.rotLerp(state.getPartialTick(),emperor.yBodyRotO,emperor.yBodyRot),
                emperor.visualPosition(state.getPartialTick()));
    }

    public void applyPose(icu.icuqalt10.panlingre.entity.boss.ShiHuang.ShiHuangPose.Frame frame) {
        for (var entry : frame.bones().entrySet()) {
            getBone(entry.getKey()).ifPresent(bone -> {
                var pose = entry.getValue();
                bone.updateRotation((float)pose.rotation().x, (float)pose.rotation().y, (float)pose.rotation().z);
                bone.updatePosition((float)pose.position().x, (float)pose.position().y, (float)pose.position().z);
                bone.updateScale((float)pose.scale().x, (float)pose.scale().y, (float)pose.scale().z);
                if(entry.getKey().startsWith("card_"))
                    bone.setHidden(pose.scale().x<=.001||pose.scale().y<=.001||pose.scale().z<=.001);
            });
        }
    }

    @Override
    public ResourceLocation getModelResource(ShiHuangEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "geo/entity/boss/shihuang/shihuang_spirit.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ShiHuangEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/entity/boss/shihuang/shihuang_spirit.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ShiHuangEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "animations/entity/boss/shihuang/shihuang_spirit.animation.json");
    }
}
