package icu.icuqalt10.panlingre.compat.beyonddimensions;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

public final class BeyondDimensionsAccess {
    public static final String MOD_ID = "beyonddimensions";

    private static final String DATA_NAME = "panlingre_beyond_dimensions_access";
    private static final String STORAGE_MENU_TITLE = "menu.title.beyonddimensions.dimensionnetmenu";
    private static final String STORAGE_MENU_BASE_CLASS =
            "com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu";

    private BeyondDimensionsAccess() {
    }

    public static boolean isInstalled() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isEnabled(MinecraftServer server) {
        return !isInstalled() || data(server).enabled;
    }

    public static void setEnabled(MinecraftServer server, boolean enabled) {
        if (!isInstalled()) return;
        data(server).setEnabled(enabled);
    }

    public static boolean shouldBlock(MinecraftServer server, @Nullable MenuProvider provider) {
        return isInstalled() && !data(server).enabled && isStorageMenuProvider(provider);
    }

    public static int closeOpenStorageMenus(MinecraftServer server) {
        int closed = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isStorageMenu(player.containerMenu)) {
                player.closeContainer();
                closed++;
            }
        }
        return closed;
    }

    private static boolean isStorageMenuProvider(@Nullable MenuProvider provider) {
        if (provider == null) return false;

        Component title = provider.getDisplayName();
        return title != null
                && title.getContents() instanceof TranslatableContents contents
                && STORAGE_MENU_TITLE.equals(contents.getKey());
    }

    private static boolean isStorageMenu(AbstractContainerMenu menu) {
        for (Class<?> type = menu.getClass(); type != null; type = type.getSuperclass()) {
            if (STORAGE_MENU_BASE_CLASS.equals(type.getName())) return true;
        }
        return false;
    }

    private static AccessData data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(AccessData.FACTORY, DATA_NAME);
    }

    private static final class AccessData extends SavedData {
        private static final String ENABLED_TAG = "Enabled";
        private static final Factory<AccessData> FACTORY = new Factory<>(AccessData::new, AccessData::load);

        private boolean enabled = true;

        private void setEnabled(boolean enabled) {
            if (this.enabled == enabled) return;
            this.enabled = enabled;
            setDirty();
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putBoolean(ENABLED_TAG, enabled);
            return tag;
        }

        private static AccessData load(CompoundTag tag, HolderLookup.Provider registries) {
            AccessData data = new AccessData();
            if (tag.contains(ENABLED_TAG)) {
                data.enabled = tag.getBoolean(ENABLED_TAG);
            }
            return data;
        }
    }
}
