package icu.icuqalt10.panlingre.subtool.opengui.data;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.trading.MerchantOffers;

public record TradeData(MerchantOffers offers, Component title) {}