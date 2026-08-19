package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SiShouMusicPayload(boolean start) implements CustomPacketPayload {
    public static final Type<SiShouMusicPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "sishou_music")
    );
    public static final StreamCodec<ByteBuf, SiShouMusicPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SiShouMusicPayload::start,
            SiShouMusicPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
