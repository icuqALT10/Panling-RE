// CustomTradeLoader.java
package icu.icuqalt10.panlingre.subtool.opengui.data;

import com.google.gson.*;
import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffers;

import java.io.InputStreamReader;
import java.util.Map;
import java.util.Optional;

public class CustomTradeLoader {

    private static final Gson GSON = new GsonBuilder().create();
    private static final String FOLDER = "custom_trades/";

    /**
     * 从文件加载 TradeData，失败返回 null
     */
    public static TradeData load(MinecraftServer server, String fileId) {
        ResourceLocation id = resolve(fileId);
        if (id == null) {
            PanlingRE.LOGGER.error("[OpenGui] 无效的文件ID: {}", fileId);
            return null;
        }

        ResourceLocation path = ResourceLocation.fromNamespaceAndPath(
                id.getNamespace(), FOLDER + id.getPath() + ".json"
        );

        ResourceManager rm = server.getResourceManager();
        try {
            Optional<Resource> res = rm.getResource(path);
            if (res.isEmpty()) {
                PanlingRE.LOGGER.error("[OpenGui] 找不到文件: data/{}/custom_trades/{}.json",
                        id.getNamespace(), id.getPath());
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(res.get().open())) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                return parseTradeData(json, server.registryAccess());
            }
        } catch (Exception e) {
            PanlingRE.LOGGER.error("[OpenGui] 读取文件失败: {}", path, e);
            return null;
        }
    }

    /**
     * 从 JSON 字符串加载 TradeData，失败返回 null
     */
    public static TradeData loadFromJson(MinecraftServer server, String jsonStr) {
        try {
            JsonObject json = GSON.fromJson(jsonStr, JsonObject.class);
            return parseTradeData(json, server.registryAccess());
        } catch (Exception e) {
            PanlingRE.LOGGER.error("[OpenGui] 解析内联JSON失败", e);
            return null;
        }
    }

    private static TradeData parseTradeData(JsonObject json, RegistryAccess registryAccess) {
        Component title = json.has("title")
                ? Component.Serializer.fromJson(json.get("title"), registryAccess)
                : Component.translatable("gui.panlingre.custom_trade");

        MerchantOffers offers = parseOffers(json, registryAccess);
        return new TradeData(offers, title);
    }

    private static ResourceLocation resolve(String fileId) {
        if (fileId.contains(":")) return ResourceLocation.tryParse(fileId);
        return ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, fileId);
    }

    private static MerchantOffers parseOffers(JsonObject root, RegistryAccess registryAccess) {
        MerchantOffers offers = new MerchantOffers();
        if (!root.has("offers") || !root.get("offers").isJsonArray()) {
            PanlingRE.LOGGER.error("[OpenGui] JSON 中缺少 'offers' 数组");
            return offers;
        }
        for (JsonElement el : root.getAsJsonArray("offers")) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            try {
                JsonObject buyObj = o.getAsJsonObject("buy");
                ItemStack buyStack = readStack(buyObj, registryAccess);
                if (buyStack.isEmpty()) {
                    PanlingRE.LOGGER.warn("[OpenGui] 跳过 buy 解析失败的交易项: {}", o);
                    continue;
                }
                ItemCost buyCost = createCostFromStack(buyStack);

                ItemStack buyBStack = ItemStack.EMPTY;
                Optional<ItemCost> buyBCost = Optional.empty();
                if (o.has("buyB")) {
                    JsonObject buyBObj = o.getAsJsonObject("buyB");
                    buyBStack = readStack(buyBObj, registryAccess);
                    if (!buyBStack.isEmpty()) {
                        buyBCost = Optional.of(createCostFromStack(buyBStack));
                    }
                }

                ItemStack sell = readStack(o.getAsJsonObject("sell"), registryAccess);
                if (sell.isEmpty()) {
                    PanlingRE.LOGGER.warn("[OpenGui] 跳过 sell 解析失败的交易项: {}", o);
                    continue;
                }

                int   maxUses    = o.has("maxUses")         ? o.get("maxUses").getAsInt()           : 2100000000;
                int   xp         = o.has("xp")              ? o.get("xp").getAsInt()                : 0;
                float priceMulti = o.has("priceMultiplier") ? o.get("priceMultiplier").getAsFloat() : 0;

                offers.add(new CustomMerchantOffer(buyCost, buyBCost, sell, maxUses, xp, priceMulti,
                        buyStack, buyBStack));

            } catch (Exception e) {
                PanlingRE.LOGGER.warn("[OpenGui] 跳过解析失败的交易项: {}", o, e);
            }
        }
        return offers;
    }

    static ItemCost createCostFromStack(ItemStack stack) {
        // ItemCost is also the client's display/autofill source, so it must retain
        // the expected components. Exact equality is still enforced by
        // CustomMerchantOffer and CustomMerchantMenu on both sides.
        return new ItemCost(
                stack.getItem().builtInRegistryHolder(),
                stack.getCount(),
                DataComponentPredicate.allOf(stack.getComponents())
        );
    }

    private static ItemStack readStack(JsonObject obj, RegistryAccess registryAccess) {
        if (obj == null || !obj.has("item")) return ItemStack.EMPTY;
        String itemId = obj.get("item").getAsString();
        int    count  = obj.has("count") ? obj.get("count").getAsInt() : 1;
        if (count <= 0 || itemId.equals("minecraft:air")) return ItemStack.EMPTY;

        if (obj.has("components") && obj.get("components").isJsonObject()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", itemId);
            tag.putInt("count", count);
            tag.put("components", jsonToTag(obj.get("components")));
            return ItemStack.parseOptional(registryAccess, tag);
        }

        if (obj.has("nbt") && obj.get("nbt").isJsonObject()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", itemId);
            tag.putInt("count", count);
            tag.put("tag", jsonToTag(obj.get("nbt")));
            return ItemStack.parseOptional(registryAccess, tag);
        }

        return BuiltInRegistries.ITEM
                .getOptional(ResourceLocation.tryParse(itemId))
                .map(item -> new ItemStack(item, count))
                .orElseGet(() -> {
                    PanlingRE.LOGGER.warn("[OpenGui] 未知物品(ItemStack): {}", itemId);
                    return ItemStack.EMPTY;
                });
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
                if (num instanceof Integer || num instanceof Long || num instanceof Short || num instanceof Byte) {
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
