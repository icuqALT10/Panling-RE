package icu.icuqalt10.panlingre.mixin;

import icu.icuqalt10.panlingre.entity.interaction.CanInteractData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin({Interaction.class, Villager.class})
public class CanInteractEntityMixin implements CanInteractData {
    @Unique
    @Nullable
    private CompoundTag panlingre$canInteract;

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void panlingre$readCanInteract(CompoundTag tag, CallbackInfo ci) {
        this.panlingre$canInteract = tag.contains("can_interact", Tag.TAG_COMPOUND)
                ? tag.getCompound("can_interact").copy()
                : null;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void panlingre$saveCanInteract(CompoundTag tag, CallbackInfo ci) {
        if (this.panlingre$canInteract != null) {
            tag.put("can_interact", this.panlingre$canInteract.copy());
        }
    }

    @Override
    @Nullable
    public CompoundTag panlingre$getCanInteract() {
        return this.panlingre$canInteract;
    }
}
