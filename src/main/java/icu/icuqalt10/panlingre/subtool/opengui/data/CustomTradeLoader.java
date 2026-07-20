// CustomTradeLoader.java
package icu.icuqalt10.panlingre.subtool.opengui.data;

import com.google.gson.*;
import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.io.InputStreamReader;
import java.util.Optional;

public class CustomTradeLoader {

    private static final Gson GSON = new GsonBuilder().create();
    private static final String FOLDER = "custom_trades/";

    /**
     * 返回 TradeData（offers + title），失败返回 null
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

                // 读取 title，不存在时使用空字符串（客户端会回退到默认值）
                Component title = json.has("title")
                        ? Component.Serializer.fromJson(json.get("title"), server.registryAccess())
                        : Component.translatable("gui.panlingre.custom_trade");

                MerchantOffers offers = parseOffers(json);
                return new TradeData(offers, title);
            }
        } catch (Exception e) {
            PanlingRE.LOGGER.error("[OpenGui] 读取文件失败: {}", path, e);
            return null;
        }
    }

    private static ResourceLocation resolve(String fileId) {
        if (fileId.contains(":")) return ResourceLocation.tryParse(fileId);
        return ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, fileId);
    }

    private static MerchantOffers parseOffers(JsonObject root) {
        MerchantOffers offers = new MerchantOffers();
        if (!root.has("offers") || !root.get("offers").isJsonArray()) {
            PanlingRE.LOGGER.error("[OpenGui] JSON 中缺少 'offers' 数组");
            return offers;
        }
        for (JsonElement el : root.getAsJsonArray("offers")) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            try {
                Optional<ItemCost> buy = readItemCost(o.getAsJsonObject("buy"));
                if (buy.isEmpty()) {
                    PanlingRE.LOGGER.warn("[OpenGui] 跳过 buy 解析失败的交易项: {}", o);
                    continue;
                }
                Optional<ItemCost> buyB = o.has("buyB")
                        ? readItemCost(o.getAsJsonObject("buyB"))
                        : Optional.empty();

                ItemStack sell = readStack(o.getAsJsonObject("sell"));
                if (sell.isEmpty()) {
                    PanlingRE.LOGGER.warn("[OpenGui] 跳过 sell 解析失败的交易项: {}", o);
                    continue;
                }

                int   maxUses    = o.has("maxUses")         ? o.get("maxUses").getAsInt()           : 2100000000;
                int   xp         = o.has("xp")              ? o.get("xp").getAsInt()                : 0;
                float priceMulti = o.has("priceMultiplier") ? o.get("priceMultiplier").getAsFloat() : 0;

                offers.add(new MerchantOffer(buy.get(), buyB, sell, maxUses, xp, priceMulti));

            } catch (Exception e) {
                PanlingRE.LOGGER.warn("[OpenGui] 跳过解析失败的交易项: {}", o, e);
            }
        }
        return offers;
    }

    private static Optional<ItemCost> readItemCost(JsonObject obj) {
        if (obj == null || !obj.has("item")) return Optional.empty();
        String itemId = obj.get("item").getAsString();
        int    count  = obj.has("count") ? obj.get("count").getAsInt() : 1;
        if (count <= 0 || itemId.equals("minecraft:air")) return Optional.empty();

        Optional<Item> item = BuiltInRegistries.ITEM
                .getOptional(ResourceLocation.tryParse(itemId));
        if (item.isEmpty()) {
            PanlingRE.LOGGER.warn("[OpenGui] 未知物品(ItemCost): {}", itemId);
            return Optional.empty();
        }
        return Optional.of(new ItemCost(item.get(), count));
    }

    private static ItemStack readStack(JsonObject obj) {
        if (obj == null || !obj.has("item")) return ItemStack.EMPTY;
        String itemId = obj.get("item").getAsString();
        int    count  = obj.has("count") ? obj.get("count").getAsInt() : 1;
        if (count <= 0 || itemId.equals("minecraft:air")) return ItemStack.EMPTY;

        return BuiltInRegistries.ITEM
                .getOptional(ResourceLocation.tryParse(itemId))
                .map(item -> new ItemStack(item, count))
                .orElseGet(() -> {
                    PanlingRE.LOGGER.warn("[OpenGui] 未知物品(ItemStack): {}", itemId);
                    return ItemStack.EMPTY;
                });
    }
}