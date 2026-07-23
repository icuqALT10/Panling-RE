package icu.icuqalt10.panlingre.network.task;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TaskEntityResultPayload(ResourceLocation taskId, int[] entityIds) implements CustomPacketPayload {
    public static final Type<TaskEntityResultPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "task_entity_result")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, TaskEntityResultPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeResourceLocation(payload.taskId);
                int length = Math.min(payload.entityIds.length, TaskEntityCheckPayload.MAX_CANDIDATES);
                buf.writeVarInt(length);
                for (int index = 0; index < length; index++) {
                    buf.writeVarInt(payload.entityIds[index]);
                }
            },
            buf -> {
                ResourceLocation taskId = buf.readResourceLocation();
                int length = buf.readVarInt();
                if (length < 0 || length > TaskEntityCheckPayload.MAX_CANDIDATES) {
                    throw new IllegalArgumentException("Invalid task entity result count: " + length);
                }
                int[] ids = new int[length];
                for (int index = 0; index < length; index++) {
                    ids[index] = buf.readVarInt();
                }
                return new TaskEntityResultPayload(taskId, ids);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
