package icu.icuqalt10.panlingre.item.other;

import icu.icuqalt10.panlingre.init.ModComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class LootKeyItem extends Item {
    public LootKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        String keyType = stack.getOrDefault(ModComponents.KEY_TYPE.get(), "golden");
        String keyId = stack.getOrDefault(ModComponents.KEY_ID.get(), "");

        String baseKey = "item.panlingre.loot_key." + keyType;
        if (keyId.isEmpty()) {
            return Component.translatable(baseKey, Component.empty());
        }
        Component dungeonName = Component.translatable("plre.loot_chest.instance_name." + keyId);
        return Component.translatable(baseKey, dungeonName);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String keyType = stack.getOrDefault(ModComponents.KEY_TYPE.get(), "golden");
        tooltip.add(Component.translatable("item.panlingre.loot_key.lore1"));
        tooltip.add(Component.translatable("item.panlingre.loot_key.lore2." + keyType));
        tooltip.add(Component.translatable("item.panlingre.loot_key.lore3"));
    }
}
