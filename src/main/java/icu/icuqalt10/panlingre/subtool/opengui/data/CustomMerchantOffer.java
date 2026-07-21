package icu.icuqalt10.panlingre.subtool.opengui.data;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.Optional;

/**
 * 扩展 MerchantOffer，保存 buy/buyB 的富组件 ItemStack，用于精确组件匹配。
 */
public class CustomMerchantOffer extends MerchantOffer {

    private final ItemStack enrichedCostA;
    private final ItemStack enrichedCostB;

    public CustomMerchantOffer(ItemCost costA, Optional<ItemCost> costB, ItemStack result,
                               int maxUses, int xp, float priceMultiplier,
                               ItemStack enrichedCostA, ItemStack enrichedCostB) {
        super(costA, costB, result, maxUses, xp, priceMultiplier);
        this.enrichedCostA = enrichedCostA;
        this.enrichedCostB = enrichedCostB;
    }

    public ItemStack getEnrichedCostA() {
        return enrichedCostA;
    }

    public ItemStack getEnrichedCostB() {
        return enrichedCostB;
    }
}
