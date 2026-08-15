package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record QinglongMusicPayload(boolean start) implements CustomPacketPayload {
    public static final Type<QinglongMusicPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "qinglong_music")
    );
    public static final StreamCodec<ByteBuf, QinglongMusicPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, QinglongMusicPayload::start,
            QinglongMusicPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
