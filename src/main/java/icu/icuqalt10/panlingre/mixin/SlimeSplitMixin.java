package icu.icuqalt10.panlingre.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Slime.class)
public class SlimeSplitMixin {

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void preventSlimeSplit(Entity.RemovalReason reason, CallbackInfo ci) {
        if (reason == Entity.RemovalReason.KILLED) {
            Slime slime = (Slime) (Object) this;

            slime.setRemoved(reason);
            ci.cancel();
        }
    }
}