package icu.icuqalt10.panlingre.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import icu.icuqalt10.panlingre.init.ModAttachments;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** Player permissions for the refined-element item series. */
public record YuansuData(boolean ys2, boolean ys3) {
    public YuansuData() {
        this(false, false);
    }

    public static final Codec<YuansuData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("ys2", false).forGetter(YuansuData::ys2),
            Codec.BOOL.optionalFieldOf("ys3", false).forGetter(YuansuData::ys3)
    ).apply(instance, YuansuData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, YuansuData> STREAM_CODEC = StreamCodec.of(
            (buffer, data) -> {
                buffer.writeBoolean(data.ys2());
                buffer.writeBoolean(data.ys3());
            },
            buffer -> new YuansuData(buffer.readBoolean(), buffer.readBoolean())
    );

    public boolean get(String series) {
        return switch (series) {
            case "ys2" -> this.ys2;
            case "ys3" -> this.ys3;
            default -> false;
        };
    }

    public YuansuData with(String series, boolean value) {
        return switch (series) {
            case "ys2" -> new YuansuData(value, this.ys3);
            case "ys3" -> new YuansuData(this.ys2, value);
            default -> this;
        };
    }

    public static boolean hasPermission(@Nullable Player player, String series) {
        return player != null && player.getData(ModAttachments.YUANSU.get()).get(series);
    }
}
