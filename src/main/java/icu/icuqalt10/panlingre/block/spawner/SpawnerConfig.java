package icu.icuqalt10.panlingre.block.spawner;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

public record SpawnerConfig(IntRange count, IntRange speed, List<Monster> monsters, SpawnLimit maxCount) {
    public record IntRange(int min, int max) {
        public int sample(RandomSource random) {
            if (min == max) return min;
            long size = (long) max - min + 1;
            return size == 2147483648L ? random.nextInt() >>> 1 : min + random.nextInt((int) size);
        }

        static IntRange parse(CompoundTag data, String key, int fallback, int minimum) {
            Tag value = data.get(key);
            int min = fallback;
            int max = fallback;
            if (value instanceof IntTag number) {
                min = max = number.getAsInt();
            } else if (value instanceof CompoundTag range) {
                if (!range.contains("min", Tag.TAG_INT) || !range.contains("max", Tag.TAG_INT)) {
                    throw new IllegalArgumentException(key + " requires integer min and max");
                }
                min = range.getInt("min");
                max = range.getInt("max");
            } else if (value != null) {
                throw new IllegalArgumentException(key + " must be an integer or {min,max}");
            }
            if (min < minimum || max < min) throw new IllegalArgumentException("Invalid " + key + " range");
            return new IntRange(min, max);
        }
    }

    public record Monster(ResourceLocation id, CompoundTag nbt, int weight) {}

    public record SpawnLimit(int limit, double distance, List<String> tags) {
        static SpawnLimit parse(Tag value) {
            if (value == null) return new SpawnLimit(9, 10, List.of("monster"));
            if (!(value instanceof CompoundTag data)
                    || !data.contains("limit", Tag.TAG_INT) || data.getInt("limit") < 0
                    || !data.contains("distance", Tag.TAG_ANY_NUMERIC)
                    || !Double.isFinite(data.getDouble("distance")) || data.getDouble("distance") < 0
                    || !(data.get("tags") instanceof ListTag tags)
                    || !tags.isEmpty() && tags.getElementType() != Tag.TAG_STRING) {
                throw new IllegalArgumentException("spawn_max_count requires non-negative integer limit, non-negative distance and string list tags");
            }
            return new SpawnLimit(data.getInt("limit"), data.getDouble("distance"),
                    tags.stream().map(Tag::getAsString).toList());
        }
    }

    public static SpawnerConfig parse(CompoundTag data) {
        return new SpawnerConfig(IntRange.parse(data, "spawn_count", 1, 0),
                IntRange.parse(data, "spawn_speed", 20, 1), SpawnerDataLoader.monsters(data.get("spawn_monster")),
                SpawnLimit.parse(data.get("spawn_max_count")));
    }

    static List<Monster> parseMonsters(ListTag list) {
        List<Monster> result = new ArrayList<>();
        long total = 0;
        for (Tag value : list) {
            if (!(value instanceof CompoundTag entry)) throw new IllegalArgumentException("Monster entries must be compounds");
            ResourceLocation id = ResourceLocation.parse(entry.getString("id"));
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) throw new IllegalArgumentException("Unknown entity " + id);
            int weight = entry.contains("weight") ? entry.getInt("weight") : 1;
            if (entry.contains("weight") && !entry.contains("weight", Tag.TAG_INT) || weight < 0) {
                throw new IllegalArgumentException("Monster weight must be a non-negative integer");
            }
            if (entry.contains("nbt") && !entry.contains("nbt", Tag.TAG_COMPOUND)) {
                throw new IllegalArgumentException("Monster nbt must be a compound");
            }
            total += weight;
            result.add(new Monster(id, entry.getCompound("nbt").copy(), weight));
        }
        if (!list.isEmpty() && (total == 0 || total > Integer.MAX_VALUE)) {
            throw new IllegalArgumentException("Total monster weight must be 1..2147483647");
        }
        return List.copyOf(result);
    }

    public Monster choose(RandomSource random) {
        int total = 0;
        for (Monster monster : monsters) total += monster.weight();
        if (total == 0) return null;
        int roll = random.nextInt(total);
        for (Monster monster : monsters) {
            roll -= monster.weight();
            if (roll < 0) return monster;
        }
        throw new AssertionError("Unreachable weighted selection");
    }
}
