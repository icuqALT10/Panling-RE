package icu.icuqalt10.panlingre.mixin;

import icu.icuqalt10.panlingre.worldtemplate.WorldTemplateUpdater;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;

@Mixin(LevelStorageSource.class)
public abstract class LevelStorageSourceMixin {
    @Inject(method = "validateAndCreateAccess", at = @At("RETURN"))
    private void panlingre$updateWorldBeforeLoad(String saveName,
                                                 CallbackInfoReturnable<LevelStorageSource.LevelStorageAccess> callback)
            throws IOException {
        LevelStorageSource.LevelStorageAccess access = callback.getReturnValue();
        if (access == null) {
            return;
        }
        try {
            WorldTemplateUpdater.updateIfNeeded(access.getLevelDirectory().path());
        } catch (IOException | RuntimeException exception) {
            access.safeClose();
            throw exception;
        }
    }
}
