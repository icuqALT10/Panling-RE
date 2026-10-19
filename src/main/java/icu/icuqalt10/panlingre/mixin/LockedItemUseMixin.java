package icu.icuqalt10.panlingre.mixin;

import icu.icuqalt10.panlingre.attachment.ItemUnlockData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemStack.class, priority = 1100)
public abstract class LockedItemUseMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void panlingre$checkUnlock(Level level, Player player, InteractionHand hand,
                                      CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!ItemUnlockData.canUse(player, stack)) cir.setReturnValue(InteractionResultHolder.pass(stack));
    }

    @Inject(method = {"useOn", "onItemUseFirst"}, at = @At("HEAD"), cancellable = true)
    private void panlingre$checkBlockUnlock(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!ItemUnlockData.canUse(context.getPlayer(), (ItemStack) (Object) this)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "interactLivingEntity", at = @At("HEAD"), cancellable = true)
    private void panlingre$checkEntityUnlock(Player player, LivingEntity target, InteractionHand hand,
                                            CallbackInfoReturnable<InteractionResult> cir) {
        if (!ItemUnlockData.canUse(player, (ItemStack) (Object) this)) cir.setReturnValue(InteractionResult.PASS);
    }
}
