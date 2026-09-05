package icu.icuqalt10.panlingre.item.common;

import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.attachment.ZhiyeData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.component.WeaponCaseContents;
import icu.icuqalt10.panlingre.init.ModComponents;
import icu.icuqalt10.panlingre.item.skill_trigger;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import icu.icuqalt10.panlingre.world.inventory.WeaponCaseMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;

public class WeaponCaseItem extends Item implements ICurioItem, skill_trigger {
    public static final String CURIO_SLOT = "fabao";
    private static final String ITEM_PACKAGE = "icu.icuqalt10.panlingre.item.";

    public WeaponCaseItem() {
        super(new Properties().stacksTo(1).fireResistant());
    }

    public static WeaponCaseContents getContents(ItemStack stack) {
        return stack.getOrDefault(ModComponents.WEAPON_CASE_CONTENTS.get(), WeaponCaseContents.EMPTY);
    }

    public static void setContents(ItemStack stack, WeaponCaseContents contents) {
        if (contents.entries().isEmpty()) {
            stack.remove(ModComponents.WEAPON_CASE_CONTENTS.get());
        } else {
            stack.set(ModComponents.WEAPON_CASE_CONTENTS.get(), contents);
        }
    }

    public static boolean isEligibleWeapon(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof skill_trigger
                && professionOf(stack) != null;
    }

    public static boolean canStore(Player player, ItemStack caseStack, ItemStack candidate) {
        Profession candidateProfession = professionOf(candidate);
        if (candidateProfession == null || !candidateProfession.matches(player)) return false;

        for (WeaponCaseContents.Entry entry : getContents(caseStack).entries()) {
            Profession storedProfession = professionOf(entry.stack());
            if (storedProfession != null && storedProfession != candidateProfession) return false;
        }
        return true;
    }

    public static boolean canUseStored(Player player, ItemStack storedStack) {
        Profession profession = professionOf(storedStack);
        return profession != null && profession.matches(player);
    }

    /** A filled case inherits the profession requirement of every weapon stored inside it. */
    public static boolean isInvalidForProfession(Player player, ItemStack caseStack) {
        return getContents(caseStack).entries().stream().anyMatch(entry -> {
            Profession profession = professionOf(entry.stack());
            return profession == null || !profession.matches(player);
        });
    }

    public static @Nullable String getProfessionRestrictionMessageKey(ItemStack caseStack) {
        Profession profession = caseProfession(caseStack);
        return profession == null ? null : profession.cantUseMessageKey;
    }

    public static boolean canProvideSkills(Player player, ItemStack caseStack) {
        List<WeaponCaseContents.Entry> entries = getContents(caseStack).entries();
        return !entries.isEmpty()
                && entries.stream().allMatch(entry -> canUseStored(player, entry.stack()));
    }

