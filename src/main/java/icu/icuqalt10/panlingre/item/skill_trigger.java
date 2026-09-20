package icu.icuqalt10.panlingre.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import icu.icuqalt10.panlingre.entity.MultipartEntity;
import icu.icuqalt10.panlingre.util.SkillHelper;

import java.util.List;
import org.jetbrains.annotations.Nullable;

public interface skill_trigger {
    boolean skill_use(Level level, Player player, ItemStack stack, int skillIndex);

    /** 返回false阻止技能执行（灵气检查前） */
    default boolean canUse(Level level, Player player, ItemStack stack, int skillIndex) { return true; }

    /** 该物品提供的技能数量 */
    default int getSkillCount() { return 1; }

    /** Stack-aware variant used by items whose skills are stored in data components. */
    default int getSkillCount(ItemStack stack) { return getSkillCount(); }

    /** 每个技能的冷却（毫秒） */
    default long getSkillCD(int skillIndex) { return 0L; }

    default long getSkillCD(ItemStack stack, int skillIndex) { return getSkillCD(skillIndex); }

    /** Cast wind-up in game ticks. Skills are instant unless they explicitly override this. */
    default int getSkillCastTimeTicks(int skillIndex) { return 0; }

    default int getSkillCastTimeTicks(ItemStack stack, int skillIndex) {
        return getSkillCastTimeTicks(skillIndex);
    }

    /** Translation key for the skill name; an empty value falls back to the item description ID. */
    default String getSkillNameKey(int skillIndex) { return ""; }

    default String getSkillNameKey(ItemStack stack, int skillIndex) { return getSkillNameKey(skillIndex); }

    /** 每个技能的灵气消耗 */
    default float getSkillLingQiCost(int skillIndex) { return 0f; }

    default float getSkillLingQiCost(ItemStack stack, int skillIndex) {
        return getSkillLingQiCost(skillIndex);
    }

    /** 自定义图标纹理路径，null则渲染物品本身 */
    @Nullable
    default ResourceLocation getSkillIcon(int skillIndex) { return null; }

    @Nullable
    default ResourceLocation getSkillIcon(ItemStack stack, int skillIndex) {
        return getSkillIcon(skillIndex);
    }

    /** 技能描述行（最多3行），返回null或空数组则显示空行 */
    @Nullable
    default String[] getSkillDescription(int skillIndex) { return null; }

    @Nullable
    default String[] getSkillDescription(ItemStack stack, int skillIndex) {
        return getSkillDescription(skillIndex);
    }

    /** Item rendered for the skill when no custom icon texture is supplied. */
    default ItemStack getSkillDisplayStack(ItemStack stack, int skillIndex) { return stack; }

    /** Stable server-side cooldown identity. */
    default String getSkillCooldownKey(ItemStack stack, int skillIndex) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + ".skill_" + skillIndex;
    }

    /** Optional vanilla item cooldown applied after a successful wheel activation. */
    @Nullable
    default Item getSkillCooldownItem(ItemStack stack, int skillIndex) { return null; }

    /**
     * 统一的技能目标入口。技能实现不要再直接调用
     * level.getEntitiesOfClass(LivingEntity.class, box)，否则多节实体的
     * 子碰撞箱会被当成普通 Entity 丢失。MultipartEntity.collectTargets 会
     * 把所有子节归并为主体，并由主体的 hurt() 继续选择实际受击部位。
     */
    static List<LivingEntity> skillTargets(Level level, AABB box, LivingEntity caster) {
        return MultipartEntity.collectTargets(level, box, caster).stream()
                .filter(SkillHelper.combatTargetFilter(caster))
                .toList();
    }

    /** 技能伤害统一入口，兼容普通实体和多节实体。 */
    static boolean hurtSkillTarget(Entity target, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (target instanceof LivingEntity living) return living.hurt(source, amount);
        if (target instanceof MultipartEntity.MultipartPart part
                && part.getMultipartRoot() instanceof LivingEntity living) {
            return living.hurt(source, amount);
        }
        return false;
    }
}
