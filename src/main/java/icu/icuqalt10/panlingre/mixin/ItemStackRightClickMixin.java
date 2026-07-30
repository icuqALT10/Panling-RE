package icu.icuqalt10.panlingre.mixin;

import icu.icuqalt10.panlingre.component.RightClickComponent;
import icu.icuqalt10.panlingre.component.RightClickComponentHandler;
import icu.icuqalt10.panlingre.init.ModComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackRightClickMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void panlingre$useRightClickComponent(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir
    ) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!stack.has(ModComponents.RIGHT_CLICK.get())) {
            return;
        }

        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            cir.setReturnValue(InteractionResultHolder.fail(stack));
            return;
        }

        player.startUsingItem(hand);
        cir.setReturnValue(InteractionResultHolder.sidedSuccess(stack, level.isClientSide()));
    }

    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void panlingre$getRightClickUseDuration(
            LivingEntity entity,
            CallbackInfoReturnable<Integer> cir
    ) {
        RightClickComponent component =
                ((ItemStack) (Object) this).get(ModComponents.RIGHT_CLICK.get());
        if (component != null) {
            cir.setReturnValue(component.usingTime());
        }
    }

    @Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
    private void panlingre$finishRightClickComponent(
            Level level,
            LivingEntity entity,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        ItemStack stack = (ItemStack) (Object) this;
        RightClickComponent component = stack.get(ModComponents.RIGHT_CLICK.get());
        if (component == null) {
            return;
        }

        if (entity instanceof ServerPlayer player) {
            RightClickComponentHandler.completeUse(player, stack, component);
        }
        cir.setReturnValue(stack);
    }

    @Inject(method = "onUseTick", at = @At("HEAD"), cancellable = true)
    private void panlingre$skipOriginalUseTick(
            Level level,
            LivingEntity entity,
            int remainingUseDuration,
            CallbackInfo ci
    ) {
        if (((ItemStack) (Object) this).has(ModComponents.RIGHT_CLICK.get())) {
            ci.cancel();
        }
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    private void panlingre$skipOriginalReleaseUsing(
            Level level,
            LivingEntity entity,
            int timeLeft,
            CallbackInfo ci
    ) {
        if (((ItemStack) (Object) this).has(ModComponents.RIGHT_CLICK.get())) {
            ci.cancel();
        }
    }

    @Inject(method = "useOnRelease", at = @At("HEAD"), cancellable = true)
    private void panlingre$disableOriginalUseOnRelease(CallbackInfoReturnable<Boolean> cir) {
        if (((ItemStack) (Object) this).has(ModComponents.RIGHT_CLICK.get())) {
            cir.setReturnValue(false);
        }
    }
}
