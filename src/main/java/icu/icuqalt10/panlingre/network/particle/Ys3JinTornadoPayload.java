package icu.icuqalt10.panlingre.network.particle;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.client.Ys3JinTornadoParticles;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Starts one client-side tornado particle effect without synchronizing every particle. */
public record Ys3JinTornadoPayload(Vec3 center, int durationTicks) implements CustomPacketPayload {
    public static final Type<Ys3JinTornadoPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "ys3_jin_tornado"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Ys3JinTornadoPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.fromCodec(Vec3.CODEC), Ys3JinTornadoPayload::center,
                    ByteBufCodecs.VAR_INT, Ys3JinTornadoPayload::durationTicks,
                    Ys3JinTornadoPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(Ys3JinTornadoPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Ys3JinTornadoParticles.start(payload.center(), payload.durationTicks()));
    }
}
