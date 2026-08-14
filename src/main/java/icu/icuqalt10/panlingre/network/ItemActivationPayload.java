package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Displays vanilla's totem activation overlay with an arbitrary item model. */
public record ItemActivationPayload(ItemStack stack) implements CustomPacketPayload {
    public static final Type<ItemActivationPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "item_activation")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemActivationPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.STREAM_CODEC, ItemActivationPayload::stack,
                    ItemActivationPayload::new
            );

    public static void handle(ItemActivationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().gameRenderer
                .displayItemActivation(payload.stack()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
