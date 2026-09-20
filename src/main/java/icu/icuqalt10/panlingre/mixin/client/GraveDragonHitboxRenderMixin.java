package icu.icuqalt10.panlingre.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragonEntity;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class GraveDragonHitboxRenderMixin {
    @Inject(method = "renderHitbox", at = @At("HEAD"), cancellable = true)
    private static void panlingre$hideDragonEnvelopes(PoseStack pose, VertexConsumer buffer, Entity entity,
                                                     float partialTick, float r, float g, float b, CallbackInfo ci) {
        // ClientModEvents renders actual oriented boxes in world space.
        if (entity instanceof GraveDragonEntity) ci.cancel();
    }
}
