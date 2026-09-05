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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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

    public static boolean isEnabled(ServerPlayer player) {
        return !isInstalled() || !data(player.getServer()).disabledPlayers.contains(player.getUUID());
    }

    public static void setEnabled(ServerPlayer player, boolean enabled) {
        if (!isInstalled()) return;
        data(player.getServer()).setEnabled(player.getUUID(), enabled);
    }

    public static boolean shouldBlock(ServerPlayer player, @Nullable MenuProvider provider) {
        return isInstalled()
                && data(player.getServer()).disabledPlayers.contains(player.getUUID())
                && isStorageMenuProvider(provider);
    }

    public static int closeOpenStorageMenu(ServerPlayer player) {
        if (!isStorageMenu(player.containerMenu)) return 0;
        player.closeContainer();
        return 1;
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
        private static final String DISABLED_PLAYERS_TAG = "DisabledPlayers";
        private static final Factory<AccessData> FACTORY = new Factory<>(AccessData::new, AccessData::load);

        private final Set<UUID> disabledPlayers = new HashSet<>();

        private void setEnabled(UUID playerId, boolean enabled) {
            boolean changed = enabled ? disabledPlayers.remove(playerId) : disabledPlayers.add(playerId);
            if (changed) setDirty();
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            CompoundTag disabledTag = new CompoundTag();
            for (UUID playerId : disabledPlayers) {
                disabledTag.putBoolean(playerId.toString(), true);
            }
            tag.put(DISABLED_PLAYERS_TAG, disabledTag);
            return tag;
        }

        private static AccessData load(CompoundTag tag, HolderLookup.Provider registries) {
            AccessData data = new AccessData();
            if (tag.contains(DISABLED_PLAYERS_TAG, CompoundTag.TAG_COMPOUND)) {
                CompoundTag disabledTag = tag.getCompound(DISABLED_PLAYERS_TAG);
                for (String key : disabledTag.getAllKeys()) {
                    try {
                        if (disabledTag.getBoolean(key)) data.disabledPlayers.add(UUID.fromString(key));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            return data;
        }
    }
}
