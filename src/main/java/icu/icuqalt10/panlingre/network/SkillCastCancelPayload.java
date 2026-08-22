package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.skill.SkillCastManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** Client request to cancel its current server-authoritative skill cast. */
public record SkillCastCancelPayload() implements CustomPacketPayload {
    public static final Type<SkillCastCancelPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "skill_cast_cancel"));
    public static final StreamCodec<ByteBuf, SkillCastCancelPayload> STREAM_CODEC =
            StreamCodec.unit(new SkillCastCancelPayload());

    public static void handle(SkillCastCancelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SkillCastManager.cancel(player);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
