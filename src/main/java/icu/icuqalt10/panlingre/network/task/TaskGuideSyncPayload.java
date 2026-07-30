package icu.icuqalt10.panlingre.network.task;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TaskGuideSyncPayload(
        boolean enabled,
        ResourceLocation taskId,
        String json,
        int selectedEntry
) implements CustomPacketPayload {
    public static final Type<TaskGuideSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "task_guide_sync")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, TaskGuideSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBoolean(payload.enabled);
                buf.writeResourceLocation(payload.taskId);
                buf.writeUtf(payload.json);
                buf.writeVarInt(payload.selectedEntry + 1);
            },
            buf -> new TaskGuideSyncPayload(
                    buf.readBoolean(),
                    buf.readResourceLocation(),
                    buf.readUtf(),
                    buf.readVarInt() - 1
            )
    );

    public static TaskGuideSyncPayload disabled(ResourceLocation taskId) {
        return new TaskGuideSyncPayload(false, taskId, "", -1);
    }

    public static TaskGuideSyncPayload disabled() {
        return disabled(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "none"));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
