package icu.icuqalt10.panlingre.network.particle;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HuoQiuExplosionParticles(Vec3 pos, boolean blue, boolean large) implements CustomPacketPayload {
    public HuoQiuExplosionParticles(Vec3 pos) { this(pos, false, false); }
    public static final Type<HuoQiuExplosionParticles> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "huo_qiu_explosion_particles"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HuoQiuExplosionParticles> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.fromCodec(Vec3.CODEC), HuoQiuExplosionParticles::pos,
                    ByteBufCodecs.BOOL, HuoQiuExplosionParticles::blue,
                    ByteBufCodecs.BOOL, HuoQiuExplosionParticles::large,
                    HuoQiuExplosionParticles::new
            );

    public static void handle(HuoQiuExplosionParticles payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level commonLevel = context.player().level();
            if (!(commonLevel instanceof ClientLevel level)) {
                return;
            }

            Vec3 center = payload.pos();
            int particleCount = payload.large() ? 512 : 72;
            double startRadius = payload.large() ? 3.0 : 0.75;
            double particleSpeed = payload.large() ? 0.55 : 0.36;
            if (payload.blue()) {
                RandomSource random = level.getRandom();
                for (int i = 0; i < (payload.large() ? 128 : 24); i++) {
                    double x = random.nextDouble() * 2 - 1;
                    double y = random.nextDouble() * 2 - 1;
                    double z = random.nextDouble() * 2 - 1;
                    level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, true,
                            center.x + x * startRadius * 0.5,
                            center.y + y * startRadius * 0.5,
                            center.z + z * startRadius * 0.5,
                            x * particleSpeed, y * particleSpeed, z * particleSpeed);
                }
            } else {
                level.addParticle(
                        payload.large() ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION,
                        true,
                        center.x, center.y, center.z,
                        0.0, 0.0, 0.0
                );
            }

            for (int i = 0; i < particleCount; i++) {
                double angle = Math.PI * 2.0 * i / particleCount;
                double directionX = Math.cos(angle);
                double directionZ = Math.sin(angle);

                level.addParticle(
                        payload.blue() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME,
                        true,
                        center.x + directionX * startRadius,
                        center.y + 0.1,
                        center.z + directionZ * startRadius,
                        directionX * particleSpeed,
                        0.015,
                        directionZ * particleSpeed
                );
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
