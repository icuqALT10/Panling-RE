package icu.icuqalt10.panlingre.data.ba_fang_yi;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 从 data/panlingre/ba_fang_yi/ 加载 JSON 配置
 */
public class BaFangYiLoader {

    private static final Gson GSON = new Gson();

    public static List<BaFangYiMajorEntry> loadAll(ServerLevel level) {
        ResourceManager rm = level.getServer().getResourceManager();
        Map<ResourceLocation, Resource> resources = rm.listResources("ba_fang_yi",
                loc -> loc.getPath().endsWith(".json") && loc.getNamespace().equals(PanlingRE.MODID));

        HolderLookup.Provider registryAccess = level.registryAccess();
        List<BaFangYiMajorEntry> entries = new ArrayList<>();

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                BaFangYiMajorEntry parsed = parseJson(json, registryAccess);
                if (parsed != null) {
                    entries.add(parsed);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return entries;
    }

    private static BaFangYiMajorEntry parseJson(JsonObject json, HolderLookup.Provider registryAccess) {
        Component title = parseTitle(json.get("title"), registryAccess);
        String id = getString(json, "id");
        String texture = getString(json, "texture");

        if (title == null || id == null) return null;

        List<BaFangYiSubEntry> poses = new ArrayList<>();
        if (json.has("poses") && json.get("poses").isJsonArray()) {
            JsonArray arr = json.getAsJsonArray("poses");
            for (JsonElement elem : arr) {
                if (elem.isJsonObject()) {
                    BaFangYiSubEntry sub = parseSubEntry(elem.getAsJsonObject(), registryAccess);
                    if (sub != null) poses.add(sub);
                }
            }
        }

        return new BaFangYiMajorEntry(title, id, texture != null ? texture : "", poses);
    }

    private static BaFangYiSubEntry parseSubEntry(JsonObject json, HolderLookup.Provider registryAccess) {
        Component title = parseTitle(json.get("title"), registryAccess);
        String id = getString(json, "id");
        String texture = getString(json, "texture");

        if (title == null || id == null) return null;

        double x = 0, y = 64, z = 0;
        if (json.has("pos") && json.get("pos").isJsonObject()) {
            JsonObject pos = json.getAsJsonObject("pos");
            x = getDouble(pos, "x", 0);
            y = getDouble(pos, "y", 64);
            z = getDouble(pos, "z", 0);
        }

        return new BaFangYiSubEntry(title, id, texture != null ? texture : "", x, y, z);
    }

    private static Component parseTitle(JsonElement element, HolderLookup.Provider registryAccess) {
        if (element == null) return null;
        return Component.Serializer.fromJson(element, registryAccess);
    }

    private static String getString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    private static double getDouble(JsonObject obj, String key, double defaultValue) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsDouble() : defaultValue;
    }
}
