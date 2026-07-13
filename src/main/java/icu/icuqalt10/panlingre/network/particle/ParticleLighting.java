package icu.icuqalt10.panlingre.network.particle;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ParticleLighting(
        Vec3 Pos
) implements CustomPacketPayload {
    public static final Type<ParticleLighting> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            PanlingRE.MODID,
                            "particle_lighting"
                    )
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, ParticleLighting> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.fromCodec(Vec3.CODEC), ParticleLighting::Pos,
                    ParticleLighting::new
            );

    /**
     * NeoForge 1.21.1 安全客户端接收处理器
     */
    public static void handle(final ParticleLighting payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Level commonLevel = context.player().level();

            // 类型强转，确保当前确实处于客户端世界中
            if (commonLevel instanceof ClientLevel level) {
                Vec3 targetPos = payload.Pos();
                RandomSource random = level.getRandom();

                //闪烁粒子
                level.addParticle(
                        ParticleTypes.FLASH,
                        true,
                        targetPos.x, targetPos.y, targetPos.z,
                        0,0,0
                );

                level.playLocalSound(
                        targetPos.x, targetPos.y, targetPos.z,
                        SoundEvents.LIGHTNING_BOLT_IMPACT,
                        SoundSource.WEATHER,
                        1.0F,
                        1.0F,
                        false
                );

                // 外层循环：Y 轴每隔 0.5 格生成一处 (0 到 10)
                for (double y = 0; y < 20; y += 0.5) {
                    double baseX = targetPos.x;
                    double baseY = targetPos.y + y;
                    double baseZ = targetPos.z;

                    // 内层循环：每个位置生成 5 个粒子
                    for (int i = 0; i < 5; i++) {
                        double rx = baseX + random.nextGaussian() * 0.1;
                        double ry = baseY + random.nextGaussian() * 0.1;
                        double rz = baseZ + random.nextGaussian() * 0.1;

                        double vx = random.nextGaussian() * 0.05;
                        double vy = random.nextGaussian() * 0.05;
                        double vz = random.nextGaussian() * 0.05;

                        level.addParticle(
                                ParticleTypes.ELECTRIC_SPARK,
                                true,
                                rx, ry, rz,
                                vx, vy, vz
                        );
                    }
                }
            }
        });
    }


    @Override
    public Type<? extends CustomPacketPayload> type(){

        return TYPE;

    }
}