package icu.icuqalt10.panlingre.item.warlock;

import icu.icuqalt10.panlingre.util.SkillTargeting;
import icu.icuqalt10.panlingre.attachment.LingQiData;

import icu.icuqalt10.panlingre.attribute.cooldown_remove;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.item.skill_trigger;
import icu.icuqalt10.panlingre.item.liandan;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import icu.icuqalt10.panlingre.world.inventory.ldlMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class suo_hun_lu extends Item implements ICurioItem,skill_trigger, liandan {

    private final int cooldown = 20;
    private final float cost = 10.0f;

    public suo_hun_lu() {
        super(
                new Properties()
                        .stacksTo(1)
                        .fireResistant()
        );
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = HashMultimap.create();
        ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "suo_hun_lu");

        modifiers.put(ModAttributes.MAGIC_DAMAGE, new AttributeModifier(
                UID,
                20,
                AttributeModifier.Operation.ADD_VALUE
        ));
        modifiers.put(ModAttributes.FALIZHI, new AttributeModifier(
                UID,
                15,
                AttributeModifier.Operation.ADD_VALUE
        ));
        modifiers.put(ModAttributes.MAX_LINGQI, new AttributeModifier(
                UID,
                15,
                AttributeModifier.Operation.ADD_VALUE
        ));

        modifiers.put(ModAttributes.COOLDOWN_REMOVE, new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "huang_tong_lu"),
                0.15,
                AttributeModifier.Operation.ADD_VALUE
        ));

        return modifiers;
    }

    //技能 skill_1
    @Override
    public boolean skill_use(Level level, Player player, ItemStack stack, int skillIndex) {

        //释放技能
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            SkillTargeting.Target target = findAlchemistTarget(serverPlayer, 5.0);

            if (target != null) {

                float attack_damage = (float) (player.getAttributeValue(ModAttributes.MAGIC_DAMAGE) *1.5);
                target.hurt(player.damageSources().indirectMagic(player,player), attack_damage);

                Vec3 playerPos = player.position().add(0, player.getEyeHeight(), 0);

                double localLeft = 0.95;
                double localBack = 0.75;
                double localUp = 0.55;

                float f = player.getYRot() * ((float)Math.PI / 180F);
                double sin = Math.sin(f);
                double cos = Math.cos(f);

                double worldX = localLeft * cos + localBack * sin;
                double worldZ = localLeft * sin - localBack * cos;

                Vec3 furnaceSource = playerPos.add(worldX, localUp, worldZ);

                Vec3 targetDest = target.point();

                //绘制法术连线粒子
                drawSpellLine(serverPlayer, furnaceSource, targetDest);

            }
            //音效
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.5f,1.0f);
            //播报
            player.displayClientMessage(Component.translatable("item.PanlingRE.suo_hun_lu.skill.success"), true);
        }

        return true;
    }

    @Override
    public long getSkillCD(int skillIndex) {
        return cooldown * 50L;
    }

    @Override
    public String getSkillNameKey(int skillIndex) {
        return "item.PanlingRE.suo_hun_lu.skill1.2";
    }

    @Override
    public float getSkillLingQiCost(int skillIndex) {
        return cost;
    }

    @Override
    public String[] getSkillDescription(int skillIndex) {
        return new String[]{
                "item.PanlingRE.suo_hun_lu.skill3"
        };
    }

    @Override
    public int getSkillCastTimeTicks(int skillIndex) { return 4; }

    private static SkillTargeting.Target findAlchemistTarget(ServerPlayer player, double range) {
        return SkillTargeting.aimed(player, player.getBoundingBox().inflate(range),
                        player.getEyePosition(), player.getLookAngle(), range, Math.toDegrees(Math.acos(.85)))
                .stream().filter(target -> target.root() instanceof Mob).findFirst().orElse(null);
    }
    private static void drawSpellLine(ServerPlayer player, Vec3 source, Vec3 dest) {
        ServerLevel serverLevel = player.serverLevel();
        Vec3 direction = dest.subtract(source);
        double distance = direction.length();
        Vec3 step = direction.normalize().scale(0.2);

        int numParticles = (int) (distance / 0.2);
        Vec3 current = source;

        for (int i = 0; i < numParticles; i++) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    current.x, current.y, current.z,
                    1,
                    0.0, 0.0, 0.0,
                    0.0 // 速度
            );
            current = current.add(step);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltip, TooltipFlag flag) {

        // 检测Shift键
        if (SafeClientAccess.isShiftPressed()) {
            tooltip.add(Component.translatable("item.PanlingRE.lore.rare4"));
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit2"));
            tooltip.add(Component.translatable("item.PanlingRE.suo_hun_lu.lore1"));
            tooltip.add(Component.translatable("item.PanlingRE.suo_hun_lu.lore2"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.suo_hun_lu.skill1.2"));
            tooltip.add(Component.translatable("item.PanlingRE.suo_hun_lu.skill2", cooldown_remove.getCooldownText(SafeClientAccess.getClientPlayer(), cooldown),
                    LingQiData.getCostText(cost)));
            tooltip.add(Component.translatable("item.PanlingRE.suo_hun_lu.skill3"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.ldl.skill1.2"
                    ,Component.keybind("key.PanlingRE.liandan").withStyle(ChatFormatting.GOLD)));
            tooltip.add(Component.translatable("item.PanlingRE.ldl.skill2"));
        } else {
            tooltip.add(Component.translatable("item.PanlingRE.lore.rare4"));
            tooltip.add(Component.translatable("item.PanlingRE.lore.limit2"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.suo_hun_lu.skill1.1"));
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.PanlingRE.ldl.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean liandan_trigger(Level level, Player player, ItemStack stack) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider((id, inv, p) ->
                            new ldlMenu(id, inv, ContainerLevelAccess.NULL),
                            Component.translatable("block.panlingre.ldl")),
                    buf -> buf.writeBlockPos(player.blockPosition()));
        }
        return true;
    }
}
