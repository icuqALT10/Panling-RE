package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.client.renderer.FreezeEffectRenderer;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** Synchronizes only the visual marker for PanlingRE's freeze effect. */
public record FreezeSyncPayload(UUID entityId, boolean frozen) implements CustomPacketPayload {
    public static final Type<FreezeSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "freeze_sync")
    );

    public static final StreamCodec<ByteBuf, FreezeSyncPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, FreezeSyncPayload::entityId,
            ByteBufCodecs.BOOL, FreezeSyncPayload::frozen,
            FreezeSyncPayload::new
    );

    public static void handle(FreezeSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> FreezeEffectRenderer.handleFreezeSync(payload.entityId(), payload.frozen()));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
