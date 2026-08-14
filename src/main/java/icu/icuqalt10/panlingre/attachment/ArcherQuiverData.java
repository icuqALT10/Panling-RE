package icu.icuqalt10.panlingre.attachment;

import com.mojang.serialization.Codec;
import icu.icuqalt10.panlingre.init.ModAttachments;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** Controls whether the player may activate Tian Xing Jian. */
public record ArcherQuiverData(boolean enabled) {
    public ArcherQuiverData() {
        this(false);
    }

    public static final Codec<ArcherQuiverData> CODEC = Codec.BOOL.xmap(
            ArcherQuiverData::new, ArcherQuiverData::enabled);

    public static final StreamCodec<RegistryFriendlyByteBuf, ArcherQuiverData> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, data) -> buffer.writeBoolean(data.enabled()),
                    buffer -> new ArcherQuiverData(buffer.readBoolean()));

    public static boolean hasPermission(@Nullable Player player) {
        return player != null
                && player.getData(ModAttachments.ARCHER_TIAN_XING_JIAN.get()).enabled();
    }
}
