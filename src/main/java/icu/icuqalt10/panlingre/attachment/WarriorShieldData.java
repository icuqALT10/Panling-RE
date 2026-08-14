package icu.icuqalt10.panlingre.attachment;

import com.mojang.serialization.Codec;
import icu.icuqalt10.panlingre.init.ModAttachments;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** 玩家使用战士盾牌的权限。 */
public record WarriorShieldData(boolean enabled) {

    public WarriorShieldData() {
        this(false);
    }

    public static final Codec<WarriorShieldData> CODEC = Codec.BOOL.xmap(
            WarriorShieldData::new, WarriorShieldData::enabled);

    public static final StreamCodec<RegistryFriendlyByteBuf, WarriorShieldData> STREAM_CODEC = StreamCodec.of(
            (buffer, data) -> buffer.writeBoolean(data.enabled()),
            buffer -> new WarriorShieldData(buffer.readBoolean())
    );

    public static boolean hasPermission(@Nullable Player player) {
        return player != null && player.getData(ModAttachments.WARRIOR_SHIELD.get()).enabled();
    }
}
