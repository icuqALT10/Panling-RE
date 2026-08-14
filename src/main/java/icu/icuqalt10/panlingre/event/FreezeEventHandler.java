package icu.icuqalt10.panlingre.event;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.effect.FreezeEffect;
import icu.icuqalt10.panlingre.init.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = PanlingRE.MODID)
public class FreezeEventHandler {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFreezeApplicable(MobEffectEvent.Applicable event) {
        if (!event.getEffectInstance().is(ModEffects.freeze)) {
            return;
        }

        if (!FreezeEffect.canApplyTo(event.getEntity())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFreezeRemoved(MobEffectEvent.Remove event) {
        if (!event.isCanceled() && event.getEffect().equals(ModEffects.freeze)) {
            FreezeEffect.removeFrom(event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFreezeExpired(MobEffectEvent.Expired event) {
        if (!event.isCanceled() && event.getEffectInstance().is(ModEffects.freeze)) {
            FreezeEffect.removeFrom(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker && isFrozen(attacker)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (isFrozen(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isFrozen(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isFrozen(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isFrozen(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        if (isFrozen(entity)) {
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0, 0.0, 1.0));
        }
    }

    private static boolean isFrozen(LivingEntity entity) {
        return entity.hasEffect(ModEffects.freeze);
    }
}
