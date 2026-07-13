package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.FreezeData;
import icu.icuqalt10.panlingre.init.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncFreezeDataPayload(int entityId, boolean frozen, int duration) implements CustomPacketPayload {

    public static final Type<SyncFreezeDataPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "sync_freeze_data")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncFreezeDataPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SyncFreezeDataPayload::entityId,
                    ByteBufCodecs.BOOL, SyncFreezeDataPayload::frozen,
                    ByteBufCodecs.VAR_INT, SyncFreezeDataPayload::duration,
                    SyncFreezeDataPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncFreezeDataPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(payload.entityId());
            if (entity instanceof LivingEntity livingEntity) {
                FreezeData freezeData = livingEntity.getData(ModAttachments.FREEZE_DATA.get());
                freezeData.setFrozen(payload.frozen());
                freezeData.setDuration(payload.duration());
            }
        });
    }
}
