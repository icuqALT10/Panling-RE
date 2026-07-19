package icu.icuqalt10.panlingre.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public interface skill_trigger {
    boolean skill_use(Level level, Player player, ItemStack stack, int skillIndex);

    /** 返回false阻止技能执行（灵气检查前） */
    default boolean canUse(Level level, Player player, ItemStack stack, int skillIndex) { return true; }

    /** 该物品提供的技能数量 */
    default int getSkillCount() { return 1; }

    /** 每个技能的冷却（毫秒） */
    default long getSkillCD(int skillIndex) { return 0L; }

    /** 每个技能的翻译键，空串则用物品描述ID */
    default String getSkillNameKey(int skillIndex) { return ""; }

    /** 每个技能的灵气消耗 */
    default float getSkillLingQiCost(int skillIndex) { return 0f; }

    /** 自定义图标纹理路径，null则渲染物品本身 */
    @Nullable
    default ResourceLocation getSkillIcon(int skillIndex) { return null; }

    /** 技能描述行（最多3行），返回null或空数组则显示空行 */
    @Nullable
    default String[] getSkillDescription(int skillIndex) { return null; }
}
