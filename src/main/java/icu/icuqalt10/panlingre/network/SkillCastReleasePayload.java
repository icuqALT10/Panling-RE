package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** Starts the brief forward throw and return animation after a successful cast. */
public record SkillCastReleasePayload(int entityId, boolean mainHand)
        implements CustomPacketPayload {
    public static final Type<SkillCastReleasePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "skill_cast_release"));

    public static final StreamCodec<ByteBuf, SkillCastReleasePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SkillCastReleasePayload::entityId,
                    ByteBufCodecs.BOOL, SkillCastReleasePayload::mainHand,
                    SkillCastReleasePayload::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
