package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.entity.boss.ShiHuang.GraveDragon.GraveDragonFireField;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Send the cached footprint once, rather than tracing the floor on every client tick. */
public record GraveDragonFireFieldPayload(int entityId, long startTick, GraveDragonFireField field)
        implements CustomPacketPayload {
    public static final Type<GraveDragonFireFieldPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "grave_dragon_fire_field"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GraveDragonFireFieldPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
                buffer.writeVarInt(payload.entityId);
                buffer.writeLong(payload.startTick);
                buffer.writeBlockPos(payload.field.origin());
                buffer.writeVarInt(payload.field.cells().size());
                for (var cell : payload.field.cells()) {
                    buffer.writeShort((int)Math.floor(cell.x) - payload.field.origin().getX());
                    buffer.writeShort((int)Math.floor(cell.z) - payload.field.origin().getZ());
                    buffer.writeFloat((float)cell.y);
                }
            }, buffer -> {
                int entity = buffer.readVarInt();
                long start = buffer.readLong();
                var origin = buffer.readBlockPos();
                int count = buffer.readVarInt();
                if (count < 0 || count > 125629) throw new IllegalArgumentException("Invalid fire field size: " + count);
                var field = new GraveDragonFireField(origin);
                for (int i = 0; i < count; i++)
                    field.add(origin.getX() + buffer.readShort(), origin.getZ() + buffer.readShort(), buffer.readFloat());
                return new GraveDragonFireFieldPayload(entity, start, field);
            });

    public static void handle(GraveDragonFireFieldPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> icu.icuqalt10.panlingre.client.GraveDragonEffects.acceptFireField(payload));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