    private static @Nullable Profession professionOf(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof skill_trigger)) return null;
        String packageName = stack.getItem().getClass().getPackageName();
        if (packageName.equals(ITEM_PACKAGE + "warrior")
                || packageName.startsWith(ITEM_PACKAGE + "warrior.")) return Profession.WARRIOR;
        if (packageName.equals(ITEM_PACKAGE + "archer")
                || packageName.startsWith(ITEM_PACKAGE + "archer.")) return Profession.ARCHER;
        if (packageName.equals(ITEM_PACKAGE + "warlock")
                || packageName.startsWith(ITEM_PACKAGE + "warlock.")) return Profession.WARLOCK;
        return null;
    }

    private static Profession caseProfession(ItemStack caseStack) {
        for (WeaponCaseContents.Entry entry : getContents(caseStack).entries()) {
            Profession profession = professionOf(entry.stack());
            if (profession != null) return profession;
        }
        return null;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return !(slotContext.entity() instanceof Player player)
                || !isInvalidForProfession(player, stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack caseStack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            int sourceSlot = hand == InteractionHand.MAIN_HAND
                    ? player.getInventory().selected
                    : Inventory.SLOT_OFFHAND;
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (containerId, inventory, ignored) ->
                                    new WeaponCaseMenu(containerId, inventory, caseStack, sourceSlot),
                            caseStack.getHoverName()
                    ),
                    buffer -> buffer.writeVarInt(sourceSlot)
            );
        }
        return InteractionResultHolder.sidedSuccess(caseStack, level.isClientSide());
    }

    @Override
    public boolean skill_use(Level level, Player player, ItemStack stack, int skillIndex) {
        SkillRef ref = getSkillRef(stack, skillIndex);
        if (ref == null || !canUseStored(player, ref.skillStack)) return false;

        ItemStack original = ref.skillStack.copyWithCount(1);
        boolean succeeded = ref.trigger.skill_use(
                level, player, ref.skillStack, ref.nestedIndex);
        if (succeeded && !level.isClientSide) {
            ItemStack updated = ref.skillStack.isEmpty()
                    ? original
                    : ref.skillStack.copyWithCount(1);
            setContents(stack, getContents(stack).set(ref.slot, updated));
        }
        return succeeded;
    }

    @Override
    public boolean canUse(Level level, Player player, ItemStack stack, int skillIndex) {
        SkillRef ref = getSkillRef(stack, skillIndex);
        return ref != null && canUseStored(player, ref.skillStack)
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
        return ref == null ? 0.0F : ref.trigger.getSkillLingQiCost(ref.skillStack, ref.nestedIndex);
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
        return ref == null ? stack : ref.trigger.getSkillDisplayStack(ref.skillStack, ref.nestedIndex);
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

    private static @Nullable SkillRef getSkillRef(ItemStack caseStack, int skillIndex) {
        List<SkillRef> refs = getSkillRefs(caseStack);
        return skillIndex >= 0 && skillIndex < refs.size() ? refs.get(skillIndex) : null;
    }

    private static List<SkillRef> getSkillRefs(ItemStack caseStack) {
        List<SkillRef> refs = new ArrayList<>();
        for (WeaponCaseContents.Entry entry : getContents(caseStack).entries()) {
            ItemStack skillStack = entry.stack().copyWithCount(1);
            if (!(skillStack.getItem() instanceof skill_trigger trigger)) continue;
            int count = Math.max(0, trigger.getSkillCount(skillStack));
            for (int nestedIndex = 0; nestedIndex < count; nestedIndex++) {
                refs.add(new SkillRef(entry.slot(), trigger, skillStack, nestedIndex));
            }
        }
        return refs;
    }

    private enum Profession {
        WARRIOR(ZhiyeData.Profession.WARRIOR, "item.PanlingRE.lore.limit0", "zhiye.cant_use.0"),
        ARCHER(ZhiyeData.Profession.ARCHER, "item.PanlingRE.lore.limit1", "zhiye.cant_use.1"),
        WARLOCK(ZhiyeData.Profession.WARLOCK, "item.PanlingRE.lore.limit2", "zhiye.cant_use.2");

        private final ZhiyeData.Profession profession;
        private final String limitLoreKey;
        private final String cantUseMessageKey;

        Profession(ZhiyeData.Profession profession, String limitLoreKey, String cantUseMessageKey) {
            this.profession = profession;
            this.limitLoreKey = limitLoreKey;
            this.cantUseMessageKey = cantUseMessageKey;
        }

        boolean matches(Player player) {
            return ZhiyeData.has(player, profession);
        }
    }

    private record SkillRef(int slot, skill_trigger trigger,
                            ItemStack skillStack, int nestedIndex) {}

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {

        tooltip.add(Component.translatable("item.PanlingRE.lore.rare6"));

        //职业限制
        Profession profession = caseProfession(stack);
        tooltip.add(Component.translatable(profession == null
                ? "item.PanlingRE.lore.limit3"
                : profession.limitLoreKey));

        // 检测Shift键
        if (SafeClientAccess.isShiftPressed()) {
            tooltip.add(Component.translatable("item.PanlingRE.wu_qi_xia.lore1"));
            tooltip.add(Component.translatable("item.PanlingRE.wu_qi_xia.lore2"));
            tooltip.add(Component.translatable("item.PanlingRE.wu_qi_xia.lore3"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.wu_qi_xia.skill1.2"));
            tooltip.add(Component.translatable("item.PanlingRE.wu_qi_xia.skill2"));
            tooltip.add(Component.translatable("item.PanlingRE.wu_qi_xia.skill3"));
        } else {
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.wu_qi_xia.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }
}
