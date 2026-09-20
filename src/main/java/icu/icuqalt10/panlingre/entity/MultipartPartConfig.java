package icu.icuqalt10.panlingre.entity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Small reusable JSON reader/writer for multipart hitbox tuning. */
public final class MultipartPartConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private MultipartPartConfig() {}

    public static float[][] load(Path file, float[][] fallback) {
        if (!Files.isRegularFile(file)) return copy(fallback);
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if (root.has("use_external") && !root.get("use_external").getAsBoolean()) return copy(fallback);
            JsonArray values = root.getAsJsonArray("parts");
            if (values == null || values.size() != fallback.length) return copy(fallback);
            float[][] result = new float[fallback.length][];
            for (int i = 0; i < result.length; i++) {
                JsonArray row = values.get(i).getAsJsonArray();
                if (row.size() != 6 && row.size() != 9) return copy(fallback);
                result[i] = new float[row.size()];
                for (int j = 0; j < row.size(); j++) {
                    result[i][j] = row.get(j).getAsFloat();
                    if (!Float.isFinite(result[i][j]) || (j < 3 && result[i][j] <= 0)) return copy(fallback);
                }
            }
            return result;
        } catch (Exception ignored) {
            return copy(fallback);
        }
    }

    public static void save(Path file, float[][] bounds) throws IOException {
        Files.createDirectories(file.getParent());
        JsonObject root = new JsonObject();
        root.addProperty("use_external", true);
        JsonArray parts = new JsonArray();
        for (float[] row : bounds) {
            JsonArray values = new JsonArray();
            for (float value : row) values.add(value);
            parts.add(values);
        }
        root.add("parts", parts);
        Files.writeString(file, GSON.toJson(root));
    }

    private static float[][] copy(float[][] source) {
        float[][] result = new float[source.length][];
        for (int i = 0; i < source.length; i++) result[i] = source[i].clone();
        return result;
    }
}
