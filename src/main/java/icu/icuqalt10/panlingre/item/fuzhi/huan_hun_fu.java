package icu.icuqalt10.panlingre.item.fuzhi;

import icu.icuqalt10.panlingre.attachment.ItemUnlockData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModItems;
import icu.icuqalt10.panlingre.item.RecipeLockedItem;
import icu.icuqalt10.panlingre.network.HuanHunEffectsPayload;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

/** Passive talisman; all copies and bags share the player's saved cooldown. */
public class huan_hun_fu extends FuZhiItem implements RecipeLockedItem {
    public static final int COOLDOWN_TICKS = 1800 * 20;

    public huan_hun_fu() {
        super(COOLDOWN_TICKS, 0, 0, 0, "huan_hun_fu");
    }

    @Override
    public int getSkillCount() { return 0; }

    @Override
    public boolean skill_use(Level level, Player player, ItemStack stack, int skillIndex) { return false; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    protected void applyEffect(Level level, Player player) {
        player.removeEffectsCuredBy(EffectCures.PROTECTED_BY_TOTEM);
        player.setHealth(player.getMaxHealth() * 0.15F);
        float absorption = player.getMaxHealth() * 0.5F;
        // Vanilla absorption capacity comes in groups of four; set the actual shield precisely.
        int amplifier = Math.max(0, (int) Math.ceil(absorption / 4.0F) - 1);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, amplifier));
        player.setAbsorptionAmount(absorption);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 1));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new HuanHunEffectsPayload(player.getId()));
    }

    /** Called only after vanilla failed to find a usable totem. */
    public static boolean tryProtect(ServerPlayer player, DamageSource damageSource) {
        huan_hun_fu item = (huan_hun_fu) ModItems.huan_hun_fu.get();
        if (damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || !ItemUnlockData.canUse(player, item)
                || player.getCooldowns().isOnCooldown(item)
                || System.currentTimeMillis() < player.getData(ModAttachments.HUAN_HUN_COOLDOWN)) return false;

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(item) && CommonHooks.onLivingUseTotem(player, damageSource, stack, hand)) {
                item.protect(player, stack.copyWithCount(1));
                stack.shrink(1);
                return true;
            }
        }

        for (InteractionHand hand : InteractionHand.values()) {
            if (item.tryBag(player, damageSource, player.getItemInHand(hand))) return true;
        }

        return CuriosApi.getCuriosInventory(player).map(handler -> {
            var slots = handler.getStacksHandler(FuZhiBagItem.CURIO_SLOT).orElse(null);
            if (slots == null) return false;
            for (int slot = 0; slot < slots.getStacks().getSlots(); slot++) {
                if (handler.isSlotActive(FuZhiBagItem.CURIO_SLOT, slot)
                        && item.tryBag(player, damageSource, slots.getStacks().getStackInSlot(slot))) return true;
            }
            return false;
        }).orElse(false);
    }

    private boolean tryBag(ServerPlayer player, DamageSource source, ItemStack bag) {
        if (!(bag.getItem() instanceof FuZhiBagItem)) return false;
        var contents = FuZhiBagItem.getContents(bag);
        var entry = contents.find(BuiltInRegistries.ITEM.getKey(this)).orElse(null);
        if (entry == null) return false;
        ItemStack talisman = new ItemStack(this);
        if (!CommonHooks.onLivingUseTotem(player, source, talisman, InteractionHand.MAIN_HAND)) return false;
        protect(player, talisman);
        FuZhiBagItem.setContents(bag, contents.remove(entry.slot(), 1));
        return true;
    }

    private void protect(ServerPlayer player, ItemStack used) {
        long cooldownMillis = Math.max(0L, cooldown_remove.skill_cd_remove(player, COOLDOWN_TICKS * 50L));
        player.setData(ModAttachments.HUAN_HUN_COOLDOWN, System.currentTimeMillis() + cooldownMillis);
        player.getCooldowns().addCooldown(this, (int) ((cooldownMillis + 49L) / 50L));
        player.awardStat(Stats.ITEM_USED.get(this));
        CriteriaTriggers.USED_TOTEM.trigger(player, used);
        player.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
        applyEffect(player.level(), player);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltip, TooltipFlag flag) {

        // 检测Shift键
        if (SafeClientAccess.isShiftPressed()) {
            tooltip.add(Component.translatable("item.PanlingRE.lore.rare6"));
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit3"));
            tooltip.add(Component.translatable("item.PanlingRE.huan_hun_fu.lore1"));
            tooltip.add(Component.translatable("item.PanlingRE.huan_hun_fu.lore2"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.huan_hun_fu.skill1.2",
                    cooldown_remove.getCooldownText(SafeClientAccess.getClientPlayer(), cooldown)
            ));
            tooltip.add(Component.translatable("item.PanlingRE.huan_hun_fu.skill2"));
            tooltip.add(Component.translatable("item.PanlingRE.huan_hun_fu.skill3"));
            tooltip.add(Component.translatable("item.PanlingRE.huan_hun_fu.skill4"));
            tooltip.add(Component.translatable("item.PanlingRE.huan_hun_fu.skill5"));
        } else {
            tooltip.add(Component.translatable("item.PanlingRE.lore.rare6"));
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit3"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.huan_hun_fu.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }
}
