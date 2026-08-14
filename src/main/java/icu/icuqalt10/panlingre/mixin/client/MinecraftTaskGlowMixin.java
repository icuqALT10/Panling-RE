package icu.icuqalt10.panlingre.mixin.client;

import icu.icuqalt10.panlingre.client.task.ClientTaskGuideState;
import icu.icuqalt10.panlingre.client.TianXingSniperState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftTaskGlowMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void panlingre$taskGuideGlow(Entity entity, CallbackInfoReturnable<Boolean> callback) {
        if (ClientTaskGuideState.shouldGlow(entity) || TianXingSniperState.shouldGlow(entity)) {
            callback.setReturnValue(true);
        }
    }
}
