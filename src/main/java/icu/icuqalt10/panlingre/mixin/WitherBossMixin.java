package icu.icuqalt10.panlingre.mixin;

import icu.icuqalt10.panlingre.init.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Allows only PanlingRE's freeze effect through the wither's blanket effect immunity. */
@Mixin(WitherBoss.class)
public abstract class WitherBossMixin {
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void panlingre$allowFreeze(
            MobEffectInstance instance,
            Entity source,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!instance.is(ModEffects.freeze)) {
            return;
        }

        WitherBoss wither = (WitherBoss) (Object) this;
        wither.forceAddEffect(instance, source);
        if (wither.getEffect(ModEffects.freeze) == instance) {
            instance.onEffectStarted(wither);
            cir.setReturnValue(true);
        }
    }
}
