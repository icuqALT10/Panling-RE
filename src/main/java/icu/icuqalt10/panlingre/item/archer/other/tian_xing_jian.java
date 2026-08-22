package icu.icuqalt10.panlingre.item.archer.other;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.attachment.ArcherQuiverData;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.init.ModComponents;
import icu.icuqalt10.panlingre.item.archer.zhu_ri;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

public class tian_xing_jian extends Item implements ICurioItem {
    public static final int FORM_INACTIVE = 0;
    public static final int FORM_SNIPER = 1;
    public static final int FORM_RANGER = 2;
    public static final int RANGER_COOLDOWN_TICKS = 30;
    public static final double SNIPER_MIN_DOT = Math.cos(Math.toRadians(30.0D));
    private static final Map<UUID, PositionSample> LAST_POSITIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_SNIPER_MESSAGES = new ConcurrentHashMap<>();

    public static final TagKey<Item> SNIPER_ITEMS = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "archer/juji"));
    public static final TagKey<Item> RANGER_ITEMS = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "archer/youxia"));

    public tian_xing_jian() {
        super(new Properties()
                .stacksTo(1)
                .fireResistant()
                .component(ModComponents.TIAN_XING_JIAN_FORM.get(), FORM_INACTIVE));
    }

    public static int getForm(ItemStack stack) {
        return stack.getOrDefault(ModComponents.TIAN_XING_JIAN_FORM.get(), FORM_INACTIVE);
    }

    public static int getFormForWeapon(ItemStack weapon) {
        if (weapon.getItem() instanceof zhu_ri) {
            return weapon.getOrDefault(ModComponents.IS_POWERED.get(), false)
                    ? FORM_SNIPER
                    : FORM_RANGER;
        }
        if (weapon.is(SNIPER_ITEMS)) return FORM_SNIPER;
        if (weapon.is(RANGER_ITEMS)) return FORM_RANGER;
        return FORM_INACTIVE;
    }

    public static boolean hasEquippedForm(Player player, int form) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack ->
                        stack.getItem() instanceof tian_xing_jian && getForm(stack) == form))
                .isPresent();
    }

    public static boolean isSniperActive(Player player) {
        return ArcherQuiverData.hasPermission(player)
                && getFormForWeapon(player.getMainHandItem()) == FORM_SNIPER
                && CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack ->
                        stack.getItem() instanceof tian_xing_jian))
                .isPresent();
    }

    public static boolean isValidSniperTarget(Player player, LivingEntity target) {
        if (target == player
                || target instanceof ArmorStand
                || !target.isAlive()
                || !target.isAttackable()
                || target.isInvulnerable()) {
            return false;
        }
        if (target instanceof Player targetPlayer
                && (targetPlayer.isCreative() || targetPlayer.isSpectator())) {
            return false;
        }
        if (player.getTeam() != null && player.getTeam() == target.getTeam()) {
            return false;
        }
        return !target.isInvulnerableTo(player.damageSources().playerAttack(player));
    }

    public static void notifySniperShot(Player player) {
        long gameTime = player.level().getGameTime();
        Long previous = LAST_SNIPER_MESSAGES.put(player.getUUID(), gameTime);
        if (previous == null || previous != gameTime) {
            player.displayClientMessage(Component.translatable(
                    "item.PanlingRE.tian_xing_jian.juji.skill.success"), true);
        }
    }

    public static void deactivateEquipped(Player player) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            var equipped = handler.getEquippedCurios();
            for (int slot = 0; slot < equipped.getSlots(); slot++) {
                ItemStack stack = equipped.getStackInSlot(slot);
                if (stack.getItem() instanceof tian_xing_jian
                        && getForm(stack) != FORM_INACTIVE) {
                    stack.set(ModComponents.TIAN_XING_JIAN_FORM.get(), FORM_INACTIVE);
                }
            }
        });
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        if (!(entity instanceof Player player)) return;

        int form = ArcherQuiverData.hasPermission(player)
                ? getFormForWeapon(player.getMainHandItem())
                : FORM_INACTIVE;
        if (getForm(stack) != form) {
            stack.set(ModComponents.TIAN_XING_JIAN_FORM.get(), form);
        }

        if (!player.level().isClientSide) {
            boolean moved = sampleMovement(player);
            if (form == FORM_RANGER && moved) {
                tryRangerReload(player);
            }
        }
    }

    private static boolean sampleMovement(Player player) {
        PositionSample current = new PositionSample(player.getX(), player.getZ());
        PositionSample previous = LAST_POSITIONS.put(player.getUUID(), current);
        return previous != null && previous.horizontalDistanceSqr(current) > 1.0E-6D;
    }

    private void tryRangerReload(Player player) {
        ItemStack weapon = player.getMainHandItem();
        if (!(weapon.getItem() instanceof CrossbowItem)
                || CrossbowItem.isCharged(weapon)
                || player.getCooldowns().isOnCooldown(this)
                || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        int projectileCount = EnchantmentHelper.processProjectileCount(
                serverLevel, weapon, player, 1);
        List<ItemStack> arrows = IntStream.range(0, projectileCount)
                .mapToObj(index -> new ItemStack(Items.ARROW))
                .toList();
        weapon.set(DataComponents.CHARGED_PROJECTILES,
                ChargedProjectiles.of(arrows));
        cooldown_remove.cd_remove(player, this, RANGER_COOLDOWN_TICKS);
        player.displayClientMessage(
                Component.translatable("item.PanlingRE.tian_xing_jian.youxia.skill.success"), true);
    }

    private record PositionSample(double x, double z) {
        private double horizontalDistanceSqr(PositionSample other) {
            double dx = x - other.x;
            double dz = z - other.z;
            return dx * dx + dz * dz;
        }
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        return switch (getForm(stack)) {
            case FORM_SNIPER -> createSniperAttributeModifiers(slotContext, id, stack);
            case FORM_RANGER -> createRangerAttributeModifiers(slotContext, id, stack);
            default -> HashMultimap.create();
        };
    }

    /** 狙击形态属性模板，在这里添加或修改属性。 */
    private static Multimap<Holder<Attribute>, AttributeModifier> createSniperAttributeModifiers(
            SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = HashMultimap.create();

        modifiers.put(ModAttributes.ARROW_DAMAGE, new AttributeModifier(
                id,
                0.25,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

        modifiers.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(
                id,
                -0.25,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

        modifiers.put(Attributes.ARMOR, new AttributeModifier(
                id,
                0.05,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

        return modifiers;
    }

    /** 游侠形态属性模板，在这里添加或修改属性。 */
    private static Multimap<Holder<Attribute>, AttributeModifier> createRangerAttributeModifiers(
            SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = HashMultimap.create();

        modifiers.put(ModAttributes.ARROW_DAMAGE, new AttributeModifier(
                id,
                2.0,
                AttributeModifier.Operation.ADD_VALUE));

        modifiers.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(
                id,
                0.25,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

        modifiers.put(Attributes.ARMOR, new AttributeModifier(
                id,
                -0.15,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

        return modifiers;
    }

    @Override
    public Component getName(ItemStack stack) {
        return switch (getForm(stack)) {
            case FORM_SNIPER -> Component.translatable("item.panlingre.tian_xing_jian.juji");
            case FORM_RANGER -> Component.translatable("item.panlingre.tian_xing_jian.youxia");
            default -> Component.translatable("item.panlingre.tian_xing_jian.inactive");
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.PanlingRE.lore.rare4"));
        tooltip.add(Component.translatable("item.PanlingRE.lore.limit1"));
        tooltip.add(Component.translatable("item.PanlingRE.tian_xing_jian.lore1"));

        if (getForm(stack) == FORM_SNIPER) {
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.tian_xing_jian.juji.skill1"));
            tooltip.add(Component.translatable("item.PanlingRE.tian_xing_jian.juji.skill2"));
        } else if (getForm(stack) == FORM_RANGER) {
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.tian_xing_jian.youxia.skill1",
                    cooldown_remove.getCooldownText(
                            SafeClientAccess.getClientPlayer(), RANGER_COOLDOWN_TICKS)));
            tooltip.add(Component.translatable("item.PanlingRE.tian_xing_jian.youxia.skill2"));
            tooltip.add(Component.translatable("item.PanlingRE.tian_xing_jian.youxia.skill3"));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }
}
