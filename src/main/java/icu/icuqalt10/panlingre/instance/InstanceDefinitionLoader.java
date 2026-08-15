package icu.icuqalt10.panlingre.instance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;

public final class InstanceDefinitionLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final Map<ResourceLocation, InstanceDefinition> DEFINITIONS = new LinkedHashMap<>();
    private static int generation;

    public InstanceDefinitionLoader() {
        super(GSON, "instance");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
        DEFINITIONS.clear();
        objects.forEach((id, element) -> {
            try {
                DEFINITIONS.put(id, InstanceDefinition.parse(element.getAsJsonObject()));
            } catch (Exception exception) {
                PanlingRE.LOGGER.error("Failed to load instance definition {}: {}", id, exception.getMessage());
            }
        });
        generation++;
        PanlingRE.LOGGER.info("Loaded {} instance definitions", DEFINITIONS.size());
    }

    public static Optional<InstanceDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(DEFINITIONS.get(id));
    }

    public static Collection<Map.Entry<ResourceLocation, InstanceDefinition>> entries() {
        return List.copyOf(DEFINITIONS.entrySet());
    }

    public static int generation() {
        return generation;
    }
}
