package icu.icuqalt10.panlingre.client.renderer;

import icu.icuqalt10.panlingre.client.models.FeiXianJianZhenModel;
import icu.icuqalt10.panlingre.entity.FeiXianJianZhenEntity;
import icu.icuqalt10.panlingre.init.ModItems;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class FeiXianJianZhenRenderer extends GeoEntityRenderer<FeiXianJianZhenEntity> {

    public FeiXianJianZhenRenderer(EntityRendererProvider.Context context) {
        super(context, new FeiXianJianZhenModel());

        this.addRenderLayer(new BlockAndItemGeoLayer<>(this) {
            @Override
            protected ItemStack getStackForBone(GeoBone bone, FeiXianJianZhenEntity animatable) {
                String boneName = bone.getName();
                if (boneName.equals("middle")) {
                    return ModItems.fei_xian_jian.get().getDefaultInstance();
                }
                return ItemStack.EMPTY;
            }
        });
    }

    @Override
    public boolean shouldRender(FeiXianJianZhenEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }
}