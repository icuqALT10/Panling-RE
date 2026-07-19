package icu.icuqalt10.panlingre.skill;

import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.item.skill_trigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.*;

public class ClientSkillState {

    public record SkillSlot(ResourceLocation itemId, int skillIndex, SkillData data, ItemStack source) {}

    private static final List<SkillSlot> availableSkills = new ArrayList<>();
    private static int selectedIndex = -1;
    /** 按 SkillName 去重用的 nameKey 集合 */
    private static final Set<String> addedNameKeys = new HashSet<>();
    /** 持久记住的上次选中技能的 nameKey */
    private static String lastSelectedNameKey = "";
    private static final Map<String, long[]> cooldownData = new HashMap<>();
    private static boolean activateMode = true;

    public static void rebuild(Player player) {
        availableSkills.clear();
        addedNameKeys.clear();
        addSkillsFromStack(player.getMainHandItem());
        addSkillsFromStack(player.getOffhandItem());
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
            handler.findCurios(stack -> true).forEach(result ->
                addSkillsFromStack(result.stack())
            )
        );

        // 恢复上次选中
        selectedIndex = -1;
        if (!lastSelectedNameKey.isEmpty()) {
            for (int i = 0; i < availableSkills.size(); i++) {
                if (availableSkills.get(i).data().name().equals(lastSelectedNameKey)) {
                    selectedIndex = i;
                    break;
                }
            }
        }
        // 没找到 → 默认第一个
        if (selectedIndex == -1 && !availableSkills.isEmpty()) {
            selectedIndex = 0;
            lastSelectedNameKey = availableSkills.get(0).data().name();
        }
    }

    private static void addSkillsFromStack(ItemStack stack) {
        if (stack.isEmpty()) return;
        if (stack.getItem() instanceof skill_trigger st) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            int count = st.getSkillCount();
            for (int i = 0; i < count; i++) {
                String name = st.getSkillNameKey(i);
                if (name.isEmpty()) name = stack.getDescriptionId();
                // 同名技能只保留第一个
                if (addedNameKeys.contains(name)) continue;
                addedNameKeys.add(name);

                SkillData sd = new SkillData("skill_use",
                        st.getSkillIcon(i), name,
                        st.getSkillLingQiCost(i), st.getSkillCD(i));
                availableSkills.add(new SkillSlot(id, i, sd, stack));
            }
        }
    }

    public static List<SkillSlot> getAvailableSkills() { return availableSkills; }
    public static int getSkillCount() { return availableSkills.size(); }

    public static SkillSlot getSelectedSkill() {
        if (selectedIndex >= 0 && selectedIndex < availableSkills.size())
            return availableSkills.get(selectedIndex);
        return null;
    }
    public static void selectSkill(int index) {
        selectedIndex = index;
        if (index >= 0 && index < availableSkills.size())
            lastSelectedNameKey = availableSkills.get(index).data().name();
    }
    public static int getSelectedIndex() { return selectedIndex; }

    public static boolean isActivateMode() { return activateMode; }
    public static void toggleMode() { activateMode = !activateMode; }

    public static long getReducedCooldown(Player player, long baseCd) {
        double cdRemove = 2.0 - player.getAttributeValue(ModAttributes.COOLDOWN_REMOVE);
        return (long)(baseCd * cdRemove);
    }

    /** 按 nameKey 记录冷却（同名技能共用冷却） */
    private static String cooldownKey(SkillSlot slot) {
        return slot.data().name();
    }

    public static void recordCooldown(SkillSlot slot, long cdMs) {
        cooldownData.put(cooldownKey(slot),
                new long[]{System.currentTimeMillis() + cdMs, cdMs});
    }

    public static float getCooldownProgress(SkillSlot slot) {
        long[] data = cooldownData.get(cooldownKey(slot));
        if (data == null) return 0f;
        long now = System.currentTimeMillis();
        if (now >= data[0]) {
            cooldownData.remove(cooldownKey(slot));
            return 0f;
        }
        return data[1] <= 0 ? 0f : (data[0] - now) / (float) data[1];
    }
}
