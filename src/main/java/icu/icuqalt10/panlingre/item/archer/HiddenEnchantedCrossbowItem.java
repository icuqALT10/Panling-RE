package icu.icuqalt10.panlingre.item.archer;

import icu.icuqalt10.panlingre.init.ModComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

/** A crossbow with built-in enchantments that are omitted from its tooltip. */
public abstract class HiddenEnchantedCrossbowItem extends CrossbowItem {
    private final int quickChargeLevel;
    private final int multishotLevel;

    protected HiddenEnchantedCrossbowItem(Properties properties, int quickChargeLevel, int multishotLevel) {
        super(properties);
        this.quickChargeLevel = quickChargeLevel;
        this.multishotLevel = multishotLevel;
    }

    protected boolean hasBuiltInEnchantments(ItemStack stack) {
        return true;
    }

    protected final void syncBuiltInEnchantments(ItemStack stack, Level level) {
        var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var quickCharge = registry.getOrThrow(Enchantments.QUICK_CHARGE);
        var multishot = registry.getOrThrow(Enchantments.MULTISHOT);
        ItemEnchantments current = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(current);
        boolean enabled = hasBuiltInEnchantments(stack);

        if (quickChargeLevel > 0) {
            mutable.set(quickCharge, enabled ? quickChargeLevel : 0);
        }
        if (multishotLevel > 0) {
            mutable.set(multishot, enabled ? multishotLevel : 0);
        }
        ItemEnchantments desired = mutable.toImmutable().withTooltip(false);
        if (!desired.equals(current)) {
            stack.set(DataComponents.ENCHANTMENTS, desired);
        }
        stack.set(ModComponents.HIDDEN_ENCHANTMENTS_INITIALIZED.get(), true);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide
                && !stack.getOrDefault(ModComponents.HIDDEN_ENCHANTMENTS_INITIALIZED.get(), false)) {
            syncBuiltInEnchantments(stack, level);
        }
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }
}
