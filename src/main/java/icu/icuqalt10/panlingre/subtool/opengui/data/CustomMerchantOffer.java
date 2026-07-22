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
        this(costA, costB, result, 0, maxUses, xp, priceMultiplier, 0, enrichedCostA, enrichedCostB);
    }

    public CustomMerchantOffer(ItemCost costA, Optional<ItemCost> costB, ItemStack result,
                               int uses, int maxUses, int xp, float priceMultiplier, int demand,
                               ItemStack enrichedCostA, ItemStack enrichedCostB) {
        super(costA, costB, result, uses, maxUses, xp, priceMultiplier, demand);
        this.enrichedCostA = enrichedCostA;
        this.enrichedCostB = enrichedCostB;
    }

    public ItemStack getEnrichedCostA() {
        return enrichedCostA;
    }

    public ItemStack getEnrichedCostB() {
        return enrichedCostB;
    }

    @Override
    public boolean satisfiedBy(ItemStack stackA, ItemStack stackB) {
        if (!matchesExactCost(stackA, enrichedCostA, getCostA().getCount())) {
            return false;
        }
        return enrichedCostB.isEmpty()
                ? stackB.isEmpty()
                : matchesExactCost(stackB, enrichedCostB, getCostB().getCount());
    }

    private static boolean matchesExactCost(ItemStack actual, ItemStack expected, int requiredCount) {
        return ItemStack.isSameItemSameComponents(actual, expected)
                && actual.getCount() >= requiredCount;
    }
}
