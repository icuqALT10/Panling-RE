package icu.icuqalt10.panlingre.task;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TaskGuideLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final Map<ResourceLocation, LoadedTask> TASKS = new HashMap<>();

    public TaskGuideLoader() {
        super(GSON, "task");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        TASKS.clear();
        objects.forEach((id, json) -> TaskGuideData.parse(json)
                .resultOrPartial(error -> PanlingRE.LOGGER.error("Failed to parse task guide {}: {}", id, error))
                .ifPresent(data -> compile(id, json, data)));
        PanlingRE.LOGGER.info("Loaded {} task guides", TASKS.size());
    }

    private static void compile(ResourceLocation id, JsonElement json, TaskGuideData data) {
        List<Optional<CompoundTag>> requiredNbts = new ArrayList<>(data.entries().size());
        for (int index = 0; index < data.entries().size(); index++) {
            TaskGuideData.Entry entry = data.entries().get(index);
            Optional<CompoundTag> requiredNbt = Optional.empty();
            if (entry.entity().isPresent() && entry.entity().get().nbt().isPresent()) {
                try {
                    requiredNbt = Optional.of(TagParser.parseTag(entry.entity().get().nbt().get()));
                } catch (Exception exception) {
                    PanlingRE.LOGGER.error(
                            "Failed to parse task guide NBT {} entries[{}]: {}",
                            id, index, exception.getMessage()
                    );
                    return;
                }
            }
            requiredNbts.add(requiredNbt);
        }
        TASKS.put(id, new LoadedTask(data, GSON.toJson(json), List.copyOf(requiredNbts)));
    }

    public static Optional<LoadedTask> get(ResourceLocation id) {
        return Optional.ofNullable(TASKS.get(id));
    }

    public static Collection<ResourceLocation> ids() {
        return TASKS.keySet();
    }

    public static Collection<String> paths() {
        return TASKS.keySet().stream().map(ResourceLocation::getPath).toList();
    }

    public record LoadedTask(TaskGuideData data, String json, List<Optional<CompoundTag>> requiredNbts) {
    }
}
