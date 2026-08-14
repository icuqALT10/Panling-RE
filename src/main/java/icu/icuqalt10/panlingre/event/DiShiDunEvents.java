package icu.icuqalt10.panlingre.event;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.item.warrior.other.di_shi_dun;
import icu.icuqalt10.panlingre.network.PojunCounterAttackPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = PanlingRE.MODID)
public final class DiShiDunEvents {

    private static final Map<UUID, Long> AUTHORIZED_COUNTER_ATTACKS = new HashMap<>();
    private static final Map<UUID, Long> ARMED_COUNTER_ATTACKS = new HashMap<>();

    private DiShiDunEvents() {
    }

    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (!event.getBlocked() || !(event.getEntity() instanceof Player player)) return;

        ItemStack shield = player.getUseItem();
        if (!(shield.getItem() instanceof di_shi_dun item)) return;
        event.setShieldDamage(0.0F);

        int form = di_shi_dun.getForm(shield);
        if (form == di_shi_dun.FORM_POJUN) {
            handlePojunBlock(player, item);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !player.isUsingItem()) return;

        ItemStack shield = player.getUseItem();
        if (!(shield.getItem() instanceof di_shi_dun shieldItem)
                || di_shi_dun.getForm(shield) != di_shi_dun.FORM_JINZHONG) return;

        float armor = (float) player.getAttributeValue(Attributes.ARMOR);
        float originalDamage = event.getOriginalAmount();
        float threshold = armor * 0.15F;

        // 延后一 tick，避免本次伤害的吸收阶段立刻消耗刚获得的黄心。
        GameBusEvents.queueTask(1, () -> {
            if (!player.isAlive()) return;

            float absorptionCap = armor * 0.5F;
            if (player.getAbsorptionAmount() < absorptionCap) {
                player.setAbsorptionAmount(Math.min(
                        absorptionCap, player.getAbsorptionAmount() + 4.0F));
            }

            player.displayClientMessage(Component.translatable(
                    "item.PanlingRE.di_shi_dun.jinzhong.skill.success"), true);

            if (originalDamage > threshold) {
                int cooldownTicks = Math.max(1, Math.round((originalDamage - threshold) * 20.0F));
                player.stopUsingItem();
                cooldown_remove.cd_remove(player, shieldItem, cooldownTicks);
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        di_shi_dun.refreshMaxAbsorption(player);

        long now = player.level().getGameTime();
        AUTHORIZED_COUNTER_ATTACKS.computeIfPresent(player.getUUID(),
                (uuid, expiresAt) -> expiresAt < now ? null : expiresAt);
        ARMED_COUNTER_ATTACKS.computeIfPresent(player.getUUID(),
                (uuid, expiresAt) -> expiresAt < now ? null : expiresAt);
    }

    private static void handlePojunBlock(Player player, di_shi_dun shieldItem) {
        shieldItem.finishSuccessfulPojunBlock(player);
        player.displayClientMessage(Component.translatable(
                "item.PanlingRE.di_shi_dun.pojun.skill.success"), true);

        if (player instanceof ServerPlayer serverPlayer) {
            AUTHORIZED_COUNTER_ATTACKS.put(player.getUUID(), player.level().getGameTime() + 10L);
            PacketDistributor.sendToPlayer(serverPlayer, new PojunCounterAttackPayload());
        }
    }

    public static void armAuthorizedCounterAttack(ServerPlayer player) {
        long now = player.level().getGameTime();
        Long authorizedUntil = AUTHORIZED_COUNTER_ATTACKS.remove(player.getUUID());
        if (authorizedUntil != null && authorizedUntil >= now) {
            ARMED_COUNTER_ATTACKS.put(player.getUUID(), now + 2L);
        }
    }

    public static boolean consumeArmedCounterAttack(Player player) {
        if (player.level().isClientSide) return false;
        Long armedUntil = ARMED_COUNTER_ATTACKS.remove(player.getUUID());
        return armedUntil != null && armedUntil >= player.level().getGameTime();
    }

}
