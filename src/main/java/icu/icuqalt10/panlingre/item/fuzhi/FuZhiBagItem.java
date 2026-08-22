package icu.icuqalt10.panlingre.item.fuzhi;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.component.FuZhiBagContents;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.init.ModComponents;
import icu.icuqalt10.panlingre.item.skill_trigger;
import icu.icuqalt10.panlingre.world.inventory.FuZhiBagMenu;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;

public class FuZhiBagItem extends Item implements ICurioItem, skill_trigger {
    public static final String CURIO_SLOT = "fabao";
    public static final TagKey<Item> FU_ZHI_TAG = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "fuzhi"));

    public FuZhiBagItem() {
        super(new Properties().stacksTo(1).fireResistant());
    }

    public static FuZhiBagContents getContents(ItemStack stack) {
        return stack.getOrDefault(ModComponents.FU_ZHI_BAG_CONTENTS.get(), FuZhiBagContents.EMPTY);
    }

    public static void setContents(ItemStack stack, FuZhiBagContents contents) {
        if (contents.entries().isEmpty()) {
            stack.remove(ModComponents.FU_ZHI_BAG_CONTENTS.get());
        } else {
            stack.set(ModComponents.FU_ZHI_BAG_CONTENTS.get(), contents);
        }
    }

    public static boolean isFuZhi(ItemStack stack) {
        return !stack.isEmpty() && stack.is(FU_ZHI_TAG);
    }

    /** Counts each stored talisman type once; stored quantity does not multiply the bonus. */
    public static double getFalizhiBonus(ItemStack stack) {
        double total = 0.0D;
        for (FuZhiBagContents.Entry entry : getContents(stack).entries()) {
            Item item = BuiltInRegistries.ITEM.get(entry.itemId());
            if (item instanceof FuZhiItem fuZhiItem) {
                total += fuZhiItem.getFalizhiBonus();
            }
        }
        return total;
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = HashMultimap.create();
        double falizhiBonus = getFalizhiBonus(stack);
        if (falizhiBonus > 0.0D) {
            modifiers.put(ModAttributes.FALIZHI, new AttributeModifier(
                    id,
                    falizhiBonus,
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }
        return modifiers;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack bagStack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            int sourceSlot = hand == InteractionHand.MAIN_HAND
                    ? player.getInventory().selected
                    : Inventory.SLOT_OFFHAND;
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (containerId, inventory, ignored) ->
                                    new FuZhiBagMenu(containerId, inventory, bagStack, sourceSlot),
                            bagStack.getHoverName()
                    ),
                    buffer -> buffer.writeVarInt(sourceSlot)
            );
        }
        return InteractionResultHolder.sidedSuccess(bagStack, level.isClientSide());
    }

    @Override
    public boolean skill_use(Level level, Player player, ItemStack stack, int skillIndex) {
        SkillRef ref = getSkillRef(stack, skillIndex);
        if (ref == null) return false;

        boolean succeeded = ref.trigger.skill_use(level, player, ref.skillStack, ref.nestedIndex);
        if (succeeded && !level.isClientSide) {
            FuZhiBagContents contents = getContents(stack);
            setContents(stack, contents.remove(ref.entry.slot(), 1));
        }
        return succeeded;
    }

    @Override
    public boolean canUse(Level level, Player player, ItemStack stack, int skillIndex) {
        SkillRef ref = getSkillRef(stack, skillIndex);
        return ref != null && ref.entry.count() > 0
                && ref.trigger.canUse(level, player, ref.skillStack, ref.nestedIndex);
    }

    @Override
    public int getSkillCount(ItemStack stack) {
        return getSkillRefs(stack).size();
    }

    @Override
    public long getSkillCD(ItemStack stack, int skillIndex) {
        SkillRef ref = getSkillRef(stack, skillIndex);
        return ref == null ? 0L : ref.trigger.getSkillCD(ref.skillStack, ref.nestedIndex);
    }

    @Override
    public int getSkillCastTimeTicks(ItemStack stack, int skillIndex) {
        SkillRef ref = getSkillRef(stack, skillIndex);
        return ref == null ? 0 : ref.trigger.getSkillCastTimeTicks(ref.skillStack, ref.nestedIndex);
    }

    @Override
    public String getSkillNameKey(ItemStack stack, int skillIndex) {
        SkillRef ref = getSkillRef(stack, skillIndex);
        return ref == null ? "" : ref.trigger.getSkillNameKey(ref.skillStack, ref.nestedIndex);
    }

    @Override
    public float getSkillLingQiCost(ItemStack stack, int skillIndex) {
        SkillRef ref = getSkillRef(stack, skillIndex);
        return ref == null ? 0f : ref.trigger.getSkillLingQiCost(ref.skillStack, ref.nestedIndex);
    }

    @Override
    public @Nullable ResourceLocation getSkillIcon(ItemStack stack, int skillIndex) {
        SkillRef ref = getSkillRef(stack, skillIndex);
        return ref == null ? null : ref.trigger.getSkillIcon(ref.skillStack, ref.nestedIndex);
    }

    @Override
    public @Nullable String[] getSkillDescription(ItemStack stack, int skillIndex) {
        SkillRef ref = getSkillRef(stack, skillIndex);
        return ref == null ? null : ref.trigger.getSkillDescription(ref.skillStack, ref.nestedIndex);
    }

    @Override
    public ItemStack getSkillDisplayStack(ItemStack stack, int skillIndex) {
        SkillRef ref = getSkillRef(stack, skillIndex);
        return ref == null
                ? stack
                : ref.trigger.getSkillDisplayStack(ref.skillStack, ref.nestedIndex);
    }

    @Override
    public String getSkillCooldownKey(ItemStack stack, int skillIndex) {
        SkillRef ref = getSkillRef(stack, skillIndex);
        return ref == null
                ? skill_trigger.super.getSkillCooldownKey(stack, skillIndex)
                : ref.trigger.getSkillCooldownKey(ref.skillStack, ref.nestedIndex);
    }

    @Override
    public @Nullable Item getSkillCooldownItem(ItemStack stack, int skillIndex) {
        SkillRef ref = getSkillRef(stack, skillIndex);
        return ref == null ? null : ref.trigger.getSkillCooldownItem(ref.skillStack, ref.nestedIndex);
    }

    private static @Nullable SkillRef getSkillRef(ItemStack bagStack, int skillIndex) {
        List<SkillRef> refs = getSkillRefs(bagStack);
        return skillIndex >= 0 && skillIndex < refs.size() ? refs.get(skillIndex) : null;
    }

    private static List<SkillRef> getSkillRefs(ItemStack bagStack) {
        List<SkillRef> refs = new ArrayList<>();
        for (FuZhiBagContents.Entry entry : getContents(bagStack).entries()) {
            Item item = BuiltInRegistries.ITEM.get(entry.itemId());
            if (!(item instanceof skill_trigger trigger)) continue;

            ItemStack skillStack = new ItemStack(item);
            int count = Math.max(0, trigger.getSkillCount(skillStack));
            for (int nestedIndex = 0; nestedIndex < count; nestedIndex++) {
                refs.add(new SkillRef(entry, trigger, skillStack, nestedIndex));
            }
        }
        return refs;
    }

    private record SkillRef(FuZhiBagContents.Entry entry, skill_trigger trigger,
                            ItemStack skillStack, int nestedIndex) {}
}
