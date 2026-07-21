package icu.icuqalt10.panlingre.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import icu.icuqalt10.panlingre.init.ModAttachments;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class BaFangYiData {

    private Map<String, Set<String>> unlockedMap = new HashMap<>();
    private long teleportCooldownEnd = 0;

    public BaFangYiData() {}

    public static final Codec<BaFangYiData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf())
                    .optionalFieldOf("unlocked", Map.of())
                    .forGetter(d -> {
                        Map<String, List<String>> map = new HashMap<>();
                        for (Map.Entry<String, Set<String>> entry : d.unlockedMap.entrySet()) {
                            map.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                        }
                        return map;
                    }),
            Codec.LONG.optionalFieldOf("teleportCooldownEnd", 0L)
                    .forGetter(d -> d.teleportCooldownEnd)
    ).apply(instance, (map, cooldownEnd) -> {
        BaFangYiData data = new BaFangYiData();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            data.unlockedMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        data.teleportCooldownEnd = cooldownEnd;
        return data;
    }));

    public boolean isMajorUnlocked(String majorId) {
        return unlockedMap.containsKey(majorId);
    }

    public boolean isSubUnlocked(String majorId, String subId) {
        return unlockedMap.containsKey(majorId) && unlockedMap.get(majorId).contains(subId);
    }

    public Map<String, Set<String>> getUnlockedMap() {
        return unlockedMap;
    }

    public long getTeleportCooldownEnd() {
        return teleportCooldownEnd;
    }

    public boolean isTeleportOnCooldown(long currentGameTime) {
        return currentGameTime < teleportCooldownEnd;
    }

    public void setTeleportCooldown(long currentGameTime, long cooldownTicks) {
        this.teleportCooldownEnd = currentGameTime + cooldownTicks;
    }

    public void unlockMajor(String majorId) {
        unlockedMap.putIfAbsent(majorId, new HashSet<>());
    }

    public void unlockSub(String majorId, String subId) {
        unlockedMap.computeIfAbsent(majorId, k -> new HashSet<>()).add(subId);
    }

    public void removeMajor(String majorId) {
        unlockedMap.remove(majorId);
    }

    public void removeSub(String majorId, String subId) {
        if (unlockedMap.containsKey(majorId)) {
            unlockedMap.get(majorId).remove(subId);
        }
    }

    public void clear() {
        unlockedMap.clear();
    }

    // --- 静态工具 ---
    public static BaFangYiData get(Player player) {
        return player.getData(ModAttachments.BA_FANG_YI_DATA.get());
    }

    public static boolean addMajor(Player player, String majorId) {
        BaFangYiData data = get(player);
        if (data.isMajorUnlocked(majorId)) return false;
        data.unlockMajor(majorId);
        player.setData(ModAttachments.BA_FANG_YI_DATA.get(), data);
        return true;
    }

    public static boolean removeMajor(Player player, String majorId) {
        BaFangYiData data = get(player);
        if (!data.isMajorUnlocked(majorId)) return false;
        data.removeMajor(majorId);
        player.setData(ModAttachments.BA_FANG_YI_DATA.get(), data);
        return true;
    }

    public static boolean addSub(Player player, String majorId, String subId) {
        BaFangYiData data = get(player);
        if (data.isSubUnlocked(majorId, subId)) return false;
        data.unlockSub(majorId, subId);
        player.setData(ModAttachments.BA_FANG_YI_DATA.get(), data);
        return true;
    }

    public static boolean removeSub(Player player, String majorId, String subId) {
        BaFangYiData data = get(player);
        if (!data.isSubUnlocked(majorId, subId)) return false;
        data.removeSub(majorId, subId);
        player.setData(ModAttachments.BA_FANG_YI_DATA.get(), data);
        return true;
    }

    public static boolean queryMajor(Player player, String majorId) {
        return get(player).isMajorUnlocked(majorId);
    }

    public static boolean querySub(Player player, String majorId, String subId) {
        return get(player).isSubUnlocked(majorId, subId);
    }
}
