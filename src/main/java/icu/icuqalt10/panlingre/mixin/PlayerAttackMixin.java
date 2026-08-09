package icu.icuqalt10.panlingre.mixin;

import icu.icuqalt10.panlingre.init.ModComponents;
import icu.icuqalt10.panlingre.item.warrior.ding_hai_shen_zhen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayerAttackMixin {

    @Redirect(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"
            )
    )
    private void panlingre$disablePoweredDingHaiShenZhenKnockback(
            LivingEntity target, double strength, double x, double z
    ) {
        Player attacker = (Player) (Object) this;
        ItemStack weapon = attacker.getMainHandItem();

        // IS_POWERED is shared by multiple weapons, so the item type check is required.
        if (weapon.getItem() instanceof ding_hai_shen_zhen
                && weapon.getOrDefault(ModComponents.IS_POWERED.get(), false)) {
            return;
        }

        target.knockback(strength, x, z);
    }
}
