package icu.icuqalt10.panlingre.client;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = PanlingRE.MODID, value = Dist.CLIENT)
public final class ZhuqueMeteorWarningParticles {
    private static final int POINTS = 48;
    private static final DustParticleOptions WARNING_PARTICLE =
            new DustParticleOptions(new Vector3f(1.0F, 0.75F, 0.0F), 1.25F);
    private static final List<Warning> WARNINGS = new ArrayList<>();

    private ZhuqueMeteorWarningParticles() {
    }

    public static void start(Vec3 center, float radius, int durationTicks) {
        WARNINGS.add(new Warning(center, radius, Math.max(1, durationTicks)));
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            WARNINGS.clear();
            return;
        }

        Iterator<Warning> iterator = WARNINGS.iterator();
        while (iterator.hasNext()) {
            Warning warning = iterator.next();
            warning.remainingTicks--;
            if (warning.remainingTicks <= 0) {
                iterator.remove();
                continue;
            }

            if (warning.remainingTicks % 2 != 0) continue;

            for (int point = 0; point < POINTS; point++) {
                double angle = Math.PI * 2.0 * point / POINTS;
                minecraft.level.addAlwaysVisibleParticle(
                        WARNING_PARTICLE,
                        true,
                        warning.center.x + Math.cos(angle) * warning.radius,
                        warning.center.y + 0.05,
                        warning.center.z + Math.sin(angle) * warning.radius,
                        0.0, 0.0, 0.0
                );
            }
        }
    }

    private static final class Warning {
        final Vec3 center;
        final float radius;
        int remainingTicks;

        Warning(Vec3 center, float radius, int totalTicks) {
            this.center = center;
            this.radius = radius;
            this.remainingTicks = totalTicks;
        }
    }
}
