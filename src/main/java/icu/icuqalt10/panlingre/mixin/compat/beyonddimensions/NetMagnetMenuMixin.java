package icu.icuqalt10.panlingre.mixin.compat.beyonddimensions;

import icu.icuqalt10.panlingre.compat.beyonddimensions.BeyondDimensionsMagnetCompat;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.wintercogs.beyonddimensions.common.menu.NetMagnetMenu", remap = false)
public abstract class NetMagnetMenuMixin {
    @Inject(method = "writeQuickDataTag", at = @At("RETURN"), remap = false)
    private void panlingre$forceOutgoingMagnetModes(CompoundTag tag, CallbackInfo ci) {
        BeyondDimensionsMagnetCompat.forceModeTag(tag);
    }

    @Inject(method = "readQuickDataTag", at = @At("HEAD"), remap = false)
    private void panlingre$forceIncomingMagnetModes(CompoundTag tag, CallbackInfo ci) {
        BeyondDimensionsMagnetCompat.forceModeTag(tag);
    }
}
