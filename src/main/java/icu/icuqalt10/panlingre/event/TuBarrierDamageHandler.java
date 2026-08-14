package icu.icuqalt10.panlingre.event;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.entity.TuBarrierEntity;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = PanlingRE.MODID)
public final class TuBarrierDamageHandler {
    private TuBarrierDamageHandler() { }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel serverLevel)) return;

        TuBarrierEntity barrier = TuBarrierEntity.findProtecting(serverLevel, event.getEntity());
        if (barrier == null) return;

        barrier.absorbDamage(event.getAmount());
        event.setCanceled(true);
    }
}
