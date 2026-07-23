package icu.icuqalt10.panlingre.task;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record TaskGuideData(
        Component title,
        GuideType type,
        Optional<EntityTarget> entity,
        Optional<PositionTarget> target
) {
    private static final Codec<TaskGuideData> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("title").forGetter(TaskGuideData::title),
            GuideType.CODEC.fieldOf("type").forGetter(TaskGuideData::type),
            EntityTarget.CODEC.optionalFieldOf("entity").forGetter(TaskGuideData::entity),
            PositionTarget.CODEC.optionalFieldOf("target").forGetter(TaskGuideData::target)
    ).apply(instance, TaskGuideData::new));

    public static final Codec<TaskGuideData> CODEC = RAW_CODEC.validate(TaskGuideData::validate);

    public static DataResult<TaskGuideData> parse(JsonElement json) {
        return CODEC.parse(JsonOps.INSTANCE, json);
    }

    private static DataResult<TaskGuideData> validate(TaskGuideData data) {
        if (data.type.hasPosition() && data.target.isEmpty()) {
            return DataResult.error(() -> "type " + data.type.serializedName + " requires target");
        }
        if (data.type.hasEntity() && data.entity.isEmpty()) {
            return DataResult.error(() -> "type " + data.type.serializedName + " requires entity");
        }
        return DataResult.success(data);
    }

    public enum GuideType {
        POS("pos", true, false),
        ENTITY("entity", false, true),
        POS_AND_ENTITY("pos_and_entity", true, true);

        public static final Codec<GuideType> CODEC = Codec.STRING.comapFlatMap(
                name -> {
                    for (GuideType value : values()) {
                        if (value.serializedName.equals(name)) {
                            return DataResult.success(value);
                        }
                    }
                    return DataResult.error(() -> "Unknown task guide type: " + name);
                },
                value -> value.serializedName
        );

        private final String serializedName;
        private final boolean position;
        private final boolean entity;

        GuideType(String serializedName, boolean position, boolean entity) {
            this.serializedName = serializedName;
            this.position = position;
            this.entity = entity;
        }

        public boolean hasPosition() {
            return position;
        }

        public boolean hasEntity() {
            return entity;
        }
    }

    public record EntityTarget(
            List<ResourceLocation> type,
            Optional<String> nbt,
            Optional<PositionCondition> pos,
            Optional<String> color
    ) {
        private static final Codec<List<ResourceLocation>> TYPE_CODEC = Codec.either(
                ResourceLocation.CODEC,
                ResourceLocation.CODEC.listOf()
        ).xmap(
                either -> either.map(List::of, list -> list),
                list -> list.size() == 1 ? Either.left(list.getFirst()) : Either.right(list)
        ).validate(list -> list.isEmpty()
                ? DataResult.error(() -> "entity.type cannot be empty")
                : DataResult.success(list));

        private static final Codec<EntityTarget> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                TYPE_CODEC.fieldOf("type").forGetter(EntityTarget::type),
                Codec.STRING.optionalFieldOf("nbt").forGetter(EntityTarget::nbt),
                PositionCondition.CODEC.optionalFieldOf("pos").forGetter(EntityTarget::pos),
                Codec.STRING.optionalFieldOf("color").forGetter(EntityTarget::color)
        ).apply(instance, EntityTarget::new));

        public static final Codec<EntityTarget> CODEC = RAW_CODEC.validate(EntityTarget::validate);

        private static DataResult<EntityTarget> validate(EntityTarget target) {
            if (target.color.isPresent() && parseColor(target.color.get()).isEmpty()) {
                return DataResult.error(() -> "entity.color must be #RRGGBB or RRGGBB");
            }
            return DataResult.success(target);
        }

        public int outlineColor() {
            return color.flatMap(EntityTarget::parseColor).orElse(0xE8C96A);
        }

        private static Optional<Integer> parseColor(String value) {
            String hex = value.startsWith("#") ? value.substring(1) : value;
            if (hex.length() != 6) {
                return Optional.empty();
            }
            try {
                return Optional.of(Integer.parseInt(hex, 16));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
    }

    public record PositionTarget(ExactPosition pos) {
        public static final Codec<PositionTarget> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExactPosition.CODEC.fieldOf("pos").forGetter(PositionTarget::pos)
        ).apply(instance, PositionTarget::new));
    }

    public record ExactPosition(int x, int y, int z) {
        public static final Codec<ExactPosition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("x").forGetter(ExactPosition::x),
                Codec.INT.fieldOf("y").forGetter(ExactPosition::y),
                Codec.INT.fieldOf("z").forGetter(ExactPosition::z)
        ).apply(instance, ExactPosition::new));
    }

    public record PositionCondition(AxisCondition x, AxisCondition y, AxisCondition z) {
        public static final Codec<PositionCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                AxisCondition.CODEC.fieldOf("x").forGetter(PositionCondition::x),
                AxisCondition.CODEC.fieldOf("y").forGetter(PositionCondition::y),
                AxisCondition.CODEC.fieldOf("z").forGetter(PositionCondition::z)
        ).apply(instance, PositionCondition::new));

        public boolean matches(int x, int y, int z) {
            return this.x.matches(x) && this.y.matches(y) && this.z.matches(z);
        }
    }

    public record AxisCondition(Optional<Integer> fixed, Optional<Range> range) {
        public static final Codec<AxisCondition> CODEC = Codec.either(Codec.INT, Range.CODEC)
                .xmap(
                        either -> either.map(
                                value -> new AxisCondition(Optional.of(value), Optional.empty()),
                                value -> new AxisCondition(Optional.empty(), Optional.of(value))
                        ),
                        condition -> condition.fixed
                                .<Either<Integer, Range>>map(Either::left)
                                .orElseGet(() -> Either.right(condition.range.orElseThrow()))
                );

        public boolean matches(int value) {
            return fixed.map(integer -> integer == value)
                    .orElseGet(() -> range.orElseThrow().matches(value));
        }
    }

    public record Range(int min, int max) {
        private static final Codec<Range> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("min").forGetter(Range::min),
                Codec.INT.fieldOf("max").forGetter(Range::max)
        ).apply(instance, Range::new));

        public static final Codec<Range> CODEC = RAW_CODEC.validate(range -> range.min > range.max
                ? DataResult.error(() -> "position range min cannot be greater than max")
                : DataResult.success(range));

        public boolean matches(int value) {
            return value >= min && value <= max;
        }
    }
}
