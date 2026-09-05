package icu.icuqalt10.panlingre.mixin;

import icu.icuqalt10.panlingre.player.ProfessionEquipmentGuard;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class OffhandSlotMixin {
    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void panlingre$rejectWrongProfessionOffhand(
            ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Slot slot = (Slot) (Object) this;
        if (!(slot.container instanceof Inventory inventory)
                || slot.getContainerSlot() != Inventory.SLOT_OFFHAND) return;

        if (ProfessionEquipmentGuard.isHandCarryExempt(stack)) return;

        if (ProfessionEquipmentGuard.isInvalidForProfession(inventory.player, stack)) {
            cir.setReturnValue(false);
        }
    }
}
