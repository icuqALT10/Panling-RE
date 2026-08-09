package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.skill.ClientSkillState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * 服务端确认技能已经成功执行后，通知客户端开始显示冷却。
 */
public record SkillUseSucceededPayload(String cooldownKey, long cooldown)
        implements CustomPacketPayload {

    public static final Type<SkillUseSucceededPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    PanlingRE.MODID, "skill_use_succeeded"));

    public static final StreamCodec<ByteBuf, SkillUseSucceededPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, SkillUseSucceededPayload::cooldownKey,
                    ByteBufCodecs.VAR_LONG, SkillUseSucceededPayload::cooldown,
                    SkillUseSucceededPayload::new
            );

    public static void handle(
            final SkillUseSucceededPayload payload,
            final IPayloadContext context
    ) {
        context.enqueueWork(() ->
                ClientSkillState.recordCooldown(payload.cooldownKey(), payload.cooldown()));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
