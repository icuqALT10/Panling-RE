package icu.icuqalt10.panlingre.client.renderer.boss.ShiHuang;

import icu.icuqalt10.panlingre.client.models.boss.ShiHuang.GraveDragonModel;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GraveDragonRenderer extends GeoEntityRenderer<GraveDragonEntity> {

    public GraveDragonRenderer(EntityRendererProvider.Context context) {
        super(context, new GraveDragonModel());
        this.shadowRadius = 0F;
    }

    /**
     * GeckoLib 只给活着的实体应用偏航（{@code GeoEntityRenderer#applyRotations} 里就一句
     * {@code rotateY(180 - yBodyRot)}，俯仰那几处都在死亡/睡觉/倒挂分支里），所以"斜着飞"要靠
     * 这里补一步。
     *
     * <p>加了之后**必须**同步 {@code GraveDragonPose.modelToEntity}——那是这次渲染变换的镜像，
     * 碰撞箱走的正是它。两边的旋转顺序与符号一致，龙首一低头碰撞箱才会跟着斜过去；
     * 不一致就会退回"看到的和打得到的对不上"。
     */
    @Override
    protected void applyRotations(GraveDragonEntity animatable, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        poseStack.mulPose(Axis.XP.rotationDegrees(animatable.bodyPitch(partialTick)));
    }

    @Override
    public boolean shouldRender(GraveDragonEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }
}