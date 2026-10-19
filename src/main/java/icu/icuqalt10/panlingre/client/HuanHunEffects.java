package icu.icuqalt10.panlingre.client;

import icu.icuqalt10.panlingre.init.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public final class HuanHunEffects {
    private HuanHunEffects() {}

    public static void play(int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Entity entity = minecraft.level.getEntity(entityId);
        if (entity == null) return;
        minecraft.particleEngine.add(new Emitter(minecraft.level, entity));
        if (entity == minecraft.player) {
            minecraft.gameRenderer.displayItemActivation(new ItemStack(ModItems.huan_hun_fu.get()));
        }
    }

    /** Keeps the vanilla totem's 30-tick burst and sprite motion, with only black/white colors. */
    private static final class Emitter extends NoRenderParticle {
        private final Entity entity;

        private Emitter(ClientLevel level, Entity entity) {
            super(level, entity.getX(), entity.getY(0.5), entity.getZ());
            this.entity = entity;
        }

        @Override
        public void tick() {
            if (entity.isRemoved() || age++ >= 30) {
                remove();
                return;
            }
            for (int i = 0; i < 16; i++) {
                double dx = random.nextFloat() * 2.0 - 1.0;
                double dy = random.nextFloat() * 2.0 - 1.0;
                double dz = random.nextFloat() * 2.0 - 1.0;
                if (dx * dx + dy * dy + dz * dz > 1.0) continue;
                var particle = Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.TOTEM_OF_UNDYING,
                        entity.getX(dx / 4.0), entity.getY(0.5 + dy / 4.0), entity.getZ(dz / 4.0),
                        dx, dy + 0.2, dz);
                if (particle != null) {
                    float shade = random.nextBoolean() ? 1.0F : 0.0F;
                    particle.setColor(shade, shade, shade);
                }
            }
        }
    }
}
