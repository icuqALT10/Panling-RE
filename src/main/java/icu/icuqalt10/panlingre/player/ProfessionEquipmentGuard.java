package icu.icuqalt10.panlingre.player;

import icu.icuqalt10.panlingre.event.GameBusEvents;
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

    public static @Nullable String getRestrictionMessageKey(Player player, ItemStack stack) {
        if (stack.isEmpty()) return null;

        if (stack.getItem() instanceof WeaponCaseItem) {
            return WeaponCaseItem.isInvalidForProfession(player, stack)
                    ? WeaponCaseItem.getProfessionRestrictionMessageKey(stack)
                    : null;
        }

        if (stack.is(GameBusEvents.WARRIOR_TAG)
                && !check.zhiye_check(player, "panlingre:warrior")) return "zhiye.cant_use.0";
        if (stack.is(GameBusEvents.ARCHER_TAG)
                && !check.zhiye_check(player, "panlingre:archer")) return "zhiye.cant_use.1";
        if (stack.is(GameBusEvents.WARLOCK_TAG)
                && !check.zhiye_check(player, "panlingre:warlock")) return "zhiye.cant_use.2";
        return null;
    }

    public static boolean hasInvalidEquippedItem(Player player) {
        if (isInvalidForProfession(player, player.getMainHandItem())
                || isInvalidForProfession(player, player.getOffhandItem())) {
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
