package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.util.LocalWeatherManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FakeSnowPayload(boolean isSnowing) implements CustomPacketPayload {

    public static final Type<FakeSnowPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "fake_snow"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FakeSnowPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, FakeSnowPayload::isSnowing,
                    FakeSnowPayload::new

            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // 客户端收到数据包后的处理逻辑
    public static void handle(FakeSnowPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            LocalWeatherManager.ClientWeatherState.isFakeSnowing = payload.isSnowing();
        });
    }
}