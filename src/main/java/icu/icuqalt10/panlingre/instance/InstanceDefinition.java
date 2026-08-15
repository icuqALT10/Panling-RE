package icu.icuqalt10.panlingre.instance;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public record InstanceDefinition(
        ResourceLocation controller,
        ResourceLocation template,
        Vec3 centerPos,
        Vec3 playerSpawnPos,
        int spacing,
        int prewarmSlots,
        int arenaVersion,
        ResourceLocation success,
        Optional<ResourceLocation> failure
) {
    public static InstanceDefinition parse(JsonObject json) {
        int spacing = json.get("spacing").getAsInt();
        int prewarmSlots = json.has("prewarm_slots") ? json.get("prewarm_slots").getAsInt() : 5;
        int arenaVersion = json.has("arena_version") ? json.get("arena_version").getAsInt() : 1;
        if (spacing <= 0) throw new IllegalArgumentException("spacing must be positive");
        if (prewarmSlots <= 0) throw new IllegalArgumentException("prewarm_slots must be positive");
        if (arenaVersion < 0) throw new IllegalArgumentException("arena_version cannot be negative");

        return new InstanceDefinition(
                id(json, "controller"),
                id(json, "template"),
                vec3(json, "center_pos"),
                vec3(json, "player_spawn_pos"),
                spacing,
                prewarmSlots,
                arenaVersion,
                id(json, "success"),
                optionalId(json, "failure")
        );
    }

    public Vec3 centerForSlot(int slot) {
        return centerPos.add((double) slot * spacing, 0.0, 0.0);
    }

    public Vec3 playerSpawnForSlot(int slot) {
        return playerSpawnPos.add((double) slot * spacing, 0.0, 0.0);
    }

    private static ResourceLocation id(JsonObject json, String field) {
        ResourceLocation id = ResourceLocation.tryParse(json.get(field).getAsString());
        if (id == null) throw new IllegalArgumentException("Invalid resource location in " + field);
        return id;
    }

    private static Optional<ResourceLocation> optionalId(JsonObject json, String field) {
        if (!json.has(field) || json.get(field).isJsonNull()) return Optional.empty();
        return Optional.of(id(json, field));
    }

    private static Vec3 vec3(JsonObject json, String field) {
        JsonArray values = json.getAsJsonArray(field);
        if (values == null || values.size() != 3) {
            throw new IllegalArgumentException(field + " must contain exactly three numbers");
        }
        return new Vec3(values.get(0).getAsDouble(), values.get(1).getAsDouble(), values.get(2).getAsDouble());
    }

}
