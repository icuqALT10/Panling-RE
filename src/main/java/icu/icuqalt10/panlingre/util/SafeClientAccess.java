package icu.icuqalt10.panlingre.util;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

public class SafeClientAccess {
    public static boolean isShiftPressed() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return ClientInternal.checkShift();
        }
        return false;
    }

    private static class ClientInternal {
        private static boolean checkShift() {
            return net.minecraft.client.gui.screens.Screen.hasShiftDown();
        }
    }
}