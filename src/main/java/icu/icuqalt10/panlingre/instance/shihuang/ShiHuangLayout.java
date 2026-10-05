package icu.icuqalt10.panlingre.instance.shihuang;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Positions and door geometry were extracted from the user's updated native NBT. */
public record ShiHuangLayout(List<Gate> gates, List<Season> seasons,
        @SerializedName("season_lamps") int[][] seasonLamps,
        @SerializedName("path_floor") int[][] floor,
        @SerializedName("path_ceiling") int[][] ceiling,
        @SerializedName("path_plates") int[][] plates,
        @SerializedName("path_routes") int[][][] routes,
        @SerializedName("backup_regions") int[][] backups) {
    public record Gate(String id, int kind, int[] pos, int[] size) { }
    public record Season(String name, int[] button, int target, int[][] lamps, int[][] indicator) { }

    public static ShiHuangLayout load(ServerLevel level) {
        try (var stream = level.getServer().getResourceManager().open(
                ResourceLocation.fromNamespaceAndPath("panlingre", "shihuang_scene/layout.json"));
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, ShiHuangLayout.class);
        } catch (IOException e) { throw new IllegalStateException("Missing tomb scene layout", e); }
    }
}
