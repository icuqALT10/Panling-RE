package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端→服务端：请求传送
 */
public record BaFangYiTeleportPayload(String majorId, String subId) implements CustomPacketPayload {

    public static final Type<BaFangYiTeleportPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "ba_fang_yi_teleport")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, BaFangYiTeleportPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BaFangYiTeleportPayload::majorId,
            ByteBufCodecs.STRING_UTF8, BaFangYiTeleportPayload::subId,
            BaFangYiTeleportPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
