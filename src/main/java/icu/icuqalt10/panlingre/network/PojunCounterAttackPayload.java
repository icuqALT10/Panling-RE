package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Requests that the client perform one counterattack against its current crosshair target. */
public record PojunCounterAttackPayload() implements CustomPacketPayload {
    public static final Type<PojunCounterAttackPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "pojun_counter_attack"));
    public static final StreamCodec<ByteBuf, PojunCounterAttackPayload> STREAM_CODEC =
            StreamCodec.unit(new PojunCounterAttackPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
