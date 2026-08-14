package icu.icuqalt10.panlingre.item.warlock.yuansu;

import icu.icuqalt10.panlingre.entity.YsMuHealingEntity;
import icu.icuqalt10.panlingre.network.ItemActivationPayload;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Shared execution logic for the tier-two elemental healing skill. */
public final class Ys2HealingSkill {
    private static final double TARGET_RADIUS = 20.0;
    private static final int MAX_TEAMMATES = 4;
    private Ys2HealingSkill() { }

    public static void execute(ServerLevel level, Player owner, ItemStack sourceStack,
                               Holder<SoundEvent> sound,
                               Consumer<LivingEntity> targetEffect) {
        execute(level, owner, sourceStack, sound, 0x00AAAA, targetEffect);
    }

    public static void execute(ServerLevel level, Player owner, ItemStack sourceStack,
                               Holder<SoundEvent> sound, int trailColor,
                               Consumer<LivingEntity> targetEffect) {
        execute(level, owner, sourceStack, sound, trailColor, 0.0F,
                (target, ignoredValue) -> targetEffect.accept(target));
    }

    public static void execute(ServerLevel level, Player owner, ItemStack sourceStack,
                               Holder<SoundEvent> sound,
                               float effectValue,
                               BiConsumer<LivingEntity, Float> targetEffect) {
        execute(level, owner, sourceStack, sound, 0x00AAAA, effectValue, targetEffect);
    }

    public static void execute(ServerLevel level, Player owner, ItemStack sourceStack,
                               Holder<SoundEvent> sound, int trailColor,
                               float effectValue,
                               BiConsumer<LivingEntity, Float> targetEffect) {
        ItemStack flyingStack = sourceStack.copyWithCount(1);
        targetEffect.accept(owner, effectValue);
        if (owner instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer,
                    new ItemActivationPayload(flyingStack.copy()));
            serverPlayer.playNotifySound(sound.value(), SoundSource.PLAYERS, 0.5F, 1.0F);
        }

        AABB searchArea = owner.getBoundingBox().inflate(TARGET_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, searchArea,
                        target -> isValidTarget(owner, target))
                .stream()
                .sorted(Comparator
                        .comparingInt((LivingEntity target) -> target instanceof Player ? 0 : 1)
                        .thenComparingDouble(owner::distanceToSqr))
                .limit(MAX_TEAMMATES)
                .toList();

        for (LivingEntity target : targets) {
            spawnHealingItem(level, owner, target, flyingStack, sound,
                    trailColor, effectValue, targetEffect);
        }

    }

    private static boolean isValidTarget(Player owner, LivingEntity target) {
        return target != owner
                && target.isAlive()
                && (!(target instanceof Player player) || !player.isSpectator())
                && owner.distanceToSqr(target) <= TARGET_RADIUS * TARGET_RADIUS
                && (owner.getTeam() == null || owner.isAlliedTo(target));
    }

    private static void spawnHealingItem(ServerLevel level, Player owner,
                                         LivingEntity target, ItemStack stack,
                                         Holder<SoundEvent> sound,
                                         int trailColor,
                                         float effectValue,
                                         BiConsumer<LivingEntity, Float> targetEffect) {
        Vec3 p0 = owner.getEyePosition().add(owner.getLookAngle().scale(0.8)).add(0.0, -0.5, 0.0);
        Vec3 p3 = target.getEyePosition().add(0.0, -0.2, 0.0);
        Vec3 towardTarget = p3.subtract(p0);
        double distance = towardTarget.length();
        if (distance < 0.01) return;

        Vec3 forward = towardTarget.normalize();
        Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
        if (right.lengthSqr() < 0.01) right = forward.cross(new Vec3(1.0, 0.0, 0.0));
        right = right.normalize();

        double angle = level.random.nextDouble() * Math.PI * 2.0;
        Vec3 sideDirection = right.scale(Math.cos(angle))
                .add(forward.cross(right).scale(Math.sin(angle)));
        if (sideDirection.lengthSqr() < 0.01) sideDirection = right;
        sideDirection = sideDirection.normalize();

        double verticalSign = level.random.nextDouble() < 0.5 ? 1.0 : -1.0;
        double verticalMagnitude = distance * (0.15 + level.random.nextDouble() * 0.2);
        double p1Distance = distance * 0.3;
        Vec3 p1 = p0.add(forward.scale(p1Distance * 0.6))
                .add(sideDirection.scale(p1Distance * 0.5))
                .add(0.0, verticalSign * verticalMagnitude * 0.6, 0.0);

        double horizontalOffset2 = (level.random.nextDouble() - 0.5)
                * Math.min(distance * 0.3, 5.0);
        Vec3 p2 = p3.subtract(forward.scale(distance * 0.25))
                .add(right.scale(horizontalOffset2))
                .add(0.0, verticalSign * verticalMagnitude * 0.3, 0.0);
        level.addFreshEntity(new YsMuHealingEntity(
                level, target, stack, sound, trailColor,
                effectValue, targetEffect, p0, p1, p2, p3));
    }
}
