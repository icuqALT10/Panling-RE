package icu.icuqalt10.panlingre.init;

import icu.icuqalt10.panlingre.client.gui.dztScreen;
import icu.icuqalt10.panlingre.client.gui.ldlScreen;
import icu.icuqalt10.panlingre.client.gui.zftScreen;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class ModScreens {
    @SubscribeEvent
    public static void register(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ldl_menu.get(), ldlScreen::new);
        event.register(ModMenus.zft_menu.get(), zftScreen::new);
        event.register(ModMenus.dzt_menu.get(), dztScreen::new);
    }
}
