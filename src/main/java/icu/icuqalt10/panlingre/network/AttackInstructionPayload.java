package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AttackInstructionPayload(int entityId, String instruction) implements CustomPacketPayload {

    public static final Type<AttackInstructionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "attack_instruction"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AttackInstructionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, AttackInstructionPayload::entityId,
                    ByteBufCodecs.STRING_UTF8, AttackInstructionPayload::instruction,
                    AttackInstructionPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}