package icu.icuqalt10.panlingre.looktip;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;

public class LookTipMatcher {

    public static boolean matchesEntity(Entity entity, LookTipData.EntityCondition condition) {
        if (!"entity".equals(condition.type())) {
            return false;
        }

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        boolean nameMatches = condition.name().stream()
                .anyMatch(name -> {
                    ResourceLocation conditionId = ResourceLocation.tryParse(name);
                    if (conditionId == null) {
                        conditionId = ResourceLocation.withDefaultNamespace(name);
                    }
                    return entityId.equals(conditionId);
                });

        if (!nameMatches) {
            return false;
        }

        // 检查位置条件
        if (condition.pos().isPresent()) {
            var pos = entity.blockPosition();
            if (!condition.pos().get().matches(pos.getX(), pos.getY(), pos.getZ())) {
                return false;
            }
        }

        if (condition.nbt().isPresent()) {
            String nbtString = condition.nbt().get();

            try {
                CompoundTag requiredNbt = TagParser.parseTag(nbtString);
                CompoundTag entityTag = new CompoundTag();
                entity.saveWithoutId(entityTag);

                return nbtContains(entityTag, requiredNbt);
            } catch (Exception e) {
                System.err.println("Failed to parse NBT: " + nbtString);
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    public static boolean matchesBlock(BlockState blockState, BlockEntity blockEntity, LookTipData.EntityCondition condition) {
        if (!"block".equals(condition.type())) {
            return false;
        }

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        boolean nameMatches = condition.name().stream()
                .anyMatch(name -> {
                    ResourceLocation conditionId = ResourceLocation.tryParse(name);
                    if (conditionId == null) {
                        conditionId = ResourceLocation.withDefaultNamespace(name);
                    }
                    return blockId.equals(conditionId);
                });

        if (!nameMatches) {
            return false;
        }

        // 检查位置条件
        if (condition.pos().isPresent() && blockEntity != null) {
            var pos = blockEntity.getBlockPos();
            if (!condition.pos().get().matches(pos.getX(), pos.getY(), pos.getZ())) {
                return false;
            }
        }

        if (condition.blockState().isPresent()) {
            Map<String, String> requiredStates = condition.blockState().get();
            for (Map.Entry<String, String> entry : requiredStates.entrySet()) {
                Property<?> property = blockState.getBlock().getStateDefinition().getProperty(entry.getKey());
                if (property == null) {
                    return false;
                }
                String actualValue = blockState.getValue(property).toString();
                if (!actualValue.equals(entry.getValue())) {
                    return false;
                }
            }
        }

        if (condition.nbt().isPresent()) {
            if (blockEntity == null) {
                return false;
            }

            String nbtString = condition.nbt().get();

            try {
                CompoundTag requiredNbt = TagParser.parseTag(nbtString);
                CompoundTag blockEntityTag = blockEntity.saveWithoutMetadata(blockEntity.getLevel().registryAccess());

                System.out.println("=== Block NBT Match Debug ===");
                System.out.println("Block: " + blockId);
                System.out.println("Required NBT: " + requiredNbt);
                System.out.println("Block Entity NBT: " + blockEntityTag);

                boolean result = nbtContains(blockEntityTag, requiredNbt);
                System.out.println("Match result: " + result);
                System.out.println("==============================");

                return result;
            } catch (Exception e) {
                System.err.println("Failed to parse NBT string: " + nbtString);
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    private static boolean nbtContains(CompoundTag actual, CompoundTag required) {
        for (String key : required.getAllKeys()) {
            if (!actual.contains(key)) {
                return false;
            }

            Tag actualTag = actual.get(key);
            Tag requiredTag = required.get(key);

            if (!nbtMatches(actualTag, requiredTag)) {
                return false;
            }
        }
        return true;
    }

    private static boolean nbtMatches(Tag actual, Tag required) {
        if (actual == null || required == null) {
            return actual == required;
        }

        if (actual.getId() != required.getId()) {
            return false;
        }

        if (actual instanceof CompoundTag actualCompound && required instanceof CompoundTag requiredCompound) {
            return nbtContains(actualCompound, requiredCompound);
        }

        if (actual instanceof ListTag actualList && required instanceof ListTag requiredList) {
            for (int i = 0; i < requiredList.size(); i++) {
                Tag requiredElement = requiredList.get(i);
                boolean found = false;

                for (int j = 0; j < actualList.size(); j++) {
                    Tag actualElement = actualList.get(j);
                    if (nbtMatches(actualElement, requiredElement)) {
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
