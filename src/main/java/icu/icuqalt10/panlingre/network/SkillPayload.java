package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record SkillPayload(int keyID) implements CustomPacketPayload {
    public static final Type<SkillPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "skill_packet"));

    public static final StreamCodec<ByteBuf, SkillPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SkillPayload::keyID,
            SkillPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}