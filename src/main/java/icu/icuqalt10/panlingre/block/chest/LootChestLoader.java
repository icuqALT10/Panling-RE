package icu.icuqalt10.panlingre.block.chest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
        ResourceLocation path = ResourceLocation.fromNamespaceAndPath("panlingre", "loot_chest/" + lootId + ".json");
        try {
            var opt = serverLevel.getServer().getResourceManager().getResource(path);
            if (opt.isEmpty()) return entries;
            Reader reader = opt.get().openAsReader();
            JsonArray array = GsonHelper.parseArray(reader);
            reader.close();
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return entries;
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
