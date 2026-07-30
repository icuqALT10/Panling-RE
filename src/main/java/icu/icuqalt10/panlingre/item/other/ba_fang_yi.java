package icu.icuqalt10.panlingre.item.other;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.BaFangYiData;
import icu.icuqalt10.panlingre.data.ba_fang_yi.BaFangYiLoader;
import icu.icuqalt10.panlingre.data.ba_fang_yi.BaFangYiMajorEntry;
import icu.icuqalt10.panlingre.data.ba_fang_yi.BaFangYiSubEntry;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.network.BaFangYiOpenPayload;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class ba_fang_yi extends Item {
    public ba_fang_yi() {
        super(new Properties().stacksTo(1).fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BaFangYiData data = BaFangYiData.get(player);
            if (!data.isEnabled()) {
                serverPlayer.sendSystemMessage(
                        Component.translatable("command.panlingre.ba_fang_yi.disabled"),
                        true
                );
                return InteractionResultHolder.fail(itemstack);
            }

            List<BaFangYiMajorEntry> allMajors = BaFangYiLoader.loadAll(serverPlayer.serverLevel());
            List<BaFangYiOpenPayload.BaFangYiMajorPayload> majors = new ArrayList<>();
            for (BaFangYiMajorEntry major : allMajors) {
                if (data.isMajorUnlocked(major.id())) {
                    List<BaFangYiOpenPayload.BaFangYiSubPayload> subs = new ArrayList<>();
                    for (BaFangYiSubEntry sub : major.poses()) {
                        if (data.isSubUnlocked(major.id(), sub.id())) {
                            subs.add(new BaFangYiOpenPayload.BaFangYiSubPayload(
                                    sub.title(), sub.id(), sub.texture(), sub.x(), sub.y(), sub.z()
                            ));
                        }
                    }
                    majors.add(new BaFangYiOpenPayload.BaFangYiMajorPayload(
                            major.title(), major.id(), major.texture(), subs
                    ));
                }
            }
            PacketDistributor.sendToPlayer(serverPlayer, new BaFangYiOpenPayload(majors));
        }

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        if (SafeClientAccess.isShiftPressed()) {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit3"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ba_fang_yi.lore1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ba_fang_yi.lore2"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.ba_fang_yi.skill1.2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ba_fang_yi.skill2",
                    Component.keybind("key.use").withStyle(ChatFormatting.GOLD)));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ba_fang_yi.skill3"));
        } else {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare3"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit3"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.ba_fang_yi.skill1.1"));
        }
        super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}
