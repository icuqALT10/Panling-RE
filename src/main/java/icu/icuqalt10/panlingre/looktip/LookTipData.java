package icu.icuqalt10.panlingre.looktip;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record LookTipData(
        Component title,
        List<EntityCondition> entities
) {
    public static final Codec<LookTipData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ComponentSerialization.CODEC.fieldOf("title").forGetter(LookTipData::title),
                    EntityCondition.CODEC.listOf().fieldOf("entities").forGetter(LookTipData::entities)
            ).apply(instance, LookTipData::new)
    );

    public record EntityCondition(
            String type,
            List<String> name,
            Optional<String> nbt,
            Optional<Map<String, String>> blockState,
            Optional<PosCondition> pos
    ) {
        private static final Codec<List<String>> NAME_CODEC = Codec.either(
                Codec.STRING,
                Codec.STRING.listOf()
        ).xmap(
                either -> either.map(List::of, list -> list),
                list -> list.size() == 1 ?
                        com.mojang.datafixers.util.Either.left(list.get(0)) :
                        com.mojang.datafixers.util.Either.right(list)
        );

        public static final Codec<EntityCondition> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("type").forGetter(EntityCondition::type),
                        NAME_CODEC.fieldOf("name").forGetter(EntityCondition::name),
                        Codec.STRING.optionalFieldOf("nbt").forGetter(EntityCondition::nbt),
                        Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("block_state").forGetter(EntityCondition::blockState),
                        PosCondition.CODEC.optionalFieldOf("pos").forGetter(EntityCondition::pos)
                ).apply(instance, EntityCondition::new)
        );
    }

    // 位置条件
    public record PosCondition(
            AxisCondition x,
            AxisCondition y,
            AxisCondition z
    ) {
        public static final Codec<PosCondition> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        AxisCondition.CODEC.fieldOf("x").forGetter(PosCondition::x),
                        AxisCondition.CODEC.fieldOf("y").forGetter(PosCondition::y),
                        AxisCondition.CODEC.fieldOf("z").forGetter(PosCondition::z)
                ).apply(instance, PosCondition::new)
        );

        public boolean matches(int x, int y, int z) {
            return this.x.matches(x) && this.y.matches(y) && this.z.matches(z);
        }
    }

    // 轴条件（可以是固定值或范围）
    public record AxisCondition(
            Optional<Integer> fixed,
            Optional<Integer> min,
            Optional<Integer> max
    ) {
        // 支持两种格式：直接整数（固定值）或对象（范围）
        public static final Codec<AxisCondition> CODEC = Codec.either(
                Codec.INT,  // 直接是整数 -> 固定值
                RecordCodecBuilder.<AxisCondition>create(instance ->
                        instance.group(
                                Codec.INT.optionalFieldOf("min").forGetter(AxisCondition::min),
                                Codec.INT.optionalFieldOf("max").forGetter(AxisCondition::max)
                        ).apply(instance, (min, max) -> new AxisCondition(Optional.empty(), min, max))
                )  // 对象 -> 范围
        ).xmap(
                either -> either.map(
                        fixedValue -> new AxisCondition(Optional.of(fixedValue), Optional.empty(), Optional.empty()),
                        rangeCondition -> rangeCondition
                ),
                condition -> {
                    if (condition.fixed.isPresent()) {
                        return com.mojang.datafixers.util.Either.left(condition.fixed.get());
                    } else {
                        return com.mojang.datafixers.util.Either.right(condition);
                    }
                }
        );

        public boolean matches(int value) {
            if (fixed.isPresent()) {
                return value == fixed.get();
            }
            boolean minOk = min.isEmpty() || value >= min.get();
            boolean maxOk = max.isEmpty() || value <= max.get();
            return minOk && maxOk;
        }
    }
}
