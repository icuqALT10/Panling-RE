package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Replaces entity event 35 for the talisman without changing vanilla totems. */
public record HuanHunEffectsPayload(int entityId) implements CustomPacketPayload {
    public static final Type<HuanHunEffectsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "huan_hun_effects"));
    public static final StreamCodec<ByteBuf, HuanHunEffectsPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(HuanHunEffectsPayload::new, HuanHunEffectsPayload::entityId);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
