package icu.icuqalt10.panlingre.network.task;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TaskEntityCheckPayload(ResourceLocation taskId, int[] entityIds) implements CustomPacketPayload {
    public static final int MAX_CANDIDATES = 128;
    public static final Type<TaskEntityCheckPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "task_entity_check")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, TaskEntityCheckPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeResourceLocation(payload.taskId);
                int length = Math.min(payload.entityIds.length, MAX_CANDIDATES);
                buf.writeVarInt(length);
                for (int index = 0; index < length; index++) {
                    buf.writeVarInt(payload.entityIds[index]);
                }
            },
            buf -> {
                ResourceLocation taskId = buf.readResourceLocation();
                int length = buf.readVarInt();
                if (length < 0 || length > MAX_CANDIDATES) {
                    throw new IllegalArgumentException("Invalid task entity candidate count: " + length);
                }
                int[] ids = new int[length];
                for (int index = 0; index < length; index++) {
                    ids[index] = buf.readVarInt();
                }
                return new TaskEntityCheckPayload(taskId, ids);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
