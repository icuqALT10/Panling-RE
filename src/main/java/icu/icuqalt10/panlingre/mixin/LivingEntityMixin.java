package icu.icuqalt10.panlingre.mixin;

import icu.icuqalt10.panlingre.init.ModComponents;
import icu.icuqalt10.panlingre.item.warrior.ding_hai_shen_zhen;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    //护甲值伤害计算
    @Inject(method = "getDamageAfterArmorAbsorb", at = @At("HEAD"), cancellable = true)
    private void panlingre$modifyArmorCalculation(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
            return;
        }

        LivingEntity entity = (LivingEntity) (Object) this;
        double armor = entity.getAttributeValue(Attributes.ARMOR);
        double k = 100.0;
        float finalDamage;

        if (armor >= 0) {
            // 100 护甲 = 50% 减伤
            // 200 护甲 = 66.6% 减伤
            finalDamage = (float) (amount * (k / (k + armor)));
        } else {
            // armor = -100 时，增伤 50% (1.5倍)
            // armor = -300 时，增伤 75% (1.75倍)
            finalDamage = (float) (amount * (2.0 - (k / (k - armor))));
        }

        cir.setReturnValue(finalDamage);
    }

    //攻击无敌时间修改
    // 记录每个受害者最后一次被你的武器攻击的时间
    @Unique
    private long panlingre$lastDingHaiHurtTime;
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void panlingre$forceAttackInterval(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Entity attacker = source.getDirectEntity();
        if (attacker instanceof LivingEntity livingAttacker) {
            ItemStack weapon = livingAttacker.getMainHandItem();

            if (weapon.getItem() instanceof ding_hai_shen_zhen &&
                    weapon.getOrDefault(ModComponents.IS_POWERED.get(), false)) {

                LivingEntity target = (LivingEntity) (Object) this;
                long currentTime = target.level().getGameTime();

                if (currentTime - this.panlingre$lastDingHaiHurtTime < 3) {
                    cir.setReturnValue(false);
                    return;
                }

                target.invulnerableTime = 0;
                this.panlingre$lastDingHaiHurtTime = currentTime;
            }
        }
    }
}