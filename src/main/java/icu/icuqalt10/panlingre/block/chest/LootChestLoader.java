package icu.icuqalt10.panlingre.block.chest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LootChestLoader {

    public record LootEntry(ItemStack item, int weight, boolean specialItem, String command) {}

    public static List<LootEntry> load(String lootId, ServerLevel serverLevel) {
        List<LootEntry> entries = new ArrayList<>();
        ResourceLocation path = createPath(lootId);
        if (path == null) {
            PanlingRE.LOGGER.warn("Cannot load loot chest table: invalid lootTableId '{}'", lootId);
            return entries;
        }

        try {
            var opt = serverLevel.getServer().getResourceManager().getResource(path);
            if (opt.isEmpty()) {
                PanlingRE.LOGGER.warn("Loot chest table '{}' was not found (lootTableId='{}')", path, lootId);
                return entries;
            }

            PanlingRE.LOGGER.debug("Loading loot chest table '{}' from datapack '{}'", path, opt.get().sourcePackId());
            try (Reader reader = opt.get().openAsReader()) {
                JsonArray array = GsonHelper.parseArray(reader);
                for (JsonElement elem : array) {
                    JsonObject obj = elem.getAsJsonObject();
                    JsonObject itemObj = obj.getAsJsonObject("item");
                    int weight = obj.has("weight") ? obj.get("weight").getAsInt() : 1;
                    boolean special = obj.has("special_item") && obj.get("special_item").getAsBoolean();
                    String command = obj.has("command") ? obj.get("command").getAsString() : "";

                    String id = itemObj.get("id").getAsString();
                    int count = itemObj.has("count") ? itemObj.get("count").getAsInt() : 1;

                    CompoundTag itemTag = new CompoundTag();
                    itemTag.putString("id", id);
                    itemTag.putInt("count", count);
                    if (itemObj.has("components")) {
                        itemTag.put("components", jsonToTag(itemObj.get("components")));
                    }

                    ItemStack stack = ItemStack.parseOptional(serverLevel.registryAccess(), itemTag);
                    if (!stack.isEmpty()) {
                        entries.add(new LootEntry(stack, weight, special, command));
                    }
                }
            }
        } catch (Exception e) {
            PanlingRE.LOGGER.error("Failed to load loot chest table '{}' (lootTableId='{}')", path, lootId, e);
        }
        return entries;
    }

    private static ResourceLocation createPath(String lootId) {
        if (lootId == null) return null;

        String value = lootId.trim();
        if (value.isEmpty()) return null;
        if (value.endsWith(".json")) {
            value = value.substring(0, value.length() - ".json".length());
        }

        String namespace = PanlingRE.MODID;
        String idPath = value;
        int namespaceSeparator = value.indexOf(':');
        if (namespaceSeparator >= 0) {
            namespace = value.substring(0, namespaceSeparator);
            idPath = value.substring(namespaceSeparator + 1);
        }
        if (idPath.startsWith("loot_chest/")) {
            idPath = idPath.substring("loot_chest/".length());
        }

        return ResourceLocation.tryBuild(namespace, "loot_chest/" + idPath + ".json");
    }

    private static Tag jsonToTag(JsonElement element) {
        if (element.isJsonObject()) {
            CompoundTag tag = new CompoundTag();
            for (Map.Entry<String, JsonElement> e : element.getAsJsonObject().entrySet()) {
                tag.put(e.getKey(), jsonToTag(e.getValue()));
            }
            return tag;
        } else if (element.isJsonArray()) {
            ListTag list = new ListTag();
            for (JsonElement item : element.getAsJsonArray()) {
                list.add(jsonToTag(item));
            }
            return list;
        } else if (element.isJsonPrimitive()) {
            JsonPrimitive prim = element.getAsJsonPrimitive();
            if (prim.isBoolean()) {
                return ByteTag.valueOf(prim.getAsBoolean());
            } else if (prim.isNumber()) {
                Number num = prim.getAsNumber();
                if (num.doubleValue() == Math.floor(num.doubleValue()) && !Double.isInfinite(num.doubleValue())) {
                    return IntTag.valueOf(num.intValue());
                }
                return DoubleTag.valueOf(num.doubleValue());
            } else {
                return StringTag.valueOf(prim.getAsString());
            }
        }
        return StringTag.valueOf("");
    }
}
