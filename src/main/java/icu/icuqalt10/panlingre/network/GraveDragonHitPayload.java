package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Chat-visible record of one damage event on a multipart boss.
 *
 * <p>Sent to the attacking player so every hit reports the part that took it, the part's
 * damage multiplier and the damage actually applied. This turns "I aimed at the leg and
 * nothing happened" into something readable: either no line appears (the attack was
 * rejected) or the line names a different part than the one under the crosshair.
 *
 * <p>Every value is carried in the packet, including the remaining health, so the client
 * can render the line without having to resolve the entity again — the entity may not be
 * client-side yet, which silently swallowed earlier reports.
 *
 * @param entityId   the damaged root entity
 * @param partIndex  part index the server resolved
 * @param partLabel  human readable part name
 * @param multiplier damage multiplier of that part
 * @param amount     damage applied after mitigation
 * @param remaining  entity health after the hit
 */
public record GraveDragonHitPayload(int entityId, int partIndex, String partLabel,
                                    float multiplier, float amount, float remaining)
        implements CustomPacketPayload {

    public static final Type<GraveDragonHitPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "grave_dragon_hit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GraveDragonHitPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, GraveDragonHitPayload::entityId,
                    ByteBufCodecs.VAR_INT, GraveDragonHitPayload::partIndex,
                    ByteBufCodecs.STRING_UTF8, GraveDragonHitPayload::partLabel,
                    ByteBufCodecs.FLOAT, GraveDragonHitPayload::multiplier,
                    ByteBufCodecs.FLOAT, GraveDragonHitPayload::amount,
                    ByteBufCodecs.FLOAT, GraveDragonHitPayload::remaining,
                    GraveDragonHitPayload::new
            );

    public static void handle(final GraveDragonHitPayload payload,
                              final net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (net.neoforged.fml.loading.FMLEnvironment.dist
                    == net.neoforged.api.distmarker.Dist.CLIENT) {
                icu.icuqalt10.panlingre.client.GraveDragonDamageLog.record(payload);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
