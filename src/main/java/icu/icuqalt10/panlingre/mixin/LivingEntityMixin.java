package icu.icuqalt10.panlingre.mixin;

import icu.icuqalt10.panlingre.init.ModComponents;
import icu.icuqalt10.panlingre.item.warrior.ding_hai_shen_zhen;
import icu.icuqalt10.panlingre.util.DamageRecord;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow public float lastHurt;

    // --- 1. 护甲计算公式修改 ---
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

    // --- 2. 独立伤害冷却记录器 ---

    // 注意这里：改用我们外部包的 DamageRecord 类
    @Unique
    private final Map<String, DamageRecord> panlingre$attackerCooldowns = new HashMap<>();

    @Unique
    private String panlingre$currentAttackerKey = null;

    @Unique
    private int panlingre$storedGlobalInvulTime;

    @Unique
    private float panlingre$storedGlobalLastHurt;

    // Tick 更新 Map 里的冷却时间
    @Inject(method = "tick", at = @At("TAIL"))
    private void panlingre$tickCooldowns(CallbackInfo ci) {
        if (!this.level().isClientSide) {
            Iterator<Map.Entry<String, DamageRecord>> iterator = panlingre$attackerCooldowns.entrySet().iterator();
            while (iterator.hasNext()) {
                DamageRecord record = iterator.next().getValue();
                if (record.invulnerableTime > 0) {
                    record.invulnerableTime--;
                }
                if (record.invulnerableTime <= 0) {
                    iterator.remove();
                }
            }
        }
    }

    // 在判定伤害前，替换为该攻击者的专属冷却
    @Inject(method = "hurt", at = @At("HEAD"))
    private void panlingre$onHurtHead(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (this.level().isClientSide) return;

        Entity attacker = source.getEntity();
        this.panlingre$currentAttackerKey = attacker != null ? attacker.getStringUUID() : source.type().msgId();

        this.panlingre$storedGlobalInvulTime = this.invulnerableTime;
        this.panlingre$storedGlobalLastHurt = this.lastHurt;

        DamageRecord record = this.panlingre$attackerCooldowns.get(this.panlingre$currentAttackerKey);
        if (record != null) {
            this.invulnerableTime = record.invulnerableTime;
            this.lastHurt = record.lastHurt;
        } else {
            this.invulnerableTime = 0;
            this.lastHurt = 0.0F;
        }
    }

    // 在判定伤害后，保存攻击者状态并特判定海神针的攻速
    @Inject(method = "hurt", at = @At("RETURN"))
    private void panlingre$onHurtReturn(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (this.level().isClientSide) return;

        if (this.panlingre$currentAttackerKey != null) {
            int newInvulTime = this.invulnerableTime;

            if (cir.getReturnValueZ()) {
                Entity attacker = source.getDirectEntity();
                if (attacker instanceof LivingEntity livingAttacker) {
                    ItemStack weapon = livingAttacker.getMainHandItem();

                    if (weapon.getItem() instanceof ding_hai_shen_zhen &&
                            weapon.getOrDefault(ModComponents.IS_POWERED.get(), false)) {
                        newInvulTime = 13;
                    }
                }
            }

            // 实例化外部的 DamageRecord 类，完全合法
            this.panlingre$attackerCooldowns.put(
                    this.panlingre$currentAttackerKey,
                    new DamageRecord(newInvulTime, this.lastHurt)
            );

            int maxInvul = this.panlingre$storedGlobalInvulTime;
            for (DamageRecord rec : this.panlingre$attackerCooldowns.values()) {
                if (rec.invulnerableTime > maxInvul) {
                    maxInvul = rec.invulnerableTime;
                }
            }

            this.invulnerableTime = maxInvul;
            this.lastHurt = this.panlingre$storedGlobalLastHurt;

            this.panlingre$currentAttackerKey = null;
        }
    }
}