package icu.icuqalt10.panlingre.item.archer;

import icu.icuqalt10.panlingre.attachment.LingQiData;

import icu.icuqalt10.panlingre.attribute.cooldown_remove;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.entity.ZhuRiArrowEntity;
import icu.icuqalt10.panlingre.init.ModAttributes;
import icu.icuqalt10.panlingre.init.ModComponents;
import icu.icuqalt10.panlingre.item.skill_trigger;
import icu.icuqalt10.panlingre.util.SafeClientAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class zhu_ri extends HiddenEnchantedCrossbowItem implements skill_trigger {

    private static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "zhu_ri");

    private final int cooldown = 400;
    private final float cost = 50.0f;

    public zhu_ri() {
        super(
                new Properties()
                        .stacksTo(1)
                        .fireResistant(), 3, 1
        );
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        boolean isPowered = stack.getOrDefault(ModComponents.IS_POWERED.get(), false);

        builder.add(
                ModAttributes.ARROW_DAMAGE,
                new AttributeModifier(
                        MODIFIER_ID,
                        isPowered ? 25.0 : 35.0,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );
        builder.add(
                Attributes.MOVEMENT_SPEED,
                new AttributeModifier(
                        MODIFIER_ID,
                        isPowered ? -0.75 : 0.2,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ),
                EquipmentSlotGroup.MAINHAND
        );
        builder.add(
                Attributes.ARMOR,
                new AttributeModifier(
                        MODIFIER_ID,
                        isPowered ? 0.15 : -0.2,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ),
                EquipmentSlotGroup.MAINHAND
        );

        return builder.build();
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        boolean isPowered = stack.getOrDefault(ModComponents.IS_POWERED.get(), false);
        return isPowered ? 7200 : 72000;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            boolean isPowered = stack.getOrDefault(ModComponents.IS_POWERED.get(), false);
            if (isPowered) {
                poweredShoot(stack, level, player, timeLeft);
            } else {
                super.releaseUsing(stack, level, entity, timeLeft);
            }
        } else {
            super.releaseUsing(stack, level, entity, timeLeft);
        }
    }

    private void poweredShoot(ItemStack stack, Level level, Player player, int timeLeft) {
        ItemStack ammo = player.getProjectile(stack);
        if (ammo.isEmpty()) return;

        if (!(level instanceof ServerLevel serverLevel)) return;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        // Target locking
        LivingEntity lockedTarget = null;
        Vec3 targetPoint;

        // AABB: 8x8 cross-section, 100 blocks forward
        Vec3 endFar = eye.add(look.scale(100));
        double minX = Math.min(eye.x, endFar.x) - 4.0;
        double maxX = Math.max(eye.x, endFar.x) + 4.0;
        double minY = Math.min(eye.y, endFar.y) - 4.0;
        double maxY = Math.max(eye.y, endFar.y) + 4.0;
        double minZ = Math.min(eye.z, endFar.z) - 4.0;
        double maxZ = Math.max(eye.z, endFar.z) + 4.0;
        AABB scanBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, scanBox,
                e -> canLockTarget(player, e))) {
            Vec3 toE = e.position().subtract(eye);
            double proj = toE.dot(look);
            if (proj <= 0 || proj > 100) continue;
            Vec3 onRay = eye.add(look.scale(proj));
            double dist = e.position().distanceTo(onRay);
            if (dist <= 4.0 && proj < bestDist) {
                bestDist = proj;
                lockedTarget = e;
            }
        }

        if (lockedTarget != null) {
            targetPoint = lockedTarget.getEyePosition();
        } else {
            var hit = player.pick(30, 0, false);
            if (hit.getType() != HitResult.Type.MISS) {
                targetPoint = hit.getLocation();
            } else {
                targetPoint = eye.add(look.scale(30));
            }
        }

        // Start: close to player, down 0.5
        Vec3 P0 = eye.add(look.scale(0.8)).add(0, -0.5, 0);
        Vec3 P3 = targetPoint;
        Vec3 dir = P3.subtract(P0);
        double dist = dir.length();
        if (dist < 0.01) return;
        Vec3 fwd = dir.normalize();
        Vec3 hRight = fwd.cross(new Vec3(0, 1, 0));
        if (hRight.lengthSqr() < 0.01) hRight = fwd.cross(new Vec3(1, 0, 0));
        hRight = hRight.normalize();

        // Random azimuth angle 0-360
        double ang = level.random.nextDouble() * Math.PI * 2;
        Vec3 sideDir = hRight.scale(Math.cos(ang))
                .add(fwd.cross(hRight).scale(Math.sin(ang)));
        if (sideDir.lengthSqr() < 0.01) sideDir = hRight;
        sideDir = sideDir.normalize();

        // Random vertical bias: up, level, or down
        double vertSign = level.random.nextDouble() < 0.5 ? 1.0 : -1.0;
        double vertMag = dist * (0.15 + level.random.nextDouble() * 0.2);

        // P1: near start, random direction + random vertical
        double p1Dist = dist * 0.3;
        Vec3 P1 = P0.add(fwd.scale(p1Dist * 0.6))
                .add(sideDir.scale(p1Dist * 0.5))
                .add(0, vertSign * vertMag * 0.6, 0);

        // P2: near target, pull from random side + smaller vertical
        double horizOff2 = (level.random.nextDouble() - 0.5) * Math.min(dist * 0.3, 5.0);
        Vec3 P2 = P3.subtract(fwd.scale(dist * 0.25))
                .add(hRight.scale(horizOff2))
                .add(0, vertSign * vertMag * 0.3, 0);

        double arrowDmg = player.getAttributeValue(ModAttributes.ARROW_DAMAGE);

        ZhuRiArrowEntity arrow = new ZhuRiArrowEntity(level, player,
                P0, P1, P2, P3, arrowDmg, lockedTarget);
        serverLevel.addFreshEntity(arrow);
        if (tian_xing_jian.isSniperActive(player)) {
            tian_xing_jian.notifySniperShot(player);
        }

        if (!player.getAbilities().instabuild) {
            ammo.shrink(1);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F,
                1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + 0.5F);

        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
    }

    private boolean canLockTarget(Player player, LivingEntity target) {
        if (target == player || !target.isAlive() || !target.isAttackable() || target.isInvulnerable()) {
            return false;
        }
        if (target instanceof Player targetPlayer
                && (targetPlayer.isCreative() || targetPlayer.isSpectator())) {
            return false;
        }
        if (player.getTeam() != null && player.isAlliedTo(target)) {
            return false;
        }
        return !target.isInvulnerableTo(player.damageSources().playerAttack(player));
    }

    @Override
    public boolean skill_use(Level level, Player player, ItemStack stack, int skillIndex) {
        //释放技能
        if (!level.isClientSide) {
            stack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
            stack.set(ModComponents.IS_POWERED.get(), true);
            stack.set(ModComponents.POWERED_TIMER.get(), level.getGameTime());
            syncBuiltInEnchantments(stack, level);

            //音效
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.5f,1.0f);
            //播报
            player.displayClientMessage(Component.translatable("item.PanlingRE.zhu_ri.skill.success"), true);
        }
        return true;
    }
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide) return;

        boolean isPowered = stack.getOrDefault(ModComponents.IS_POWERED.get(), false);
        if (isPowered) {
            long startTime = stack.getOrDefault(ModComponents.POWERED_TIMER.get(), 0L);
            if (level.getGameTime() - startTime > 200) {
                stack.set(ModComponents.IS_POWERED.get(), false);
                syncBuiltInEnchantments(stack, level);
                if (entity instanceof Player player) {
                    player.displayClientMessage(Component.translatable("item.PanlingRE.zhu_ri.skill.expired"), true);
                }
            }
        }
    }

    @Override
    protected boolean hasBuiltInEnchantments(ItemStack stack) {
        return !stack.getOrDefault(ModComponents.IS_POWERED.get(), false);
    }

    @Override
    public long getSkillCD(int skillIndex) {
        return cooldown * 50L;
    }

    @Override
    public String getSkillNameKey(int skillIndex) {
        return "item.PanlingRE.zhu_ri.skill1.2";
    }

    @Override
    public float getSkillLingQiCost(int skillIndex) {
        return cost;
    }

    @Override
    public String[] getSkillDescription(int skillIndex) {
        return new String[]{
                "item.PanlingRE.zhu_ri.skill3",
                "item.PanlingRE.zhu_ri.skill4",
                "item.PanlingRE.zhu_ri.skill5"
        };
    }

    @Override
    public Component getName(ItemStack stack) {
        return stack.getOrDefault(ModComponents.IS_POWERED.get(), false) ?
                Component.translatable("item.panlingre.zhu_ri.juji") :
                Component.translatable("item.panlingre.zhu_ri.youxia");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {

        // 检测Shift键
        if (SafeClientAccess.isShiftPressed()) {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare6"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.zhu_ri.lore1"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.zhu_ri.lore2"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.zhu_ri.skill1.2"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.zhu_ri.skill2", cooldown_remove.getCooldownText(SafeClientAccess.getClientPlayer(), cooldown),
                    LingQiData.getCostText(cost)));
            tooltipComponents.add(Component.translatable("item.PanlingRE.zhu_ri.skill3"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.zhu_ri.skill4"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.zhu_ri.skill5"));
        } else {
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.rare6"));
            tooltipComponents.add(Component.translatable("item.PanlingRE.lore.limit1"));
            tooltipComponents.add(Component.empty());
            tooltipComponents.add(Component.translatable("item.PanlingRE.zhu_ri.skill1.1"));
        }

        super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}
