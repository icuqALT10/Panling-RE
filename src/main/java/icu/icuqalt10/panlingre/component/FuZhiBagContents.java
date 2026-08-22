package icu.icuqalt10.panlingre.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Immutable contents of a five-slot talisman bag. Each slot stores at most 9999 items. */
public record FuZhiBagContents(List<Entry> entries) {
    public static final int SLOT_COUNT = 5;
    public static final long MAX_COUNT_PER_SLOT = 9999L;
    public static final FuZhiBagContents EMPTY = new FuZhiBagContents(List.of());

    public static final Codec<FuZhiBagContents> CODEC = Entry.CODEC.listOf()
            .xmap(FuZhiBagContents::new, FuZhiBagContents::entries);

    public static final StreamCodec<RegistryFriendlyByteBuf, FuZhiBagContents> STREAM_CODEC = StreamCodec.of(
            (buffer, contents) -> {
                buffer.writeVarInt(contents.entries.size());
                for (Entry entry : contents.entries) {
                    buffer.writeVarInt(entry.slot);
                    buffer.writeResourceLocation(entry.itemId);
                    buffer.writeVarLong(entry.count);
                }
            },
            buffer -> {
                int size = buffer.readVarInt();
                if (size < 0 || size > SLOT_COUNT) {
                    throw new IllegalArgumentException("Invalid talisman bag entry count: " + size);
                }
                List<Entry> entries = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    entries.add(new Entry(
                            buffer.readVarInt(),
                            buffer.readResourceLocation(),
                            buffer.readVarLong()
                    ));
                }
                return new FuZhiBagContents(entries);
            }
    );

    public FuZhiBagContents {
        entries = normalize(entries);
    }

    public Optional<Entry> get(int slot) {
        return entries.stream().filter(entry -> entry.slot == slot).findFirst();
    }

    public Optional<Entry> find(ResourceLocation itemId) {
        return entries.stream().filter(entry -> entry.itemId.equals(itemId)).findFirst();
    }

    public long count(int slot) {
        return get(slot).map(Entry::count).orElse(0L);
    }

    public FuZhiBagContents add(int preferredSlot, ResourceLocation itemId, long amount) {
        if (amount <= 0) return this;

        Entry existing = find(itemId).orElse(null);
        int targetSlot = existing != null ? existing.slot : preferredSlot;
        if (targetSlot < 0 || targetSlot >= SLOT_COUNT) return this;

        Entry occupant = get(targetSlot).orElse(null);
        if (occupant != null && !occupant.itemId.equals(itemId)) return this;

        long oldCount = existing == null ? 0L : existing.count;
        long newCount = cappedAdd(oldCount, amount);
        if (newCount == oldCount) return this;
        List<Entry> changed = new ArrayList<>(entries);
        changed.removeIf(entry -> entry.slot == targetSlot || entry.itemId.equals(itemId));
        changed.add(new Entry(targetSlot, itemId, newCount));
        return new FuZhiBagContents(changed);
    }

    public FuZhiBagContents remove(int slot, long amount) {
        if (amount <= 0) return this;
        Entry existing = get(slot).orElse(null);
        if (existing == null) return this;

        long remaining = existing.count - Math.min(existing.count, amount);
        List<Entry> changed = new ArrayList<>(entries);
        changed.removeIf(entry -> entry.slot == slot);
        if (remaining > 0) changed.add(new Entry(slot, existing.itemId, remaining));
        return new FuZhiBagContents(changed);
    }

    private static long cappedAdd(long left, long right) {
        if (left >= MAX_COUNT_PER_SLOT) return MAX_COUNT_PER_SLOT;
        return left + Math.min(right, MAX_COUNT_PER_SLOT - left);
    }

    private static List<Entry> normalize(List<Entry> input) {
        if (input == null || input.isEmpty()) return List.of();

        Map<Integer, Entry> bySlot = new HashMap<>();
        Map<ResourceLocation, Integer> slotByItem = new HashMap<>();
        for (Entry entry : input) {
            if (entry == null || entry.slot < 0 || entry.slot >= SLOT_COUNT || entry.count <= 0) continue;

            Integer existingSlot = slotByItem.get(entry.itemId);
            if (existingSlot != null) {
                Entry existing = bySlot.get(existingSlot);
                bySlot.put(existingSlot, new Entry(existingSlot, entry.itemId,
                        cappedAdd(existing.count, entry.count)));
            } else if (!bySlot.containsKey(entry.slot)) {
                Entry clean = new Entry(entry.slot, entry.itemId,
                        Math.min(entry.count, MAX_COUNT_PER_SLOT));
                bySlot.put(entry.slot, clean);
                slotByItem.put(entry.itemId, entry.slot);
            }
        }

        return bySlot.values().stream()
                .sorted(Comparator.comparingInt(Entry::slot))
                .limit(SLOT_COUNT)
                .toList();
    }

    public record Entry(int slot, ResourceLocation itemId, long count) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("slot").forGetter(Entry::slot),
                ResourceLocation.CODEC.fieldOf("item").forGetter(Entry::itemId),
                Codec.LONG.fieldOf("count").forGetter(Entry::count)
        ).apply(instance, Entry::new));
    }
}
