package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.client.GroundSmashRenderer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record GroundSmashPayload(
        Vec3 center,
        float radius,
        int ticks
) implements CustomPacketPayload {

    public static final Type<GroundSmashPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "ground_smash"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GroundSmashPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.fromCodec(Vec3.CODEC), GroundSmashPayload::center,
                    ByteBufCodecs.FLOAT, GroundSmashPayload::radius,
                    ByteBufCodecs.VAR_INT, GroundSmashPayload::ticks,
                    GroundSmashPayload::new
            );

    public static void handle(final GroundSmashPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            GroundSmashRenderer.triggerSmash(payload.center(), payload.radius(), payload.ticks());
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
