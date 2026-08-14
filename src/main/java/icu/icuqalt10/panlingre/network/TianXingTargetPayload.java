package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** The sniper target selected locally by a player's crosshair. */
public record TianXingTargetPayload(int entityId) implements CustomPacketPayload {
    public static final Type<TianXingTargetPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "tian_xing_target"));
    public static final StreamCodec<ByteBuf, TianXingTargetPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TianXingTargetPayload::entityId,
            TianXingTargetPayload::new);

    private static final Map<UUID, LockedTarget> TARGETS = new ConcurrentHashMap<>();

    public static void handle(TianXingTargetPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (payload.entityId() < 0) {
                TARGETS.remove(player.getUUID());
            } else {
                TARGETS.put(player.getUUID(),
                        new LockedTarget(payload.entityId(), player.level().getGameTime()));
            }
        });
    }

    public static LockedTarget getRecent(ServerPlayer player) {
        LockedTarget target = TARGETS.get(player.getUUID());
        if (target == null || player.level().getGameTime() - target.gameTime() > 10L) {
            TARGETS.remove(player.getUUID());
            return null;
        }
        return target;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record LockedTarget(int entityId, long gameTime) {
    }
}
