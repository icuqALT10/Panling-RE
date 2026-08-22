package icu.icuqalt10.panlingre.client.compat.beyonddimensions;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.compat.beyonddimensions.BeyondDimensionsAccess;
import icu.icuqalt10.panlingre.compat.beyonddimensions.BeyondDimensionsMagnetCompat;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.lang.reflect.Field;

@EventBusSubscriber(modid = PanlingRE.MODID, value = Dist.CLIENT)
public final class BeyondDimensionsMagnetClientEvents {
    private static final String[] REMOVED_BUTTON_FIELDS = {
            "hopperItemModeButton",
            "hopperXpModeButton",
            "hopperNBTModeButton",
            "hopperFluidModeButton"
    };

    private static boolean reflectionFailureLogged;

    private BeyondDimensionsMagnetClientEvents() {
    }

    @SubscribeEvent
    public static void afterScreenInit(ScreenEvent.Init.Post event) {
        if (!BeyondDimensionsAccess.isInstalled()) return;

        Screen screen = event.getScreen();
        if (!BeyondDimensionsMagnetCompat.MAGNET_SCREEN_CLASS.equals(screen.getClass().getName())) return;

        for (String fieldName : REMOVED_BUTTON_FIELDS) {
            try {
                Field field = screen.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                if (field.get(screen) instanceof GuiEventListener listener) {
                    event.removeListener(listener);
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                if (!reflectionFailureLogged) {
                    reflectionFailureLogged = true;
                    PanlingRE.LOGGER.error("Failed to hide BeyondDimensions network magnet mode buttons", exception);
                }
            }
        }
    }
}
