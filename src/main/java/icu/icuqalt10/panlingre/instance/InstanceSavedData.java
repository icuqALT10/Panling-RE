package icu.icuqalt10.panlingre.instance;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public final class InstanceSavedData extends SavedData {
    public static final Factory<InstanceSavedData> FACTORY = new Factory<>(InstanceSavedData::new, InstanceSavedData::load);
    private final Map<String, Integer> preparedVersions = new HashMap<>();

    public int preparedVersion(String instanceId, int slot) {
        return preparedVersions.getOrDefault(key(instanceId, slot), -1);
    }

    public void setPreparedVersion(String instanceId, int slot, int version) {
        preparedVersions.put(key(instanceId, slot), version);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        preparedVersions.forEach((key, version) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("Key", key);
            entry.putInt("Version", version);
            entries.add(entry);
        });
        tag.put("Prepared", entries);
        return tag;
    }

    private static InstanceSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        InstanceSavedData data = new InstanceSavedData();
        ListTag entries = tag.getList("Prepared", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            data.preparedVersions.put(entry.getString("Key"), entry.getInt("Version"));
        }
        return data;
    }

    private static String key(String instanceId, int slot) {
        return instanceId + "#" + slot;
    }
}
