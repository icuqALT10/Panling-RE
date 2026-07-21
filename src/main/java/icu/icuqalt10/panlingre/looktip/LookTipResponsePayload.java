package icu.icuqalt10.panlingre.looktip;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import icu.icuqalt10.panlingre.PanlingRE;

// 服务端发送给客户端：匹配结果
public record LookTipResponsePayload(
        boolean hasResult,
        Component tipText
) implements CustomPacketPayload {

    public static final Type<LookTipResponsePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "look_tip_response")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, LookTipResponsePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            LookTipResponsePayload::hasResult,
            ComponentSerialization.STREAM_CODEC,
            LookTipResponsePayload::tipText,
            LookTipResponsePayload::new
    );

    public static LookTipResponsePayload empty() {
        return new LookTipResponsePayload(false, Component.empty());
    }

    public static LookTipResponsePayload of(Component text) {
        return new LookTipResponsePayload(true, text);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
