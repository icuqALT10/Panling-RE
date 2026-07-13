package icu.icuqalt10.panlingre.client.renderer.boss;

import icu.icuqalt10.panlingre.client.models.boss.PanGuModel;
import icu.icuqalt10.panlingre.entity.boss.PanGuEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class PanGuRenderer extends GeoEntityRenderer<PanGuEntity> {

    public PanGuRenderer(EntityRendererProvider.Context context) {
        super(context, new PanGuModel());
        this.shadowRadius = 0.5F;

        this.addRenderLayer(new BlockAndItemGeoLayer<>(this) {
            @Override
            protected ItemStack getStackForBone(GeoBone bone, PanGuEntity animatable) {
                if (bone.getName().equals("hand_right") || bone.getName().equals("axe")) {
                    return animatable.getItemBySlot(EquipmentSlot.MAINHAND);
                }
                return ItemStack.EMPTY;
            }
        });
    }

    @Override
    public boolean shouldRender(PanGuEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }
}