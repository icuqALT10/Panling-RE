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

public record GatherBall(Vec3 center, ParticleOptions particle,int count,double rad,double speed) implements CustomPacketPayload {

    public static final Type<GatherBall> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "particle_gatherball"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GatherBall> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.fromCodec(Vec3.CODEC), GatherBall::center,
                    ParticleTypes.STREAM_CODEC, GatherBall::particle,
                    ByteBufCodecs.VAR_INT, GatherBall::count,
                    ByteBufCodecs.DOUBLE, GatherBall::rad,
                    ByteBufCodecs.DOUBLE, GatherBall::speed,
                    GatherBall::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // 客户端收到数据包后的处理逻辑
    public static void handle(GatherBall payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 获取客户端的世界实例
            Level level = context.player().level();
            RandomSource random = level.getRandom();

            int particleCount = payload.count();
            double radius = payload.rad();
            double speed = payload.speed();

            for (int i = 0; i < particleCount; i++) {
                // 使用球面坐标系随机生成均匀分布的球面点
                double theta = random.nextDouble() * 2 * Math.PI;
                double phi = Math.acos(random.nextDouble() * 2 - 1);

                // 计算相对于中心的偏移量向量 (dx, dy, dz)
                double dx = Math.sin(phi) * Math.cos(theta);
                double dy = Math.sin(phi) * Math.sin(theta);
                double dz = Math.cos(phi);

                // 计算粒子的出生坐标（中心点 + 偏移量 * 半径）
                double startX = payload.center().x + dx * radius;
                double startY = payload.center().y + dy * radius;
                double startZ = payload.center().z + dz * radius;

                // 计算粒子的运动速度：方向指向中心，即偏移量的反方向，乘以设定的速度值
                double vx = -dx * speed;
                double vy = -dy * speed;
                double vz = -dz * speed;

                // 在客户端生成粒子
                level.addParticle(payload.particle(),true , startX, startY, startZ, vx, vy, vz);
            }
        });
    }
}