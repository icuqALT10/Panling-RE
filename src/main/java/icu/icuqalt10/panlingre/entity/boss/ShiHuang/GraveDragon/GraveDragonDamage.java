package icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Scale attacks once, including dragon projectiles and their lingering burning. */
@EventBusSubscriber(modid = PanlingRE.MODID)
public final class GraveDragonDamage {
    private static final String BURN_UNTIL = "GraveDragonBurnUntil";

    static void ignite(LivingEntity target, int seconds) {
        target.igniteForSeconds(seconds);
        target.getPersistentData().putLong(BURN_UNTIL,
                target.level().getGameTime() + target.getRemainingFireTicks());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void reduceDamage(LivingIncomingDamageEvent event) {
        var source = event.getSource();
        boolean dragon = source.getEntity() instanceof GraveDragonEntity
                || source.getDirectEntity() instanceof GraveDragonFireballEntity
                || source.getDirectEntity() instanceof GraveDragonRockEntity;
        boolean burning = source.is(DamageTypes.ON_FIRE) && event.getEntity().isOnFire()
                && event.getEntity().getPersistentData().getLong(BURN_UNTIL) > event.getEntity().level().getGameTime();
        if (dragon || burning) event.setAmount(event.getAmount() * (2F / 3F));
    }

    @SubscribeEvent
    public static void clearExtinguishedBurn(PlayerTickEvent.Post event) {
        var entity = event.getEntity();
        if (!entity.level().isClientSide && !entity.isOnFire()
                && entity.getPersistentData().contains(BURN_UNTIL)) entity.getPersistentData().remove(BURN_UNTIL);
    }
}
