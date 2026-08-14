package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.event.DiShiDunEvents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 客户端确认准星存在实体，武装紧随其后的原版攻击包。 */
public record PojunCounterAttackReadyPayload() implements CustomPacketPayload {

    public static final Type<PojunCounterAttackReadyPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "pojun_counter_attack_ready"));
    public static final StreamCodec<ByteBuf, PojunCounterAttackReadyPayload> STREAM_CODEC =
            StreamCodec.unit(new PojunCounterAttackReadyPayload());

    public static void handle(PojunCounterAttackReadyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                DiShiDunEvents.armAuthorizedCounterAttack(player);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
