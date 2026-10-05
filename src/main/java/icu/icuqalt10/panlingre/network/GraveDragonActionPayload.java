package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** One activation starts both the animation and its local visual timeline. */
public record GraveDragonActionPayload(int entityId, String action, long sequence, long startTick,
                                       long seed, List<Vec3> points,
                                       String blendFrom, double blendFromSeconds) implements CustomPacketPayload {
    private static final int MAX_POINTS = 33;
    public static final Type<GraveDragonActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "grave_dragon_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GraveDragonActionPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
                if (payload.points.size() > MAX_POINTS)
                    throw new IllegalArgumentException("Invalid dragon action points: " + payload.points.size());
                buffer.writeVarInt(payload.entityId);
                buffer.writeUtf(payload.action, 80);
                buffer.writeLong(payload.sequence);
                buffer.writeLong(payload.startTick);
                buffer.writeLong(payload.seed);
                buffer.writeVarInt(payload.points.size());
                for (Vec3 point : payload.points) {
                    buffer.writeDouble(point.x);
                    buffer.writeDouble(point.y);
                    buffer.writeDouble(point.z);
                }
                buffer.writeUtf(payload.blendFrom, 80);
                buffer.writeDouble(payload.blendFromSeconds);
            }, buffer -> {
                int id = buffer.readVarInt();
                String action = buffer.readUtf(80);
                long sequence = buffer.readLong();
                long start = buffer.readLong();
                long seed = buffer.readLong();
                int count = buffer.readVarInt();
                if (count < 0 || count > MAX_POINTS) throw new IllegalArgumentException("Invalid dragon action points: " + count);
                List<Vec3> points = new ArrayList<>(count);
                for (int i = 0; i < count; i++) points.add(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
                return new GraveDragonActionPayload(id, action, sequence, start, seed,
                        List.copyOf(points), buffer.readUtf(80), buffer.readDouble());
            });

    public static void handle(GraveDragonActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> icu.icuqalt10.panlingre.client.GraveDragonEffects.activate(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
