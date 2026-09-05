package icu.icuqalt10.panlingre.player;

import icu.icuqalt10.panlingre.event.GameBusEvents;
import icu.icuqalt10.panlingre.attachment.ZhiyeData;
import icu.icuqalt10.panlingre.attachment.ZhiyeData.Profession;
import icu.icuqalt10.panlingre.init.ModEffects;
import icu.icuqalt10.panlingre.item.common.WeaponCaseItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;

/** Server-side guard against using equipment that belongs to another profession. */
public final class ProfessionEquipmentGuard {
    private static final int REJECTION_FREEZE_TICKS = 5;

    private ProfessionEquipmentGuard() {
    }

    public static boolean isInvalidForProfession(Player player, ItemStack stack) {
        return getRestrictionMessageKey(player, stack) != null;
    }

    /** Weapon cases may be carried and lent regardless of the stored weapons' profession. */
    public static boolean isHandCarryExempt(ItemStack stack) {
        return stack.getItem() instanceof WeaponCaseItem;
    }

    public static @Nullable String getRestrictionMessageKey(Player player, ItemStack stack) {
        if (stack.isEmpty()) return null;

        if (stack.getItem() instanceof WeaponCaseItem) {
            return WeaponCaseItem.isInvalidForProfession(player, stack)
                    ? WeaponCaseItem.getProfessionRestrictionMessageKey(stack)
                    : null;
        }

        if (stack.is(GameBusEvents.WARRIOR_TAG)
                && !ZhiyeData.has(player, Profession.WARRIOR)) return "zhiye.cant_use.0";
        if (stack.is(GameBusEvents.ARCHER_TAG)
                && !ZhiyeData.has(player, Profession.ARCHER)) return "zhiye.cant_use.1";
        if (stack.is(GameBusEvents.WARLOCK_TAG)
                && !ZhiyeData.has(player, Profession.WARLOCK)) return "zhiye.cant_use.2";
        return null;
    }

    public static boolean hasInvalidEquippedItem(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offhand = player.getOffhandItem();
        if ((!isHandCarryExempt(mainHand) && isInvalidForProfession(player, mainHand))
                || (!isHandCarryExempt(offhand) && isInvalidForProfession(player, offhand))) {
            return true;
        }

        for (ItemStack stack : player.getArmorSlots()) {
            if (isInvalidForProfession(player, stack)) return true;
        }

        return CuriosApi.getCuriosInventory(player).map(handler -> {
            var equipped = handler.getEquippedCurios();
            for (int slot = 0; slot < equipped.getSlots(); slot++) {
                if (isInvalidForProfession(player, equipped.getStackInSlot(slot))) return true;
            }
            return false;
        }).orElse(false);
    }

    /** Returns true when the current action must be rejected. */
    public static boolean shouldBlockActions(Player player) {
        if (player.hasEffect(ModEffects.freeze)) return true;
        if (!hasInvalidEquippedItem(player)) return false;

        applyRejectionFreeze(player);
        return true;
    }

    public static void applyRejectionFreeze(Player player) {
        player.stopUsingItem();
        player.addEffect(new MobEffectInstance(
                ModEffects.freeze, REJECTION_FREEZE_TICKS, 0, false, false, false));
    }
}
