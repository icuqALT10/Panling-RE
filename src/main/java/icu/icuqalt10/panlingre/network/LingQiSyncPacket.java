package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.LingQiData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record LingQiSyncPacket(UUID owner, float current, float max) implements CustomPacketPayload {

    public static final Type<LingQiSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "lingqi_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LingQiSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString), LingQiSyncPacket::owner,
            ByteBufCodecs.FLOAT, LingQiSyncPacket::current,
            ByteBufCodecs.FLOAT, LingQiSyncPacket::max,
            LingQiSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            LingQiData.ClientLingQiData.set(this.owner, this.current, this.max);
        });
    }
}
