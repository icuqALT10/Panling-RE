package icu.icuqalt10.panlingre.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class FreezeData {
    private boolean frozen;
    private int duration;

    public FreezeData() {
        this(false, 0);
    }

    public FreezeData(boolean frozen, int duration) {
        this.frozen = frozen;
        this.duration = duration;
    }

    public static final Codec<FreezeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("frozen", false).forGetter(FreezeData::isFrozen),
            Codec.INT.optionalFieldOf("duration", 0).forGetter(FreezeData::getDuration)
    ).apply(instance, FreezeData::new));

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void tick() {
        if (frozen && duration > 0) {
            duration--;
            if (duration <= 0) {
                frozen = false;
            }
        }
    }
}
