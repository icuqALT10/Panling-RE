package icu.icuqalt10.panlingre.item.common;

import icu.icuqalt10.panlingre.compat.beyonddimensions.BeyondDimensionsAccess;
import icu.icuqalt10.panlingre.compat.beyonddimensions.BeyondDimensionsNetworkStorage;
import icu.icuqalt10.panlingre.init.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.UUID;

public class NaWuCiItem extends Item implements ICurioItem {
    private static final String FABAO_SLOT = "fabao";
    private static final double RANGE = 15.0D;
    private static final double RANGE_SQR = RANGE * RANGE;

    public NaWuCiItem() {
        super(new Properties()
                .stacksTo(1)
                .fireResistant()
                .component(ModComponents.NA_WU_CI_SHI_NETWORK_MODE.get(), false)
                .component(ModComponents.NA_WU_CI_SHI_NET_ID.get(), -1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.fail(stack);
            if (!level.isClientSide) toggleNetworkBinding(player, stack);
        } else if (!level.isClientSide) {
            boolean networkMode = !isNetworkMode(stack);
            stack.set(ModComponents.NA_WU_CI_SHI_NETWORK_MODE.get(), networkMode);
            player.displayClientMessage(Component.translatable(networkMode
                    ? "item.panlingre.na_wu_ci_shi.mode.network"
                    : "item.panlingre.na_wu_ci_shi.mode.inventory"), true);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity wearer = slotContext.entity();
        if (!FABAO_SLOT.equals(slotContext.identifier())
                || wearer.level().isClientSide
                || !(wearer instanceof Player player)) {
            return;
        }

        AABB area = player.getBoundingBox().inflate(RANGE);
        collectExperience(player, area);

        if (isNetworkMode(stack)) {
            collectItemsToNetwork(player, stack, area);
        } else {
            collectItemsToInventory(player, area);
        }
    }

    private static void collectExperience(Player player, AABB area) {
        for (ExperienceOrb orb : player.level().getEntitiesOfClass(ExperienceOrb.class, area,
                orb -> canReach(player, orb))) {
            orb.playerTouch(player);
        }
    }

    private static void collectItemsToInventory(Player player, AABB area) {
        for (ItemEntity itemEntity : player.level().getEntitiesOfClass(ItemEntity.class, area,
                item -> canPlayerPickUp(player, item))) {
            itemEntity.playerTouch(player);
        }
    }

    private static void collectItemsToNetwork(Player player, ItemStack magnet, AABB area) {
        int networkId = magnet.getOrDefault(ModComponents.NA_WU_CI_SHI_NET_ID.get(), -1);
        if (networkId < 0 || !BeyondDimensionsAccess.isInstalled()) return;

        for (ItemEntity itemEntity : player.level().getEntitiesOfClass(ItemEntity.class, area,
                item -> canPlayerPickUp(player, item))) {
            ItemStack dropped = itemEntity.getItem();
            if (dropped.isEmpty() || EventHooks.fireItemPickupPre(itemEntity, player).canPickup().isFalse()) continue;

            int inserted = BeyondDimensionsNetworkStorage.insert(networkId, dropped);
            if (inserted <= 0) continue;

            ItemStack pickedUp = dropped.copyWithCount(inserted);
            dropped.shrink(inserted);
            EventHooks.fireItemPickupPost(itemEntity, player, pickedUp);
            player.take(itemEntity, inserted);
            player.awardStat(Stats.ITEM_PICKED_UP.get(pickedUp.getItem()), inserted);
            player.onItemPickup(itemEntity);
            if (dropped.isEmpty()) itemEntity.discard();
        }
    }

    private static boolean canPlayerPickUp(Player player, ItemEntity itemEntity) {
        if (!canReach(player, itemEntity) || itemEntity.hasPickUpDelay() || itemEntity.getItem().isEmpty()) return false;
        UUID owner = itemEntity.getTarget();
        return owner == null || owner.equals(player.getUUID());
    }

    private static boolean canReach(Player player, net.minecraft.world.entity.Entity entity) {
        return !entity.isRemoved() && entity.distanceToSqr(player) <= RANGE_SQR;
    }

    private static void toggleNetworkBinding(Player player, ItemStack stack) {
        int currentId = stack.getOrDefault(ModComponents.NA_WU_CI_SHI_NET_ID.get(), -1);
        if (currentId >= 0) {
            stack.set(ModComponents.NA_WU_CI_SHI_NET_ID.get(), -1);
            player.displayClientMessage(Component.translatable("item.panlingre.na_wu_ci_shi.unbound", currentId), true);
            return;
        }

        if (!BeyondDimensionsAccess.isInstalled()) {
            player.displayClientMessage(Component.translatable("item.panlingre.na_wu_ci_shi.no_byd"), true);
            return;
        }

        int networkId = BeyondDimensionsNetworkStorage.getPlayerNetworkId(player);
        if (networkId < 0) {
            player.displayClientMessage(Component.translatable("item.panlingre.na_wu_ci_shi.no_network"), true);
            return;
        }

        stack.set(ModComponents.NA_WU_CI_SHI_NET_ID.get(), networkId);
        player.displayClientMessage(Component.translatable("item.panlingre.na_wu_ci_shi.bound", networkId), true);
    }

    private static boolean isNetworkMode(ItemStack stack) {
        return stack.getOrDefault(ModComponents.NA_WU_CI_SHI_NETWORK_MODE.get(), false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        boolean networkMode = isNetworkMode(stack);
        int networkId = stack.getOrDefault(ModComponents.NA_WU_CI_SHI_NET_ID.get(), -1);
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("item.PanlingRE.na_wu_ci_shi.lore1"));
        tooltip.add(Component.translatable("item.PanlingRE.na_wu_ci_shi.skill1"));
        tooltip.add(Component.translatable("item.PanlingRE.na_wu_ci_shi.skill2"));
        tooltip.add(Component.translatable("item.PanlingRE.na_wu_ci_shi.skill3"));
        tooltip.add(Component.translatable("item.PanlingRE.na_wu_ci_shi.skill4"));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable(networkMode
                        ? "item.PanlingRE.na_wu_ci_shi.tip1"
                        : "item.PanlingRE.na_wu_ci_shi.tip2")
                .withStyle(networkMode ? ChatFormatting.AQUA : ChatFormatting.GREEN));
        tooltip.add((networkId >= 0
                        ? Component.translatable("item.panlingre.na_wu_ci_shi.tooltip.bound", networkId)
                        : Component.translatable("item.panlingre.na_wu_ci_shi.tooltip.unbound"))
                .withStyle(networkId >= 0 ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
