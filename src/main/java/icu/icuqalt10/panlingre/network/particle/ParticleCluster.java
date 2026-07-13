package icu.icuqalt10.panlingre.network.particle;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.core.particles.ParticleOptions;
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

/**
 * 通用粒子团网络包
 * 可以指定起始位置、目标位置、粒子类型、数量和半径
 * 粒子会从起始位置的球形区域发射，并朝向目标位置移动
 */
public record ParticleCluster(Vec3 start, Vec3 target, ParticleOptions particle, int count, double radius) implements CustomPacketPayload {

    public static final Type<ParticleCluster> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "particle_cluster"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ParticleCluster> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.fromCodec(Vec3.CODEC), ParticleCluster::start,
                    ByteBufCodecs.fromCodec(Vec3.CODEC), ParticleCluster::target,
                    ParticleTypes.STREAM_CODEC, ParticleCluster::particle,
                    ByteBufCodecs.VAR_INT, ParticleCluster::count,
                    ByteBufCodecs.DOUBLE, ParticleCluster::radius,
                    ParticleCluster::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ParticleCluster payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = context.player().level();
            RandomSource random = level.getRandom();

            Vec3 direction = payload.target().subtract(payload.start());
            double distance = direction.length();

            // 根据距离计算速度，让粒子在合理时间内到达目标
            // 假设粒子存活时间约为40-60 ticks，计算需要的速度
            Vec3 velocity;
            if (distance < 0.001) {
                velocity = Vec3.ZERO;
            } else {
                double speed = distance / 5;
                velocity = direction.normalize().scale(speed);
            }

            int successCount = 0;
            for (int i = 0; i < payload.count(); i++) {
                // 使用极坐标法生成球体内随机点
                double theta = random.nextDouble() * 2 * Math.PI;
                double phi = Math.acos(2 * random.nextDouble() - 1);
                double r = Math.cbrt(random.nextDouble()) * payload.radius();

                double offsetX = r * Math.sin(phi) * Math.cos(theta);
                double offsetY = r * Math.sin(phi) * Math.sin(theta);
                double offsetZ = r * Math.cos(phi);

                double spawnX = payload.start().x + offsetX;
                double spawnY = payload.start().y + offsetY;
                double spawnZ = payload.start().z + offsetZ;

                level.addParticle(
                        payload.particle(),
                        true,
                        spawnX, spawnY, spawnZ,
                        velocity.x, velocity.y, velocity.z
                );
                successCount++;
            }

            // 调试信息
            PanlingRE.LOGGER.info("ParticleCluster spawned {} particles from {} to {} with velocity {} (distance: {})",
                    successCount, payload.start(), payload.target(), velocity, distance);
        });
    }
}
