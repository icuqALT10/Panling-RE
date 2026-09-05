package icu.icuqalt10.panlingre.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import icu.icuqalt10.panlingre.init.ModAttachments;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** Persistent profession permissions, independent from the symbolic profession Curios. */
public record ZhiyeData(boolean warrior, boolean archer, boolean warlock) {
    public ZhiyeData() {
        this(false, false, false);
    }

    public static final Codec<ZhiyeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("warrior", false).forGetter(ZhiyeData::warrior),
            Codec.BOOL.optionalFieldOf("archer", false).forGetter(ZhiyeData::archer),
            Codec.BOOL.optionalFieldOf("warlock", false).forGetter(ZhiyeData::warlock)
    ).apply(instance, ZhiyeData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ZhiyeData> STREAM_CODEC = StreamCodec.of(
            (buffer, data) -> {
                buffer.writeBoolean(data.warrior());
                buffer.writeBoolean(data.archer());
                buffer.writeBoolean(data.warlock());
            },
            buffer -> new ZhiyeData(
                    buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean())
    );

    public boolean isEnabled(Profession profession) {
        return switch (profession) {
            case WARRIOR -> warrior;
            case ARCHER -> archer;
            case WARLOCK -> warlock;
        };
    }

    public ZhiyeData with(Profession profession, boolean enabled) {
        return switch (profession) {
            case WARRIOR -> new ZhiyeData(enabled, archer, warlock);
            case ARCHER -> new ZhiyeData(warrior, enabled, warlock);
            case WARLOCK -> new ZhiyeData(warrior, archer, enabled);
        };
    }

    public static boolean has(@Nullable Player player, Profession profession) {
        return player != null
                && player.getData(ModAttachments.ZHIYE.get()).isEnabled(profession);
    }

    /** Accepts both command names and the old namespaced profession item IDs. */
    public static boolean has(@Nullable Player player, String professionId) {
        Profession profession = Profession.fromName(professionId);
        return profession != null && has(player, profession);
    }

    public enum Profession {
        WARRIOR("warrior"),
        ARCHER("archer"),
        WARLOCK("warlock");

        private final String commandName;

        Profession(String commandName) {
            this.commandName = commandName;
        }

        public String commandName() {
            return commandName;
        }

        public static @Nullable Profession fromName(String name) {
            String normalized = name.toLowerCase(Locale.ROOT);
            int separator = normalized.indexOf(':');
            if (separator >= 0) normalized = normalized.substring(separator + 1);
            return switch (normalized) {
                case "warrior" -> WARRIOR;
                case "archer" -> ARCHER;
                case "warlock" -> WARLOCK;
                default -> null;
            };
        }
    }
}
