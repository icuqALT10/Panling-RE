package icu.icuqalt10.panlingre.mixin;

import icu.icuqalt10.panlingre.init.ModComponents;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.event.DiShiDunEvents;
import icu.icuqalt10.panlingre.item.warrior.ding_hai_shen_zhen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerAttackMixin {

    @Unique
    private static final ResourceLocation PANLINGRE$POJUN_COUNTER_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "di_shi_dun_pojun_counter_damage");
    @Unique
    private boolean panlingre$pojunCounterDamageApplied;

    @Inject(method = "attack", at = @At("HEAD"))
    private void panlingre$beginPojunCounterAttack(Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (!DiShiDunEvents.consumeArmedCounterAttack(player)) return;

        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage == null) return;
        attackDamage.removeModifier(PANLINGRE$POJUN_COUNTER_DAMAGE_ID);
        attackDamage.addTransientModifier(new AttributeModifier(
                PANLINGRE$POJUN_COUNTER_DAMAGE_ID,
                1.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        panlingre$pojunCounterDamageApplied = true;
    }

    @Inject(method = "attack", at = @At("RETURN"))
    private void panlingre$endPojunCounterAttack(Entity target, CallbackInfo ci) {
        if (!panlingre$pojunCounterDamageApplied) return;
        Player player = (Player) (Object) this;
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) attackDamage.removeModifier(PANLINGRE$POJUN_COUNTER_DAMAGE_ID);
        panlingre$pojunCounterDamageApplied = false;
    }

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
