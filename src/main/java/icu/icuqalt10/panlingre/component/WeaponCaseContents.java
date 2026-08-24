package icu.icuqalt10.panlingre.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Immutable contents of a five-slot weapon case. Each slot stores one complete item stack. */
public record WeaponCaseContents(List<Entry> entries) {
    public static final int SLOT_COUNT = 5;
    public static final WeaponCaseContents EMPTY = new WeaponCaseContents(List.of());

    public static final Codec<WeaponCaseContents> CODEC = Entry.CODEC.listOf()
            .xmap(WeaponCaseContents::new, WeaponCaseContents::entries);

    public static final StreamCodec<RegistryFriendlyByteBuf, WeaponCaseContents> STREAM_CODEC = StreamCodec.of(
            (buffer, contents) -> {
                buffer.writeVarInt(contents.entries.size());
                for (Entry entry : contents.entries) {
                    buffer.writeVarInt(entry.slot);
                    ItemStack.STREAM_CODEC.encode(buffer, entry.stack);
                }
            },
            buffer -> {
                int size = buffer.readVarInt();
                if (size < 0 || size > SLOT_COUNT) {
                    throw new IllegalArgumentException("Invalid weapon case entry count: " + size);
                }
                List<Entry> entries = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    entries.add(new Entry(buffer.readVarInt(), ItemStack.STREAM_CODEC.decode(buffer)));
                }
                return new WeaponCaseContents(entries);
            }
    );

    public WeaponCaseContents {
        entries = normalize(entries);
    }

    public Optional<Entry> get(int slot) {
        return entries.stream().filter(entry -> entry.slot == slot).findFirst();
    }

    public WeaponCaseContents set(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT || stack.isEmpty()) return this;
        List<Entry> changed = new ArrayList<>(entries);
        changed.removeIf(entry -> entry.slot == slot);
        ItemStack stored = stack.copyWithCount(1);
        changed.add(new Entry(slot, stored));
        return new WeaponCaseContents(changed);
    }

    public WeaponCaseContents remove(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT || get(slot).isEmpty()) return this;
        List<Entry> changed = new ArrayList<>(entries);
        changed.removeIf(entry -> entry.slot == slot);
        return new WeaponCaseContents(changed);
    }

    private static List<Entry> normalize(List<Entry> input) {
        if (input == null || input.isEmpty()) return List.of();

        Set<Integer> occupiedSlots = new HashSet<>();
        List<Entry> clean = new ArrayList<>(SLOT_COUNT);
        for (Entry entry : input) {
            if (entry == null || entry.slot < 0 || entry.slot >= SLOT_COUNT
                    || entry.stack == null || entry.stack.isEmpty()
                    || !occupiedSlots.add(entry.slot)) {
                continue;
            }
            clean.add(new Entry(entry.slot, entry.stack.copyWithCount(1)));
        }
        clean.sort(Comparator.comparingInt(Entry::slot));
        return List.copyOf(clean);
    }

    public record Entry(int slot, ItemStack stack) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("slot").forGetter(Entry::slot),
                ItemStack.CODEC.fieldOf("stack").forGetter(Entry::stack)
        ).apply(instance, Entry::new));
    }
}
