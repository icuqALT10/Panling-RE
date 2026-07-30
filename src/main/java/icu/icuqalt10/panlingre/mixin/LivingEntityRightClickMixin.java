package icu.icuqalt10.panlingre.mixin;

import icu.icuqalt10.panlingre.init.ModComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LivingEntityRightClickMixin {
    @Redirect(
            method = "stopUsingItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;onStopUsing(Lnet/minecraft/world/entity/LivingEntity;I)V"
            )
    )
    private void panlingre$skipOriginalStopUsing(ItemStack stack, LivingEntity entity, int count) {
        if (!stack.has(ModComponents.RIGHT_CLICK.get())) {
            stack.onStopUsing(entity, count);
        }
    }
}
