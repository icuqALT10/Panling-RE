package icu.icuqalt10.panlingre.client.renderer;

import icu.icuqalt10.panlingre.entity.TombTrapArrowEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class TombTrapArrowRenderer extends ArrowRenderer<TombTrapArrowEntity> {
    public TombTrapArrowRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(TombTrapArrowEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");
    }
}
