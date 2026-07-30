package icu.icuqalt10.panlingre.mixin;

import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Creeper.class)
public class CreeperSkullDropMixin {

    @Inject(method = "canDropMobsSkull", at = @At("HEAD"), cancellable = true)
    private void panlingre$disableChargedCreeperSkullDrops(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
