package icu.icuqalt10.panlingre.subtool.opengui;

import icu.icuqalt10.panlingre.subtool.opengui.data.CustomMerchantOffer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.List;

/**
 * 覆写 MerchantMenu 的服务端匹配逻辑：
 * - 点击交易项：按 item ID 和完整 components 精确匹配
 * - 放入后精确匹配 id+count+components，完全一致才显示结果
 * - 单击交易、shift 一键换货均有精确匹配拦截
 */
public class CustomMerchantMenu extends MerchantMenu {

    private final MerchantOffers offers;
    private final Player player;
    private int selectedTradeIndex;

    public CustomMerchantMenu(int containerId, Inventory playerInv, MerchantOffers offers) {
        super(containerId, playerInv, new SimpleMerchant(offers));
        this.player = playerInv.player;
        this.offers = offers;
    }

    @Override
    public boolean stillValid(Player p) { return true; }

    @Override
    public void removed(Player p) {
        returnToInv(this.slots.get(0));
        returnToInv(this.slots.get(1));
        super.removed(p);
    }

    @Override
    public void setOffers(MerchantOffers newOffers) {
        super.setOffers(newOffers);
        this.offers.clear();
        this.offers.addAll(newOffers);
    }

    @Override
    public void setSelectionHint(int tradeIndex) {
        this.selectedTradeIndex = tradeIndex;
        super.setSelectionHint(tradeIndex);
    }

    @SuppressWarnings("unchecked")
    private List<CustomMerchantOffer> customOffers() {
        return (List<CustomMerchantOffer>) (List<?>) offers;
    }

    // ========== 点击左侧交易项：只填入与成本完全相同的物品 ==========

    @Override
    public void tryMoveItems(int tradeIndex) {
        List<CustomMerchantOffer> trades = customOffers();
        if (tradeIndex < 0 || tradeIndex >= trades.size()) return;
        setSelectionHint(tradeIndex);
        CustomMerchantOffer offer = trades.get(tradeIndex);

        Slot slotA = this.slots.get(0);
        Slot slotB = this.slots.get(1);
        Slot resultSlot = this.slots.get(2);

        returnToInv(slotA);
        returnToInv(slotB);
        resultSlot.set(ItemStack.EMPTY);

        fillMatchingItems(slotA, offer.getEnrichedCostA());
        if (!offer.getEnrichedCostB().isEmpty()) {
            fillMatchingItems(slotB, offer.getEnrichedCostB());
        }

        slotA.setChanged();
        slotB.setChanged();
        // slotsChanged -> correctResultSlot 会做精确匹配
    }

    // ========== 放入物品后：精确匹配决定结果槽 ==========

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        correctResultSlot();
    }

    private void correctResultSlot() {
        Slot resultSlot = this.slots.get(2);

        CustomMerchantOffer match = findTradableOffer();
        if (match != null) {
            if (!ItemStack.matches(resultSlot.getItem(), match.getResult())) {
                resultSlot.set(match.getResult().copy());
            }
        } else if (resultSlot.hasItem()) {
            resultSlot.set(ItemStack.EMPTY);
        }
    }

    // ========== 单击/Shift 交易：精确匹配拦截 ==========

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player p) {
        if (slotId == 2 && this.slots.get(2).hasItem()) {
            if (findTradableOffer() == null) {
                return;
            }
        }
        super.clicked(slotId, button, clickType, p);
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        if (index != 2) return super.quickMoveStack(p, index);

        Slot resultSlot = this.slots.get(2);
        if (!resultSlot.hasItem()) return ItemStack.EMPTY;

        Slot slotA = this.slots.get(0);
        Slot slotB = this.slots.get(1);
        CustomMerchantOffer offer = findTradableOffer();
        if (offer == null) return ItemStack.EMPTY;

        int needA = offer.getCostA().getCount();
        int needB = offer.getEnrichedCostB().isEmpty() ? 0 : offer.getCostB().getCount();
        int maxTrades = slotA.getItem().getCount() / Math.max(1, needA);
        if (needB > 0) maxTrades = Math.min(maxTrades, slotB.getItem().getCount() / Math.max(1, needB));
        if (maxTrades <= 0) return ItemStack.EMPTY;

        ItemStack firstResult = ItemStack.EMPTY;
        for (int i = 0; i < maxTrades; i++) {
            ItemStack result = offer.getResult().copy();
            if (!player.getInventory().add(result)) { player.drop(result, false); break; }
            slotA.getItem().shrink(needA);
            if (needB > 0) slotB.getItem().shrink(needB);
            if (firstResult.isEmpty()) firstResult = result;
        }
        slotA.setChanged();
        slotB.setChanged();
        correctResultSlot();
        return firstResult.isEmpty() ? ItemStack.EMPTY : firstResult;
    }

    // ========== 精确匹配 ==========

    private CustomMerchantOffer findTradableOffer() {
        List<CustomMerchantOffer> trades = customOffers();
        if (selectedTradeIndex >= 0 && selectedTradeIndex < trades.size()) {
            CustomMerchantOffer selectedOffer = trades.get(selectedTradeIndex);
            if (canTrade(selectedOffer)) {
                return selectedOffer;
            }
        }

        // Manual insertion may happen without selecting a row first. In that
        // case, use the first offer that is fully satisfied, not merely the
        // first offer with the same item/components.
        for (int i = 0; i < trades.size(); i++) {
            if (i == selectedTradeIndex) continue;
            CustomMerchantOffer offer = trades.get(i);
            if (canTrade(offer)) {
                return offer;
            }
        }
        return null;
    }

    private boolean canTrade(CustomMerchantOffer o) {
        ItemStack a = this.slots.get(0).getItem();
        ItemStack b = this.slots.get(1).getItem();
        if (!ItemStack.isSameItemSameComponents(a, o.getEnrichedCostA())) return false;
        if (a.getCount() < o.getCostA().getCount()) return false;
        if (!o.getEnrichedCostB().isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(b, o.getEnrichedCostB())) return false;
            if (b.getCount() < o.getCostB().getCount()) return false;
        } else if (!b.isEmpty()) {
            return false;
        }
        return true;
    }

    // ========== 填槽（完整 components 匹配，不限数量） ==========

    private void fillMatchingItems(Slot slot, ItemStack expected) {
        int maxStack = expected.getMaxStackSize(); // 物品自身堆叠上限（64）
        for (ItemStack stack : player.getInventory().items) {
            if (!ItemStack.isSameItemSameComponents(stack, expected)) continue;
            int canFit = maxStack - slot.getItem().getCount();
            if (canFit <= 0) return;
            int take = Math.min(canFit, stack.getCount());
            ItemStack taken = stack.split(take);
            if (slot.getItem().isEmpty()) slot.set(taken);
            else slot.getItem().grow(take);
        }
    }

    private void returnToInv(Slot slot) {
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return;
        slot.set(ItemStack.EMPTY);
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
}
