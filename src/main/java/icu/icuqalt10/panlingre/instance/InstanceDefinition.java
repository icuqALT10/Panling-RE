package icu.icuqalt10.panlingre.instance;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public record InstanceDefinition(
        ResourceLocation controller,
        ResourceLocation template,
        Vec3 centerPos,
        Vec3 playerSpawnPos,
        Rotation rotation,
        double safeDistance,
        PlayerLimit playerLimitCount,
        int spacing,
        int regionDepth,
        int prewarmSlots,
        int arenaVersion,
        ResourceLocation success,
        Optional<ResourceLocation> failure,
        Optional<ResourceLocation> intoFunction
) {
    public record Rotation(float yaw, float pitch) {}
    public record PlayerLimit(int min, int max) {
        public PlayerLimit {
            if (min < 1 || max < min)
                throw new IllegalArgumentException("player_limit_count requires 1 <= min <= max");
        }
    }

    public static InstanceDefinition parse(JsonObject json) {
        int spacing = json.get("spacing").getAsInt();
        int regionDepth = json.has("region_depth") ? json.get("region_depth").getAsInt() : 1;
        int prewarmSlots = json.has("prewarm_slots") ? json.get("prewarm_slots").getAsInt() : 5;
        int arenaVersion = json.has("arena_version") ? json.get("arena_version").getAsInt() : 1;
        if (spacing != 512 || regionDepth < 1 || regionDepth > 2)
            throw new IllegalArgumentException("Instances require 512-block spacing and one or two regions in Z");
        if (prewarmSlots <= 0) throw new IllegalArgumentException("prewarm_slots must be positive");
        if (arenaVersion < 0) throw new IllegalArgumentException("arena_version cannot be negative");
        double safeDistance = json.has("safe_distance") ? json.get("safe_distance").getAsDouble() : 0;
        if (!Double.isFinite(safeDistance) || safeDistance < 0)
            throw new IllegalArgumentException("safe_distance must be a finite non-negative radius");
        Rotation rotation = new Rotation(0, 0);
        if (json.has("rotation")) {
            JsonArray values = json.getAsJsonArray("rotation");
            if (values == null || values.size() != 2)
                throw new IllegalArgumentException("rotation must contain [yaw, pitch]");
            float yaw = values.get(0).getAsFloat(), pitch = values.get(1).getAsFloat();
            if (!Float.isFinite(yaw) || !Float.isFinite(pitch) || pitch < -90 || pitch > 90)
                throw new IllegalArgumentException("rotation requires finite angles and pitch between -90 and 90");
            rotation = new Rotation(yaw, pitch);
        }
        PlayerLimit playerLimit = new PlayerLimit(1, 1);
        if (json.has("player_limit_count")) {
            JsonElement value = json.get("player_limit_count");
            if (value.isJsonObject()) {
                JsonObject range = value.getAsJsonObject();
                playerLimit = new PlayerLimit(count(range.get("min")), count(range.get("max")));
            } else playerLimit = new PlayerLimit(1, count(value));
        }

        return new InstanceDefinition(
                id(json, "controller"),
                id(json, "template"),
                vec3(json, "center_pos"),
                vec3(json, "player_spawn_pos"),
                rotation,
                safeDistance,
                playerLimit,
                spacing,
                regionDepth,
                prewarmSlots,
                arenaVersion,
                id(json, "success"),
                optionalId(json, "failure"),
                json.has("into_function") && json.get("into_function").isJsonPrimitive()
                        && json.get("into_function").getAsString().isBlank()
                        ? Optional.empty() : optionalId(json, "into_function")
        );
    }

    public Vec3 centerForSlot(int slot) {
        return centerPos.add((double) slot * spacing, 0.0, 0.0);
    }

    public Vec3 playerSpawnForSlot(int slot) {
        return playerSpawnPos.add((double) slot * spacing, 0.0, 0.0);
    }

    public boolean contains(int slot, Vec3 position) {
        Vec3 center = centerForSlot(slot);
        return position.x >= center.x - 256 && position.x < center.x + 256
                && position.z >= center.z - regionDepth * 256 && position.z < center.z + regionDepth * 256;
    }

    private static int count(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("player_limit_count and its min/max must be integers");
        try {
            return value.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("player_limit_count must use integers within the int range", exception);
        }
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
