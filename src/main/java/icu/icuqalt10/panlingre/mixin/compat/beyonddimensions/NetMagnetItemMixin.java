package icu.icuqalt10.panlingre.mixin.compat.beyonddimensions;

import icu.icuqalt10.panlingre.compat.beyonddimensions.BeyondDimensionsMagnetCompat;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.wintercogs.beyonddimensions.common.item.NetMagnetItem", remap = false)
public abstract class NetMagnetItemMixin {
    @Inject(method = "checkComponents", at = @At("RETURN"), remap = false)
    private void panlingre$forceMagnetModes(ItemStack stack, CallbackInfo ci) {
        BeyondDimensionsMagnetCompat.forceModes(stack);
    }
}
