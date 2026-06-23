package icu.icuqalt10.panlingre.subtool.opengui.network;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.subtool.opengui.client.OpenGuiClientHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

public record OpenVillagerGuiPacket(MerchantOffers offers, Component title) implements CustomPacketPayload {

    public static final Type<OpenVillagerGuiPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "open_villager_gui")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenVillagerGuiPacket> STREAM_CODEC =
            StreamCodec.of(OpenVillagerGuiPacket::encode, OpenVillagerGuiPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, OpenVillagerGuiPacket pkt) {
        ComponentSerialization.STREAM_CODEC.encode(buf, pkt.title());
        buf.writeInt(pkt.offers().size());
        for (MerchantOffer offer : pkt.offers()) {
            ItemStack costAStack = offer.getCostA();
            ItemCost  costA      = new ItemCost(costAStack.getItem(), costAStack.getCount());

            ItemStack          costBStack = offer.getCostB();
            Optional<ItemCost> costB      = costBStack.isEmpty()
                    ? Optional.empty()
                    : Optional.of(new ItemCost(costBStack.getItem(), costBStack.getCount()));

            ItemCost.STREAM_CODEC.encode(buf, costA);
            ByteBufCodecs.optional(ItemCost.STREAM_CODEC).encode(buf, costB);
            ItemStack.STREAM_CODEC.encode(buf, offer.getResult());
            buf.writeInt(offer.getUses());
            buf.writeInt(offer.getMaxUses());
            buf.writeInt(offer.getXp());
            buf.writeInt(offer.getSpecialPriceDiff());
            buf.writeFloat(offer.getPriceMultiplier());
            buf.writeInt(offer.getDemand());
        }
    }

    private static OpenVillagerGuiPacket decode(RegistryFriendlyByteBuf buf) {
        Component title = ComponentSerialization.STREAM_CODEC.decode(buf);
        int size = buf.readInt();
        MerchantOffers offers = new MerchantOffers();
        for (int i = 0; i < size; i++) {
            ItemCost           costA      = ItemCost.STREAM_CODEC.decode(buf);
            Optional<ItemCost> costB      = ByteBufCodecs.optional(ItemCost.STREAM_CODEC).decode(buf);
            ItemStack          result     = ItemStack.STREAM_CODEC.decode(buf);
            int                uses       = buf.readInt();
            int                maxUses    = buf.readInt();
            int                xp         = buf.readInt();
            int                specialPrc = buf.readInt();
            float              priceMulti = buf.readFloat();
            int                demand     = buf.readInt();

            MerchantOffer offer = new MerchantOffer(costA, costB, result, uses, maxUses, xp, priceMulti, demand);
            offer.addToSpecialPriceDiff(specialPrc);
            offers.add(offer);
        }
        return new OpenVillagerGuiPacket(offers, title);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> OpenGuiClientHandler.open(this.offers, this.title));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}