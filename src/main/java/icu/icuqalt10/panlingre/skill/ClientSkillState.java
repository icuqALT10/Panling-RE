package icu.icuqalt10.panlingre.skill;

import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.item.fuzhi.FuZhiBagItem;
import icu.icuqalt10.panlingre.item.fuzhi.FuZhiItem;
import icu.icuqalt10.panlingre.item.common.WeaponCaseItem;
import icu.icuqalt10.panlingre.item.skill_trigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.*;

public class ClientSkillState {

    public record SkillSlot(ResourceLocation itemId, int skillIndex, SkillData data,
                            ItemStack source, ItemStack displayStack) {}

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
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            var equippedCurios = handler.getEquippedCurios();
            for (int slot = 0; slot < equippedCurios.getSlots(); slot++) {
                ItemStack equipped = equippedCurios.getStackInSlot(slot);
                if (!(equipped.getItem() instanceof FuZhiBagItem)
                        && !(equipped.getItem() instanceof WeaponCaseItem)) {
                    addSkillsFromStack(equipped);
                }
            }

            // Skill containers only provide their nested skills from an active fabao slot.
            handler.getStacksHandler(FuZhiBagItem.CURIO_SLOT).ifPresent(stackHandler -> {
                var stacks = stackHandler.getStacks();
                for (int slot = 0; slot < stacks.getSlots(); slot++) {
                    ItemStack equipped = stacks.getStackInSlot(slot);
                    if ((equipped.getItem() instanceof FuZhiBagItem
                            || equipped.getItem() instanceof WeaponCaseItem weaponCase
                            && weaponCase.canProvideSkills(player, equipped))
                            && handler.isSlotActive(FuZhiBagItem.CURIO_SLOT, slot)) {
                        addSkillsFromStack(equipped);
                    }
                }
            });
        });
        player.getArmorSlots().forEach(stack -> {
            if (!(stack.getItem() instanceof FuZhiBagItem)
                    && !(stack.getItem() instanceof WeaponCaseItem)) addSkillsFromStack(stack);
        });
        if (!(player.getOffhandItem().getItem() instanceof FuZhiBagItem)
                && !(player.getOffhandItem().getItem() instanceof WeaponCaseItem)) {
            addSkillsFromStack(player.getOffhandItem());
        }
        if (!(player.getMainHandItem().getItem() instanceof FuZhiBagItem)
                && !(player.getMainHandItem().getItem() instanceof WeaponCaseItem)) {
            addSkillsFromStack(player.getMainHandItem());
        }

        // 恢复上次选中。如果该技能只是暂时不可用，保留 nameKey，
        // 这样玩家重新换回对应武器时会自动恢复原选中技能。
        selectedIndex = -1;
        if (!lastSelectedNameKey.isEmpty()) {
            for (int i = 0; i < availableSkills.size(); i++) {
                if (availableSkills.get(i).data().name().equals(lastSelectedNameKey)) {
                    selectedIndex = i;
                    break;
                }
            }
        }
        // 只有尚未选过任何技能时才初始化为第一个。
        if (lastSelectedNameKey.isEmpty() && !availableSkills.isEmpty()) {
            selectedIndex = 0;
            lastSelectedNameKey = availableSkills.get(0).data().name();
        }
    }

    private static void addSkillsFromStack(ItemStack stack) {
        // Individual talismans are right-click consumables, not independent skill sources.
        // A FuZhiBagItem exposes its stored talismans through its own skill_trigger methods.
        if (stack.isEmpty() || stack.getItem() instanceof FuZhiItem) return;
        if (stack.getItem() instanceof skill_trigger st) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            int count = st.getSkillCount(stack);
            for (int i = 0; i < count; i++) {
                String name = st.getSkillNameKey(stack, i);
                if (name.isEmpty()) name = stack.getDescriptionId();
                // 同名技能只保留第一个
                if (addedNameKeys.contains(name)) continue;
                addedNameKeys.add(name);

                SkillData sd = new SkillData("skill_use",
                        st.getSkillIcon(stack, i), name,
                        st.getSkillLingQiCost(stack, i), st.getSkillCD(stack, i),
                        Math.max(0, st.getSkillCastTimeTicks(stack, i)));
                ItemStack displayStack = st.getSkillDisplayStack(stack, i);
                if (displayStack.isEmpty()) displayStack = stack;
                availableSkills.add(new SkillSlot(id, i, sd, stack, displayStack));
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

    /**
     * 返回当前选中技能；原选中技能已消失时，才在玩家主动释放的这一刻
     * 改选第一个可用技能。没有可用技能时保留原记忆。
     */
    public static SkillSlot getSkillForActivation() {
        SkillSlot selected = getSelectedSkill();
        if (selected != null || availableSkills.isEmpty()) return selected;

        selectSkill(0);
        return availableSkills.get(0);
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
        double cdRemove = 1.0 - player.getAttributeValue(ModAttributes.COOLDOWN_REMOVE);
        return (long)(baseCd * cdRemove);
    }

    /** 按 nameKey 记录冷却（同名技能共用冷却） */
    private static String cooldownKey(SkillSlot slot) {
        return slot.data().name();
    }

    public static void recordCooldown(SkillSlot slot, long cdMs) {
        recordCooldown(cooldownKey(slot), cdMs);
    }

    public static void recordCooldown(String cooldownKey, long cdMs) {
        long duration = Math.max(0L, cdMs);
        cooldownData.put(cooldownKey,
                new long[]{System.currentTimeMillis() + duration, duration});
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
