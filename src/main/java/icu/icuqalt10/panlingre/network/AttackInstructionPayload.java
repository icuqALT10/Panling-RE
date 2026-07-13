package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.entity.boss.PanGuEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AttackInstructionPayload(int entityId, String instruction) implements CustomPacketPayload {

    public static final Type<AttackInstructionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "attack_instruction"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AttackInstructionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, AttackInstructionPayload::entityId,
                    ByteBufCodecs.STRING_UTF8, AttackInstructionPayload::instruction,
                    AttackInstructionPayload::new
            );

    public static void handle(AttackInstructionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player(); // 发包的玩家
            if (player.level().getEntity(payload.entityId()) instanceof PanGuEntity boss) {
                boss.serverHandleInstruction(payload.instruction());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}