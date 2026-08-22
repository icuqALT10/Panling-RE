package icu.icuqalt10.panlingre.init;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.world.inventory.FuZhiBagMenu;
import icu.icuqalt10.panlingre.world.inventory.dztMenu;
import icu.icuqalt10.panlingre.world.inventory.ldlMenu;
import icu.icuqalt10.panlingre.world.inventory.zftMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, PanlingRE.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ldlMenu>> ldl_menu =
            MENUS.register("ldl_menu", () -> IMenuTypeExtension.create(ldlMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<zftMenu>> zft_menu =
            MENUS.register("zft_menu", () -> IMenuTypeExtension.create(zftMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<dztMenu>> dzt_menu =
            MENUS.register("dzt_menu", () -> IMenuTypeExtension.create(dztMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FuZhiBagMenu>> fu_zhi_bag_menu =
            MENUS.register("fu_zhi_bag_menu", () -> IMenuTypeExtension.create(FuZhiBagMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
