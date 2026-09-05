package icu.icuqalt10.panlingre.mixin.compat.beyonddimensions;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.wintercogs.beyonddimensions.common.item.NetMagnetItem", remap = false)
public abstract class DisabledNetMagnetItemMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true, remap = false)
    private void panlingre$disableUse(Level level, Player player, InteractionHand hand,
                                     CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        cir.setReturnValue(InteractionResultHolder.fail(player.getItemInHand(hand)));
    }

    @Inject(method = "shouldWork", at = @At("HEAD"), cancellable = true, remap = false)
    private void panlingre$disableWorkCheck(ItemStack stack, Level level, Entity entity,
                                            int slot, boolean selected,
                                            CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "workContent", at = @At("HEAD"), cancellable = true, remap = false)
    private void panlingre$disableWorkContent(ItemStack stack, Level level, Entity entity,
                                              int slot, boolean selected, CallbackInfo ci) {
        ci.cancel();
    }
}
