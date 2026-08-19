package icu.icuqalt10.panlingre.network.particle;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.client.ZhuqueMeteorWarningParticles;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ZhuqueMeteorWarningPayload(Vec3 center, float radius, int durationTicks)
        implements CustomPacketPayload {
    public static final Type<ZhuqueMeteorWarningPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "zhuque_meteor_warning")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ZhuqueMeteorWarningPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.fromCodec(Vec3.CODEC), ZhuqueMeteorWarningPayload::center,
                    ByteBufCodecs.FLOAT, ZhuqueMeteorWarningPayload::radius,
                    ByteBufCodecs.VAR_INT, ZhuqueMeteorWarningPayload::durationTicks,
                    ZhuqueMeteorWarningPayload::new
            );

    public static void handle(ZhuqueMeteorWarningPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ZhuqueMeteorWarningParticles.start(
                payload.center(), payload.radius(), payload.durationTicks()
        ));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
