package icu.icuqalt10.panlingre.mixin.client;

import icu.icuqalt10.panlingre.client.task.ClientTaskGuideState;
import icu.icuqalt10.panlingre.client.TianXingSniperState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityTaskGlowColorMixin {
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void panlingre$taskGuideGlowColor(CallbackInfoReturnable<Integer> callback) {
        Entity entity = (Entity)(Object)this;
        if (TianXingSniperState.shouldGlow(entity)) {
            callback.setReturnValue(TianXingSniperState.outlineColor());
        } else if (ClientTaskGuideState.shouldGlow(entity)) {
            callback.setReturnValue(ClientTaskGuideState.outlineColor());
        }
    }
}
