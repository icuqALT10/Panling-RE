package icu.icuqalt10.panlingre.task;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record TaskGuideState(Optional<ResourceLocation> activeTask) {
    public static final TaskGuideState EMPTY = new TaskGuideState(Optional.empty());

    public static final Codec<TaskGuideState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("active_task").forGetter(TaskGuideState::activeTask)
    ).apply(instance, TaskGuideState::new));
}
