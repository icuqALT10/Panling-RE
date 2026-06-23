package icu.icuqalt10.panlingre.subtool.opengui.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.trading.MerchantOffers;

public class OpenGuiClientHandler {

    /**
     * 此时原版 openMenu 流程已经打开了 MerchantScreen（containerId 真实有效）
     * 我们只需覆盖客户端显示的 offers 即可
     */
    public static void open(MerchantOffers offers, Component title) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof MerchantScreen merchantScreen)) return;

        // getMenu() 返回 MerchantMenu，其内部持有 ClientSideMerchant
        // setOffers 会刷新左侧交易列表的显示
        merchantScreen.getMenu().setOffers(offers);
    }
}