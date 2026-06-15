package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.BlessData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncBlessPayload(BlessData data) implements CustomPacketPayload {

    public static final Type<SyncBlessPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "sync_bless"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncBlessPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeJsonWithCodec(BlessData.CODEC, payload.data()),
            buf -> new SyncBlessPayload(buf.readJsonWithCodec(BlessData.CODEC))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(final SyncBlessPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            net.minecraft.world.entity.player.Player player = context.player();
            if (player != null) {
                player.setData(icu.icuqalt10.panlingre.init.ModAttachments.BLESS.get(), payload.data());
            }
        });
    }
}