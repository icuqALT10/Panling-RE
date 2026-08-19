package icu.icuqalt10.panlingre.event;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.world.entity.monster.Blaze;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;

/** Prevents blaze fireballs from placing fire when they hit blocks. */
@EventBusSubscriber(modid = PanlingRE.MODID)
public final class BlazeFireballHandler {
    private BlazeFireballHandler() { }

    @SubscribeEvent
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() instanceof Blaze) {
            event.setCanGrief(false);
        }
    }
}
