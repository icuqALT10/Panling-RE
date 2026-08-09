package icu.icuqalt10.panlingre.mixin;

import icu.icuqalt10.panlingre.init.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Allows only PanlingRE's freeze effect through the dragon's blanket effect immunity. */
@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin {
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void panlingre$allowFreeze(
            MobEffectInstance instance,
            Entity source,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!instance.is(ModEffects.freeze)) {
            return;
        }

        EnderDragon dragon = (EnderDragon) (Object) this;
        dragon.forceAddEffect(instance, source);
        if (dragon.getEffect(ModEffects.freeze) == instance) {
            instance.onEffectStarted(dragon);
            cir.setReturnValue(true);
        }
    }
}
