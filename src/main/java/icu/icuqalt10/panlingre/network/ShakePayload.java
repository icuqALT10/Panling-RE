package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.client.ClientModEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ShakePayload(
        Vec3 center,
        double radius,
        int ticks,
        float intensity
) implements CustomPacketPayload {

    public static final Type<ShakePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "shake"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShakePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.fromCodec(Vec3.CODEC), ShakePayload::center,
                    ByteBufCodecs.DOUBLE, ShakePayload::radius,
                    ByteBufCodecs.VAR_INT, ShakePayload::ticks,
                    ByteBufCodecs.FLOAT, ShakePayload::intensity,
                    ShakePayload::new
            );

    public static void handle(final ShakePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientModEvents.startShake(payload.center(), payload.radius(), payload.ticks(), payload.intensity());
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
