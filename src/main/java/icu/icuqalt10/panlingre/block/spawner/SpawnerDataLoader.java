package icu.icuqalt10.panlingre.block.spawner;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpawnerDataLoader extends SimpleJsonResourceReloadListener {
    private static final Map<ResourceLocation, List<SpawnerConfig.Monster>> MONSTERS = new HashMap<>();
    private static final Map<ResourceLocation, SpawnerConfig> BLOCKS = new HashMap<>();
    private static int generation;

    public SpawnerDataLoader() {
        super(new Gson(), "spawner");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        MONSTERS.clear();
        BLOCKS.clear();
        files.forEach((id, json) -> {
            if (!id.getPath().startsWith("monster/")) return;
            try {
                if (!(jsonNbt(json, "") instanceof ListTag list)) throw new IllegalArgumentException("Expected a JSON list");
                MONSTERS.put(id, SpawnerConfig.parseMonsters(list));
            } catch (Exception exception) {
                PanlingRE.LOGGER.error("Invalid spawner monster list {}: {}", id, exception.getMessage());
            }
        });
        files.forEach((id, json) -> {
            if (!id.getPath().startsWith("block/")) return;
            try {
                if (!(jsonNbt(json, "") instanceof CompoundTag data)) throw new IllegalArgumentException("Expected a JSON object");
                BLOCKS.put(id, SpawnerConfig.parse(data));
            } catch (Exception exception) {
                PanlingRE.LOGGER.error("Invalid spawner block definition {}: {}", id, exception.getMessage());
            }
        });
        generation++;
        PanlingRE.LOGGER.info("Loaded {} spawner definitions and {} monster lists", BLOCKS.size(), MONSTERS.size());
    }

    public static int generation() {
        return generation;
    }

    public static SpawnerConfig resolve(Tag source) {
        if (source instanceof CompoundTag data) return SpawnerConfig.parse(data);
        if (source instanceof StringTag name) {
            ResourceLocation id = path(name.getAsString(), "block");
            SpawnerConfig config = BLOCKS.get(id);
            if (config == null) throw new IllegalArgumentException("Missing spawner definition " + id);
            return config;
        }
        throw new IllegalArgumentException("spawner_data must be a JSON path or compound");
    }

    static List<SpawnerConfig.Monster> monsters(Tag source) {
        if (source == null) return List.of();
        if (source instanceof ListTag list) return SpawnerConfig.parseMonsters(list);
        if (source instanceof StringTag name) {
            ResourceLocation id = path(name.getAsString(), "monster");
            List<SpawnerConfig.Monster> list = MONSTERS.get(id);
            if (list == null) throw new IllegalArgumentException("Missing monster list " + id);
            return list;
        }
        throw new IllegalArgumentException("spawn_monster must be a JSON path or list");
    }

    private static ResourceLocation path(String name, String folder) {
        ResourceLocation id = ResourceLocation.parse(name.contains(":") ? name : PanlingRE.MODID + ":" + name);
        String path = id.getPath();
        if (path.endsWith(".json")) path = path.substring(0, path.length() - 5);
        if (path.startsWith("spawner/")) path = path.substring(8);
        if (!path.startsWith(folder + "/")) path = folder + "/" + path;
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), path);
    }

    // JSON has no NBT numeric suffixes. Preserve entity fields which Minecraft reads as typed lists.
    private static Tag jsonNbt(JsonElement json, String key) {
        if (json.isJsonObject()) {
            CompoundTag result = new CompoundTag();
            json.getAsJsonObject().entrySet().forEach(entry -> result.put(entry.getKey(), jsonNbt(entry.getValue(), entry.getKey())));
            return result;
        }
        if (json.isJsonArray()) {
            var array = json.getAsJsonArray();
            if (key.equals("UUID")) {
                int[] values = new int[array.size()];
                for (int i = 0; i < values.length; i++) values[i] = array.get(i).getAsInt();
                return new IntArrayTag(values);
            }
            boolean floats = key.equals("ArmorDropChances") || key.equals("HandDropChances") || key.equals("Rotation");
            boolean doubles = key.equals("Pos") || key.equals("Motion");
            // Mixed integral/decimal JSON numbers still form one homogeneous NBT list.
            for (JsonElement value : array) {
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()
                        && jsonNbt(value, "") instanceof DoubleTag) doubles = true;
            }
            ListTag result = new ListTag();
            for (JsonElement value : array) {
                Tag entry = floats ? FloatTag.valueOf(value.getAsFloat())
                        : doubles ? DoubleTag.valueOf(value.getAsDouble()) : jsonNbt(value, "");
                if (!result.add(entry)) throw new IllegalArgumentException("NBT lists require a single element type");
            }
            return result;
        }
        if (!json.isJsonPrimitive()) throw new IllegalArgumentException("NBT cannot contain JSON null");
        var value = json.getAsJsonPrimitive();
        if (value.isBoolean()) return ByteTag.valueOf(value.getAsBoolean());
        if (value.isString()) return StringTag.valueOf(value.getAsString());
        try {
            return TagParser.parseTag("{value:" + value + "}").get("value");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
            throw new IllegalArgumentException("Invalid NBT number " + value, exception);
        }
    }
}
