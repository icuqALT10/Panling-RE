package icu.icuqalt10.panlingre.item.warlock;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.item.skill_1_key;
import icu.icuqalt10.panlingre.item.skill_2_key;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class qi_sha_din extends Item implements ICurioItem,skill_1_key, skill_2_key {

    public qi_sha_din() {
        super(
                new Properties()
                        .stacksTo(1)
                        .fireResistant()
        );
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = HashMultimap.create();
        ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "qi_sha_din");

        modifiers.put(ModAttributes.MAGIC_DAMAGE, new AttributeModifier(
                UID,
                25,
                AttributeModifier.Operation.ADD_VALUE
        ));
        modifiers.put(ModAttributes.FALIZHI, new AttributeModifier(
                UID,
                20,
                AttributeModifier.Operation.ADD_VALUE
        ));
        modifiers.put(ModAttributes.MAX_LINGQI, new AttributeModifier(
                UID,
                20,
                AttributeModifier.Operation.ADD_VALUE
        ));

        return modifiers;
    }

    //技能 skill_1
    @Override
    public boolean skill_1_trigger(Level level, Player player, ItemStack stack) {

        LingQiData data = player.getData(ModAttachments.LINGQI);
        float cost = 20f;
        //如果灵气不足
        if (!data.consume(player,cost)) return false;
        //释放技能
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            LivingEntity target = findAlchemistTarget(serverPlayer, 5.0);

            if (target != null) {

                float attack_damage = (float) (player.getAttributeValue(ModAttributes.MAGIC_DAMAGE) * 3);
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

                Vec3 targetDest = target.getBoundingBox().getCenter();

                //绘制法术连线粒子
                drawSpellLine(serverPlayer, furnaceSource, targetDest);

            }
            //cd
            cooldown_remove.cd_remove(player,this,20);
            //音效
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.5f,1.0f);
            //播报
            player.displayClientMessage(Component.translatable("item.PanlingRE.qi_sha_din.skill.success"), true);
        }

        return true;
    }

    private static LivingEntity findAlchemistTarget(ServerPlayer player, double range) {
        Level level = player.level();
        // 找出范围内所有的生物
        AABB searchBox = player.getBoundingBox().inflate(range);
        List<Mob> entities = level.getEntitiesOfClass(Mob.class, searchBox, entity -> {
            return entity.isAttackable() && entity.isAlive();
        });

        LivingEntity closest = null;
        double closestScore = Double.MAX_VALUE;
        Vec3 lookVec = player.getLookAngle().normalize();

        for (Mob mob : entities) {
            Vec3 toMob = mob.position().add(0, mob.getEyeHeight(), 0).subtract(player.getEyePosition());
            double dist = toMob.length();
            toMob = toMob.normalize();

            double dotProduct = lookVec.dot(toMob);

            if (dotProduct > 0.85) {
                double score = dist * (2.0 - dotProduct);
                if (score < closestScore) {
                    closestScore = score;
                    closest = mob;
                }
            }
        }
        return closest;
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
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {

        // 检测Shift键
        if (SafeClientAccess.isShiftPressed()) {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare5"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.qi_sha_din.lore1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.qi_sha_din.lore2"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.qi_sha_din.skill1.2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.qi_sha_din.skill2"
                    ,Component.keybind("key.PanlingRE.skill_1").withStyle(ChatFormatting.GOLD)));
            tooltipComponents.add(Component.translatable("item.PanlingRE.qi_sha_din.skill3"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ldl.skill1.2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ldl.skill2"
                    ,Component.keybind("key.PanlingRE.skill_2").withStyle(ChatFormatting.GOLD)));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ldl.skill3"));
        } else {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare5"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit2"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.qi_sha_din.skill1.1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.ldl.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltipComponents, flag);
    }

    @Override
    public boolean skill_2_trigger(Level level, Player player, ItemStack stack) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider((id, inv, p) ->
                            new ldlMenu(id, inv, ContainerLevelAccess.NULL),
                            Component.translatable("block.panlingre.ldl")),
                    buf -> buf.writeBlockPos(player.blockPosition()));

            cooldown_remove.cd_remove(player, this, 20);
        }
        return true;
    }
}
