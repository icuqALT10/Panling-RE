package icu.icuqalt10.panlingre.mixin.client;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererActivationMixin {
    private static final int FAST_ACTIVATION_LENGTH = 13;

    @Shadow
    private int itemActivationTicks;

    @Inject(method = "displayItemActivation", at = @At("TAIL"))
    private void panlingre$setFastActivationLength(ItemStack stack, CallbackInfo callbackInfo) {
        this.itemActivationTicks = FAST_ACTIVATION_LENGTH;
    }

    @ModifyConstant(
            method = "renderItemActivationAnimation",
            constant = @org.spongepowered.asm.mixin.injection.Constant(intValue = 40)
    )
    private int panlingre$fastActivationIntLength(int original) {
        return FAST_ACTIVATION_LENGTH;
    }

    @ModifyConstant(
            method = "renderItemActivationAnimation",
            constant = @org.spongepowered.asm.mixin.injection.Constant(floatValue = 40.0F)
    )
    private float panlingre$fastActivationFloatLength(float original) {
        return FAST_ACTIVATION_LENGTH;
    }
}
