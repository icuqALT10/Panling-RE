package icu.icuqalt10.panlingre.item.common;

import icu.icuqalt10.panlingre.item.RecipeLockedItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class SpawnerBoostItem extends Item {
    public static final int RADIUS = 16;
    public static final double SPAWN_FREQUENCY_MULTIPLIER = 2.0;
    public static final double SPAWN_COUNT_MULTIPLIER = 2.0;

    private final int radius;
    private final double frequencyMultiplier;
    private final double countMultiplier;

    public SpawnerBoostItem() {
        this(RADIUS, SPAWN_FREQUENCY_MULTIPLIER, SPAWN_COUNT_MULTIPLIER);
    }

    // New variants only need their own three constants and a call to this constructor.
    protected SpawnerBoostItem(int radius, double frequencyMultiplier, double countMultiplier) {
        super(new Properties().stacksTo(1));
        this.radius = radius;
        this.frequencyMultiplier = frequencyMultiplier;
        this.countMultiplier = countMultiplier;
    }

    public int getRadius() {
        return radius;
    }

    public double getFrequencyMultiplier() {
        return frequencyMultiplier;
    }

    public double getCountMultiplier() {
        return countMultiplier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.PanlingRE.lore.rare6"));
        tooltip.add(Component.translatable("item.PanlingRE.lore.limit3"));
        tooltip.add(Component.translatable("item.PanlingRE.spawner_boost.lore1"));
        tooltip.add(Component.translatable("item.PanlingRE.spawner_boost.lore2"));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("item.PanlingRE.spawner_boost.skill1"));
        tooltip.add(Component.translatable("item.PanlingRE.spawner_boost.skill2", radius));
        tooltip.add(Component.translatable("item.PanlingRE.spawner_boost.skill3", frequencyMultiplier, countMultiplier));
        tooltip.add(Component.translatable("item.PanlingRE.spawner_boost.skill4"));
    }
}
