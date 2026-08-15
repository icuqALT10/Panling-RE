package icu.icuqalt10.panlingre.item.warrior.other;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.WarriorShieldData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.init.ModComponents;
import icu.icuqalt10.panlingre.item.warrior.ding_hai_shen_zhen;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.ItemAbilities;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class di_shi_dun extends ShieldItem {

    private static final ResourceLocation MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "di_shi_dun");

    private static final ResourceLocation JINZHONG_MAX_ABSORPTION_ID =
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "di_shi_dun_jinzhong_max_absorption");

    public static final int FORM_INACTIVE = 0;
    public static final int FORM_POJUN = 1;
    public static final int FORM_JINZHONG = 2;
    public static final int POJUN_COOLDOWN_TICKS_SUCCESS = 20;
    public static final int POJUN_COOLDOWN_TICKS_FAIL = 150;
    public static final int POJUN_BLOCK_TICKS = 7;

    public static final TagKey<Item> POJUN_ITEMS = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "warrior/pojun"));
    public static final TagKey<Item> JINZHONG_ITEMS = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "warrior/jinzhong"));

    public di_shi_dun() {
        super(new Properties()
                .stacksTo(1)
                .fireResistant()
                .component(ModComponents.DI_SHI_DUN_FORM.get(), FORM_INACTIVE));
    }

    public static int getForm(ItemStack stack) {
        return stack.getOrDefault(ModComponents.DI_SHI_DUN_FORM.get(), FORM_INACTIVE);
    }

    private static int getFormForWeapon(ItemStack weapon) {
        if (weapon.getItem() instanceof ding_hai_shen_zhen) {
            return weapon.getOrDefault(ModComponents.IS_POWERED.get(), false)
                    ? FORM_POJUN
                    : FORM_JINZHONG;
        }
        if (weapon.is(POJUN_ITEMS)) return FORM_POJUN;
        if (weapon.is(JINZHONG_ITEMS)) return FORM_JINZHONG;
        return FORM_INACTIVE;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!(entity instanceof Player player)) return;

        boolean isOffhandStack = player.getOffhandItem() == stack;
        int form = isOffhandStack && WarriorShieldData.hasPermission(player)
                ? getFormForWeapon(player.getMainHandItem())
                : FORM_INACTIVE;
        if (form == getForm(stack)) return;

        stack.set(ModComponents.DI_SHI_DUN_FORM.get(), form);

        if (player.getUseItem() == stack && form == FORM_INACTIVE) {
            player.stopUsingItem();
        }
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return getFormAttributeModifiers(getForm(stack));
    }

    private static ItemAttributeModifiers getFormAttributeModifiers(int form) {
        return switch (form) {
            case FORM_POJUN -> createPojunAttributeTemplate();
            case FORM_JINZHONG -> createJinzhongAttributeModifiers();
            default -> ItemAttributeModifiers.EMPTY;
        };
    }

    /**
     * 破军形态属性模板。之后可在 builder 上继续 add；形态 0 不会调用此模板。
     */
    private static ItemAttributeModifiers createPojunAttributeTemplate() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        builder.add(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(MODIFIER_ID,
                        10.0,
                        AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.OFFHAND);

        builder.add(Attributes.ATTACK_SPEED,
                new AttributeModifier(MODIFIER_ID,
                        0.15,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                EquipmentSlotGroup.OFFHAND);

        builder.add(Attributes.MOVEMENT_SPEED,
                new AttributeModifier(MODIFIER_ID,
                        -0.10,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                EquipmentSlotGroup.OFFHAND);

        builder.add(Attributes.ARMOR,
                new AttributeModifier(MODIFIER_ID,
                        0.05,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                EquipmentSlotGroup.OFFHAND);

        return builder.build();
    }

    private static ItemAttributeModifiers createJinzhongAttributeModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        builder.add(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(MODIFIER_ID,
                        5.0,
                        AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.OFFHAND);

        builder.add(Attributes.ATTACK_SPEED,
                new AttributeModifier(MODIFIER_ID,
                        -0.1,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                EquipmentSlotGroup.OFFHAND);

        builder.add(Attributes.MOVEMENT_SPEED,
                new AttributeModifier(MODIFIER_ID,
                        -0.15,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                EquipmentSlotGroup.OFFHAND);

        builder.add(Attributes.ARMOR,
                new AttributeModifier(MODIFIER_ID,
                        0.15,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                EquipmentSlotGroup.OFFHAND);

        return builder.build();
    }

    /** 按玩家实时护甲值刷新金钟形态提供的最大伤害吸收值。 */
    public static void refreshMaxAbsorption(Player player) {
        var maxAbsorption = player.getAttribute(Attributes.MAX_ABSORPTION);
        if (maxAbsorption == null) return;

        ItemStack offhand = player.getOffhandItem();
        boolean jinzhongActive = offhand.getItem() instanceof di_shi_dun
                && getForm(offhand) == FORM_JINZHONG;
        double desiredBonus = jinzhongActive
                ? player.getAttributeValue(Attributes.ARMOR) * 0.5D
                : 0.0D;

        AttributeModifier current = maxAbsorption.getModifier(JINZHONG_MAX_ABSORPTION_ID);
        if (desiredBonus <= 0.0D) {
            if (current != null) maxAbsorption.removeModifier(JINZHONG_MAX_ABSORPTION_ID);
            return;
        }

        if (current == null || Math.abs(current.amount() - desiredBonus) > 1.0E-6D) {
            maxAbsorption.addOrUpdateTransientModifier(new AttributeModifier(
                    JINZHONG_MAX_ABSORPTION_ID,
                    desiredBonus,
                    AttributeModifier.Operation.ADD_VALUE));
        }
    }

    // 格挡不消耗盾牌耐久。
    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (getForm(stack) == FORM_INACTIVE) {
            return InteractionResultHolder.fail(stack);
        }
        return super.use(level, player, hand);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (!level.isClientSide
                && livingEntity instanceof Player player
                && getForm(stack) == FORM_POJUN
                && getUseDuration(stack, livingEntity) - remainingUseDuration >= POJUN_BLOCK_TICKS) {
            applyPojunCooldown(player, false);
            player.stopUsingItem();
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        if (!level.isClientSide && livingEntity instanceof Player player && getForm(stack) == FORM_POJUN) {
            applyPojunCooldown(player, false);
        }
        super.releaseUsing(stack, level, livingEntity, timeLeft);
    }

    public void finishSuccessfulPojunBlock(Player player) {
        if (!player.level().isClientSide) {
            applyPojunCooldown(player, true);
        }
        player.stopUsingItem();
    }

    private void applyPojunCooldown(Player player, boolean success) {
        cooldown_remove.cd_remove(player, this,
                success ? POJUN_COOLDOWN_TICKS_SUCCESS : POJUN_COOLDOWN_TICKS_FAIL);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        if (itemAbility == ItemAbilities.SHIELD_BLOCK && getForm(stack) == FORM_INACTIVE) {
            return false;
        }
        return super.canPerformAction(stack, itemAbility);
    }

    @Override
    public Component getName(ItemStack stack) {
        return switch (getForm(stack)) {
            case FORM_POJUN -> Component.translatable("item.panlingre.di_shi_dun.pojun");
            case FORM_JINZHONG -> Component.translatable("item.panlingre.di_shi_dun.jinzhong");
            default -> Component.translatable("item.panlingre.di_shi_dun.inactive");
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag flag) {
        tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare4"));
        tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit0"));
        tooltipComponents.add(Component.translatable("item.PanlingRE.di_shi_dun.lore1"));

        switch (getForm(stack)) {
            case FORM_POJUN -> appendPojunLore(tooltipComponents);
            case FORM_JINZHONG -> appendJinzhongLore(tooltipComponents);
            default -> {
                // 形态 0 只显示到 lore1。
            }
        }

        super.appendHoverText(stack, context, tooltipComponents, flag);
    }

    /** 破军形态 lore 模板；可直接在这里增删翻译键。 */
    private static void appendPojunLore(List<Component> tooltip) {
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("item.PanlingRE.di_shi_dun.pojun.skill1"));
        tooltip.add(Component.translatable("item.PanlingRE.di_shi_dun.pojun.skill2",
                Component.keybind("key.use").withStyle(ChatFormatting.GOLD),
                cooldown_remove.getCooldownText(SafeClientAccess.getClientPlayer(), POJUN_COOLDOWN_TICKS_SUCCESS)));
        tooltip.add(Component.translatable("item.PanlingRE.di_shi_dun.pojun.skill3"));
        tooltip.add(Component.translatable("item.PanlingRE.di_shi_dun.pojun.skill4"));
    }

    private static void appendJinzhongLore(List<Component> tooltip) {
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("item.PanlingRE.di_shi_dun.jinzhong.skill1",
                Component.keybind("key.use").withStyle(ChatFormatting.GOLD)));
        tooltip.add(Component.translatable("item.PanlingRE.di_shi_dun.jinzhong.skill2"));
        tooltip.add(Component.translatable("item.PanlingRE.di_shi_dun.jinzhong.skill3"));
        tooltip.add(Component.translatable("item.PanlingRE.di_shi_dun.jinzhong.skill4"));
    }
}
