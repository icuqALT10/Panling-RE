package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Reports which part the server actually resolved for a melee attack.
 *
 * <p>Client-side picking and server-side resolution can disagree (the client selects
 * through AABB envelopes, the server re-casts against the oriented boxes), and without
 * this feedback there is no way to tell which of the two produced a miss. The debug
 * overlay highlights the reported part so the disagreement is visible in game.
 *
 * @param requested part index the client named in its attack packet
 * @param struck    part index the server resolved and damaged
 * @param fromRay   whether the server's own ray produced {@code struck}, as opposed to
 *                  falling back to {@code requested}
 */
public record MeleeHitReportPayload(int requested, int struck, boolean fromRay)
        implements CustomPacketPayload {

    public static final Type<MeleeHitReportPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "melee_hit_report"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MeleeHitReportPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, MeleeHitReportPayload::requested,
                    ByteBufCodecs.VAR_INT, MeleeHitReportPayload::struck,
                    ByteBufCodecs.BOOL, MeleeHitReportPayload::fromRay,
                    MeleeHitReportPayload::new
            );

    public static void handle(final MeleeHitReportPayload payload,
                              final net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (net.neoforged.fml.loading.FMLEnvironment.dist
                    == net.neoforged.api.distmarker.Dist.CLIENT) {
                icu.icuqalt10.panlingre.client.MeleeHitDebugState.record(
                        payload.requested(), payload.struck(), payload.fromRay());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
