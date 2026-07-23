package icu.icuqalt10.panlingre.mixin.client;

import icu.icuqalt10.panlingre.client.task.ClientTaskGuideState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityTaskGlowColorMixin {
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void panlingre$taskGuideGlowColor(CallbackInfoReturnable<Integer> callback) {
        if (ClientTaskGuideState.shouldGlow((Entity)(Object)this)) {
            callback.setReturnValue(ClientTaskGuideState.outlineColor());
        }
    }
}
