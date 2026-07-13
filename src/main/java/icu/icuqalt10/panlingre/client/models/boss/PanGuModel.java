package icu.icuqalt10.panlingre.client.models.boss;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.entity.boss.PanGuEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class PanGuModel extends GeoModel<PanGuEntity> {
    @Override
    public ResourceLocation getModelResource(PanGuEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "geo/entity/boss/pangu.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PanGuEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "textures/entity/boss/pangu.png");
    }

    @Override
    public ResourceLocation getAnimationResource(PanGuEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "animations/entity/boss/pangu.animation.json");
    }

    @Override
    public void setCustomAnimations(PanGuEntity animatable, long instanceId, AnimationState<PanGuEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

            getAnimationProcessor().getBone("hand_right").setHidden(true);
            getAnimationProcessor().getBone("axe").setHidden(true);

        GeoBone head = getAnimationProcessor().getBone("head");

        if (head != null) {
            Player nearest = animatable.level().getNearestPlayer(
                    animatable.getX(), animatable.getY(), animatable.getZ(),
                    80.0D, false
            );

            boolean isPlayingWalkAnim = false;
            if (nearest != null) {
                isPlayingWalkAnim = (animatable.getActionState() == PanGuEntity.ActionState.ATTACK_COOLDOWN ||
                        animatable.getActionState() == PanGuEntity.ActionState.IDLE_OR_WALK) && animatable.distanceToSqr(nearest) <= 400.0D;
            }

            if (isPlayingWalkAnim) {
                EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
                head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
                head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
            }
        }
    }
}