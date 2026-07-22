package icu.icuqalt10.panlingre.entity.interaction;

import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

/**
 * Implemented by vanilla entities that support the custom can_interact NBT tag.
 */
public interface CanInteractData {
    @Nullable
    CompoundTag panlingre$getCanInteract();
}
