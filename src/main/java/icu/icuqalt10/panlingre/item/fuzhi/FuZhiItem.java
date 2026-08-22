package icu.icuqalt10.panlingre.item.fuzhi;

import icu.icuqalt10.panlingre.item.skill_trigger;
import icu.icuqalt10.panlingre.network.SkillWheelPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Arrays;

/** Common hand-use and skill-wheel behavior for consumable talismans. */
public abstract class FuZhiItem extends Item implements skill_trigger {
    protected final int cooldown;
    protected final float cost;
    protected final int castTimeTicks;

    private final double falizhiBonus;
    private final String skillNameKey;
    private final String[] skillDescriptions;

    protected FuZhiItem(int cooldown, float cost, int castTimeTicks, double falizhiBonus,
                        String translationId, int... descriptionLines) {
        super(new Properties().stacksTo(64).fireResistant());
        this.cooldown = cooldown;
        this.cost = cost;
        this.castTimeTicks = Math.max(0, castTimeTicks);
        this.falizhiBonus = falizhiBonus;
        this.skillNameKey = "item.PanlingRE." + translationId + ".skill1.2";
        this.skillDescriptions = Arrays.stream(descriptionLines)
                .mapToObj(line -> "item.PanlingRE." + translationId + ".skill" + line)
                .toArray(String[]::new);
    }

    public final double getFalizhiBonus() {
        return falizhiBonus;
    }

    /** Applies only the effect. Resource cost, consumption, and cooldown are handled by the caller. */
    protected abstract void applyEffect(Level level, Player player);

    private boolean cast(Level level, Player player) {
        if (!level.isClientSide) {
            applyEffect(level, player);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5f, 1.0f);
        }
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            SkillWheelPayload.requestHandUse(
                    serverPlayer,
                    BuiltInRegistries.ITEM.getKey(stack.getItem()),
                    0,
                    hand
            );
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean skill_use(Level level, Player player, ItemStack stack, int skillIndex) {
        if (skillIndex != 0) return false;
        boolean succeeded = cast(level, player);
        if (succeeded && !level.isClientSide) stack.consume(1, player);
        return succeeded;
    }

    @Override
    public long getSkillCD(int skillIndex) {
        return skillIndex == 0 ? cooldown * 50L : 0L;
    }

    @Override
    public int getSkillCastTimeTicks(int skillIndex) {
        return skillIndex == 0 ? castTimeTicks : 0;
    }

    @Override
    public String getSkillNameKey(int skillIndex) {
        return skillIndex == 0 ? skillNameKey : "";
    }

    @Override
    public float getSkillLingQiCost(int skillIndex) {
        return skillIndex == 0 ? cost : 0f;
    }

    @Override
    public String[] getSkillDescription(int skillIndex) {
        return skillIndex == 0 ? skillDescriptions.clone() : null;
    }

    @Override
    public Item getSkillCooldownItem(ItemStack stack, int skillIndex) {
        return skillIndex == 0 ? this : null;
    }
}
