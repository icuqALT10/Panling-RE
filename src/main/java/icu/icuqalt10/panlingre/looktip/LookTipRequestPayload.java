package icu.icuqalt10.panlingre.looktip;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import icu.icuqalt10.panlingre.PanlingRE;

import java.util.UUID;

// 客户端发送给服务端：请求匹配检查
public record LookTipRequestPayload(
        int typeOrdinal,
        UUID entityUuid,
        BlockPos blockPos
) implements CustomPacketPayload {

    public static final Type<LookTipRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "look_tip_request")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, LookTipRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            LookTipRequestPayload::typeOrdinal,
            StreamCodec.of(
                    (buf, uuid) -> {
                        buf.writeLong(uuid.getMostSignificantBits());
                        buf.writeLong(uuid.getLeastSignificantBits());
                    },
                    buf -> new UUID(buf.readLong(), buf.readLong())
            ),
            LookTipRequestPayload::entityUuid,
            BlockPos.STREAM_CODEC,
            LookTipRequestPayload::blockPos,
            LookTipRequestPayload::new
    );

    public TargetType getType() {
        return TargetType.values()[typeOrdinal];
    }

    public static LookTipRequestPayload create(TargetType type, UUID entityUuid, BlockPos blockPos) {
        return new LookTipRequestPayload(type.ordinal(), entityUuid, blockPos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum TargetType {
        ENTITY,
        BLOCK,
        NONE
    }
}
