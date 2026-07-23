package icu.icuqalt10.panlingre.task;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class TaskGuideMatcher {
    private TaskGuideMatcher() {
    }

    public static boolean matchesClient(Entity entity, TaskGuideData.EntityTarget target) {
        if (!target.type().contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))) {
            return false;
        }
        if (target.pos().isPresent()) {
            var pos = entity.blockPosition();
            return target.pos().get().matches(pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    public static boolean matchesServer(
            Entity entity,
            TaskGuideData.EntityTarget target,
            Optional<CompoundTag> requiredNbt
    ) {
        if (!matchesClient(entity, target)) {
            return false;
        }
        if (requiredNbt.isEmpty()) {
            return true;
        }
        CompoundTag actual = new CompoundTag();
        entity.saveWithoutId(actual);
        return contains(actual, requiredNbt.get());
    }

    private static boolean contains(CompoundTag actual, CompoundTag required) {
        for (String key : required.getAllKeys()) {
            if (!matches(actual.get(key), required.get(key))) {
                return false;
            }
        }
        return true;
    }

    private static boolean matches(Tag actual, Tag required) {
        if (actual == null || required == null || actual.getId() != required.getId()) {
            return false;
        }
        if (actual instanceof CompoundTag actualCompound && required instanceof CompoundTag requiredCompound) {
            return contains(actualCompound, requiredCompound);
        }
        if (actual instanceof ListTag actualList && required instanceof ListTag requiredList) {
            for (Tag requiredElement : requiredList) {
                boolean found = false;
                for (Tag actualElement : actualList) {
                    if (matches(actualElement, requiredElement)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
            return true;
        }
        return actual.equals(required);
    }
}
