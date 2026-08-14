package icu.icuqalt10.panlingre.client;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;

/** Client-only renderer for the network-started layered cloud tornado. */
@EventBusSubscriber(modid = PanlingRE.MODID, value = Dist.CLIENT)
public final class Ys3JinTornadoParticles {
    private static final List<Storm> ACTIVE = new ArrayList<>();
    private static final int COLOR = 0xE3D4D1;

    private Ys3JinTornadoParticles() { }

    public static void start(Vec3 center, int durationTicks) {
        ACTIVE.add(new Storm(center, Math.max(1, durationTicks)));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || ACTIVE.isEmpty()) return;
        for (int stormIndex = ACTIVE.size() - 1; stormIndex >= 0; stormIndex--) {
            Storm storm = ACTIVE.get(stormIndex);
            if (storm.age++ >= storm.duration) {
                ACTIVE.remove(stormIndex);
                continue;
            }

            // Update only one layer each tick. This keeps distinct rings instead of building a solid cloud mass.
            int layerCount = 8;
            int layer = storm.age % layerCount;
            double layerFactor = layer / (double) (layerCount - 1);
            double y = 0.35D + layer * 1.0D;
            // The first ring is exactly one block wide; upper rings widen gradually.
            double maxRadius = 0.5D + layerFactor * 1.75D;
            int cycle = storm.age / layerCount;
            double expansion = (cycle * 0.16D) % 1.0D;
            double radius = layer == 0 ? 0.5D : maxRadius * expansion;
            int points = 12;
            double phase = storm.age * 0.08D + layer * 0.55D;
            for (int point = 0; point < points; point++) {
                double angle = phase + point * Mth.TWO_PI / points;
                double x = storm.center.x + Math.cos(angle) * radius;
                double z = storm.center.z + Math.sin(angle) * radius;
                minecraft.level.addParticle(ParticleTypes.CLOUD, true, x,
                        storm.center.y + y, z, 0.0D, 0.002D, 0.0D);
            }
        }
    }

    private static final class Storm {
        private final Vec3 center;
        private final int duration;
        private int age;

        private Storm(Vec3 center, int duration) {
            this.center = center;
            this.duration = duration;
        }
    }
}
