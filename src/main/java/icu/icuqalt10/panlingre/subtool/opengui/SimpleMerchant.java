package icu.icuqalt10.panlingre.subtool.opengui;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public class SimpleMerchant implements Merchant {

    private MerchantOffers offers;
    private Player tradingPlayer;

    public SimpleMerchant(MerchantOffers offers) {
        this.offers = offers;
    }

    @Override public void setTradingPlayer(Player player) { this.tradingPlayer = player; }
    @Override public Player getTradingPlayer() { return tradingPlayer; }
    @Override public MerchantOffers getOffers() { return offers; }
    @Override public void overrideOffers(MerchantOffers newOffers) { this.offers = newOffers; }
    @Override public void notifyTrade(MerchantOffer offer) { offer.increaseUses(); }
    @Override public void notifyTradeUpdated(ItemStack stack) {}
    @Override public int getVillagerXp() { return 0; }
    @Override public void overrideXp(int xp) {}
    @Override public boolean showProgressBar() { return true; }
    @Override public SoundEvent getNotifyTradeSound() { return SoundEvents.VILLAGER_YES; }
    @Override public boolean isClientSide() { return false; }
}