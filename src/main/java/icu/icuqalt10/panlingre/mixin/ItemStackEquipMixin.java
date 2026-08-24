package icu.icuqalt10.panlingre.mixin;

import icu.icuqalt10.panlingre.player.ProfessionEquipmentGuard;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.inventory.ArmorSlot")
public abstract class ItemStackEquipMixin {
    @Shadow
    @Final
    private LivingEntity owner;

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void panlingre$rejectWrongProfessionArmor(
            ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!(owner instanceof Player player)) return;
        if (ProfessionEquipmentGuard.isInvalidForProfession(player, stack)) {
            cir.setReturnValue(false);
        }
    }
}
