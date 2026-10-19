package icu.icuqalt10.panlingre.attachment;

import com.mojang.serialization.Codec;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.item.RecipeLockedItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Immutable, saved and owner-synced item permissions, independent of the recipe book. */
public record ItemUnlockData(Set<ResourceLocation> items) {
    public static final ItemUnlockData EMPTY = new ItemUnlockData(Set.of());
    public static final Codec<ItemUnlockData> CODEC = ResourceLocation.CODEC.listOf()
            .xmap(ids -> new ItemUnlockData(Set.copyOf(ids)), data -> List.copyOf(data.items));
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemUnlockData> STREAM_CODEC = StreamCodec.of(
            (buffer, data) -> buffer.writeCollection(data.items, (buf, id) -> buf.writeResourceLocation(id)),
            buffer -> new ItemUnlockData(Set.copyOf(buffer.readList(buf -> buf.readResourceLocation()))));

    public ItemUnlockData {
        items = Set.copyOf(items);
    }

    public ItemUnlockData unlock(ResourceLocation id) {
        Set<ResourceLocation> updated = new HashSet<>(items);
        updated.add(id);
        return new ItemUnlockData(updated);
    }

    public ItemUnlockData lock(ResourceLocation id) {
        Set<ResourceLocation> updated = new HashSet<>(items);
        updated.remove(id);
        return new ItemUnlockData(updated);
    }

    public static boolean canUse(@Nullable Player player, ItemStack stack) {
        return canUse(player, stack.getItem());
    }

    public static boolean canUse(@Nullable Player player, Item item) {
        return !(item instanceof RecipeLockedItem) || player != null
                && player.getData(ModAttachments.ITEM_UNLOCKS).items.contains(BuiltInRegistries.ITEM.getKey(item));
    }
}
