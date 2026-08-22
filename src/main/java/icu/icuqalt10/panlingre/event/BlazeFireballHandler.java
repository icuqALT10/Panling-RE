package icu.icuqalt10.panlingre.event;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

/** Prevents blaze fireballs from placing fire when they hit blocks. */
@EventBusSubscriber(modid = PanlingRE.MODID)
public final class BlazeFireballHandler {
    private BlazeFireballHandler() { }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof SmallFireball fireball)
                || !(fireball.getOwner() instanceof Blaze)
                || event.getRayTraceResult().getType() != HitResult.Type.BLOCK) {
            return;
        }

        // Cancel before SmallFireball#onHitBlock can place a fire block, then remove the
        // projectile because a canceled impact would otherwise let it continue flying.
        event.setCanceled(true);
        fireball.discard();
    }
}
