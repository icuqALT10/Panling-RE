package icu.icuqalt10.panlingre.mixin;

import icu.icuqalt10.panlingre.util.LocalWeatherManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Biome.class)
public class BiomeMixin {
    @Inject(method = "getPrecipitationAt", at = @At("HEAD"), cancellable = true)
    private void forceSnowPrecipitation(BlockPos pos, CallbackInfoReturnable<Biome.Precipitation> cir) {
        // 如果客户端处于假雪模式，强制返回 SNOW
        if (LocalWeatherManager.ClientWeatherState.isFakeSnowing) {
            cir.setReturnValue(Biome.Precipitation.SNOW);
        }
    }
}