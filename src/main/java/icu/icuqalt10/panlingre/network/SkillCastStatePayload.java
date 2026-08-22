package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** Synchronizes a wheel-skill wind-up for first-person HUD and third-person animation. */
public record SkillCastStatePayload(int entityId, int durationTicks, boolean mainHand)
        implements CustomPacketPayload {

    public static final Type<SkillCastStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "skill_cast_state"));

    public static final StreamCodec<ByteBuf, SkillCastStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SkillCastStatePayload::entityId,
                    ByteBufCodecs.VAR_INT, SkillCastStatePayload::durationTicks,
                    ByteBufCodecs.BOOL, SkillCastStatePayload::mainHand,
                    SkillCastStatePayload::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
