package icu.icuqalt10.panlingre.item.other;

import icu.icuqalt10.panlingre.player.check;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class yao_yin extends Item {

    public yao_yin() {
        super(
                new Item.Properties()
                        .stacksTo(64)
                        .fireResistant()
        );
    }

    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        if (FMLEnvironment.dist == Dist.CLIENT) {

            Player player = Minecraft.getInstance().player;
            if (player != null && check.race_check(player, "panlingre:race_yao")) {
                tooltipComponents.add(Component.translatable("item.panlingre.yao_yin.lore2"));
            } else {
                tooltipComponents.add(Component.translatable("item.panlingre.yao_yin.lore1.1"));
                tooltipComponents.add(Component.translatable("item.panlingre.yao_yin.lore1.2"));
            }

        }

        super.appendHoverText(stack, context, tooltipComponents, flag);
    }

}
