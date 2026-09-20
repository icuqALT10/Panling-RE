package icu.icuqalt10.panlingre.animation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;

/** Atomic clip + clock snapshot, included in vanilla tracking metadata for late observers. */
public final class SyncedEntityAnimation {
    private final Entity entity;
    private final EntityDataAccessor<CompoundTag> key;

    public SyncedEntityAnimation(Entity entity, EntityDataAccessor<CompoundTag> key) {
        this.entity = entity;
        this.key = key;
    }

    public WorldTimeAnimationController.Playback playback() {
        CompoundTag tag = entity.getEntityData().get(key);
        return new WorldTimeAnimationController.Playback(tag.getString("clip"),
                tag.contains("start") ? tag.getLong("start") : -1, tag.getBoolean("loop"),
                tag.contains("speed") ? tag.getDouble("speed") : 1);
    }

    public void start(String clip, boolean loop, double speed) {
        if (entity.level().isClientSide) return;
        CompoundTag tag = new CompoundTag();
        tag.putString("clip", clip);
        tag.putLong("start", entity.level().getGameTime());
        tag.putBoolean("loop", loop);
        tag.putDouble("speed", speed);
        entity.getEntityData().set(key, tag);
    }

    /** Preserve the clock while the same loop remains active; no per-tick packets. */
    public void loop(String clip, double speed) {
        var current = playback();
        if (!current.active() || !current.loop() || !current.animation().equals(clip) || current.speed() != speed)
            start(clip, true, speed);
    }

    public void stop() {
        if (!entity.level().isClientSide) entity.getEntityData().set(key, new CompoundTag());
    }

    public void save(CompoundTag tag, String name) { tag.put(name, entity.getEntityData().get(key).copy()); }
    public void load(CompoundTag tag, String name) {
        if (tag.contains(name)) entity.getEntityData().set(key, tag.getCompound(name).copy());
    }
}
