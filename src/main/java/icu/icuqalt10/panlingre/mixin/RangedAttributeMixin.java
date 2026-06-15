package icu.icuqalt10.panlingre.mixin;


import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RangedAttribute.class)
public abstract class RangedAttributeMixin {
    @Shadow @Final @Mutable
    private double maxValue;
    @Shadow @Final @Mutable
    private double minValue;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(String descriptionId, double defaultValue, double min, double max, CallbackInfo ci) {
        if (descriptionId.equals("attribute.name.generic.armor")) {
            this.maxValue = 1000000.0D;
            this.minValue = -1000000.0D;
        }

        if (descriptionId.equals("attribute.name.generic.max_health")) {
            this.maxValue = 1000000.0D;
        }
    }
}