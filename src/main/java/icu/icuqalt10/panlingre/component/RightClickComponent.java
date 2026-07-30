package icu.icuqalt10.panlingre.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record RightClickComponent(int usingTime, String command, Cooldown cooldown) {
    public static final Codec<RightClickComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, Integer.MAX_VALUE)
                    .optionalFieldOf("using_time", 1)
                    .forGetter(RightClickComponent::usingTime),
            Codec.STRING
                    .validate(command -> command.trim().isEmpty()
                            ? com.mojang.serialization.DataResult.error(() -> "command must not be empty")
                            : com.mojang.serialization.DataResult.success(command))
                    .fieldOf("command")
                    .forGetter(RightClickComponent::command),
            Cooldown.CODEC
                    .optionalFieldOf("cooldown", Cooldown.DEFAULT)
                    .forGetter(RightClickComponent::cooldown)
    ).apply(instance, RightClickComponent::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RightClickComponent> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, RightClickComponent::usingTime,
                    ByteBufCodecs.STRING_UTF8, RightClickComponent::command,
                    Cooldown.STREAM_CODEC, RightClickComponent::cooldown,
                    RightClickComponent::new
            );

    public record Cooldown(int time, boolean cdRemove) {
        public static final Cooldown DEFAULT = new Cooldown(0, true);

        public static final Codec<Cooldown> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(0, Integer.MAX_VALUE)
                        .optionalFieldOf("time", 0)
                        .forGetter(Cooldown::time),
                Codec.BOOL
                        .optionalFieldOf("cd_remove", true)
                        .forGetter(Cooldown::cdRemove)
        ).apply(instance, Cooldown::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Cooldown> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, Cooldown::time,
                        ByteBufCodecs.BOOL, Cooldown::cdRemove,
                        Cooldown::new
                );
    }
}
