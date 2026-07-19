package icu.icuqalt10.panlingre.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;

public class SafeClientAccess {
    public static boolean isShiftPressed() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return ClientInternal.checkShift();
        }
        return false;
    }

    @Nullable
    public static Player getClientPlayer() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return ClientInternal.getPlayer();
        }
        return null;
    }

    private static class ClientInternal {
        private static boolean checkShift() {
            return Screen.hasShiftDown();
        }

        @Nullable
        private static Player getPlayer() {
            return Minecraft.getInstance().player;
        }
    }
}