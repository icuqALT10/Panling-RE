package icu.icuqalt10.panlingre.client;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.item.archer.other.tian_xing_jian;
import icu.icuqalt10.panlingre.item.archer.zhu_ri;
import icu.icuqalt10.panlingre.network.TianXingTargetPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Comparator;

@EventBusSubscriber(modid = PanlingRE.MODID, value = Dist.CLIENT)
public final class TianXingSniperState {
    private static final double TARGET_RANGE = 128.0D;
    private static int targetId = -1;
    private static int syncTicks;

    private TianXingSniperState() {
    }

    public static boolean shouldGlow(Entity entity) {
        return entity.getId() == targetId;
    }

    public static int outlineColor() {
        return 0xFF3030;
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null || !isFullyDrawnSniper(player)) {
            setTarget(-1, false);
            return;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        int selected = minecraft.level.getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(TARGET_RANGE),
                        target -> tian_xing_jian.isValidSniperTarget(player, target)
                                && target.distanceToSqr(player) <= TARGET_RANGE * TARGET_RANGE
                                && directionDot(eye, look, target) >= tian_xing_jian.SNIPER_MIN_DOT)
                .stream()
                .max(Comparator
                        .comparingDouble((LivingEntity target) -> directionDot(eye, look, target))
                        .thenComparingDouble(target -> -target.distanceToSqr(player)))
                .map(Entity::getId)
                .orElse(-1);

        setTarget(selected, ++syncTicks % 5 == 0);
    }

    private static boolean isFullyDrawnSniper(Player player) {
        ItemStack weapon = player.getMainHandItem();
        if (!player.isUsingItem() || player.getUseItem() != weapon
                || tian_xing_jian.getFormForWeapon(weapon) != tian_xing_jian.FORM_SNIPER
                || !tian_xing_jian.hasEquippedForm(player, tian_xing_jian.FORM_SNIPER)) {
            return false;
        }

        int usedTicks = weapon.getUseDuration(player) - player.getUseItemRemainingTicks();
        if (weapon.getItem() instanceof BowItem) {
            return BowItem.getPowerForTime(usedTicks) >= 1.0F;
        }
        return weapon.getItem() instanceof zhu_ri && usedTicks >= 20;
    }

    private static double directionDot(Vec3 eye, Vec3 look, LivingEntity target) {
        Vec3 direction = target.getBoundingBox().getCenter().subtract(eye);
        return direction.lengthSqr() < 1.0E-6D ? 1.0D : look.dot(direction.normalize());
    }

    private static void setTarget(int newTargetId, boolean refresh) {
        if (newTargetId == targetId && !refresh) return;
        targetId = newTargetId;
        syncTicks = newTargetId < 0 ? 0 : syncTicks;
        PacketDistributor.sendToServer(new TianXingTargetPayload(newTargetId));
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        targetId = -1;
        syncTicks = 0;
    }
}
