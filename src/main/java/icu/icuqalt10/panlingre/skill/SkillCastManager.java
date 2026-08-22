package icu.icuqalt10.panlingre.skill;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.init.ModEffects;
import icu.icuqalt10.panlingre.network.SkillCastStatePayload;
import icu.icuqalt10.panlingre.network.SkillWheelPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative scheduler for skill wind-ups. */
@EventBusSubscriber(modid = PanlingRE.MODID)
public final class SkillCastManager {
    private static final Map<UUID, PendingCast> ACTIVE_CASTS = new HashMap<>();

    private SkillCastManager() {
    }

    public static boolean start(ServerPlayer player, ResourceLocation itemId, int skillIndex,
                                String castKey, int durationTicks,
                                @Nullable InteractionHand sourceHand,
                                InteractionHand castingHand) {
        MinecraftServer server = player.getServer();
        int safeDuration = Math.max(0, durationTicks);
        if (server == null || safeDuration <= 0 || player.hasEffect(ModEffects.freeze)
                || player.containerMenu != player.inventoryMenu) {
            cancel(player);
            return false;
        }

        PendingCast current = ACTIVE_CASTS.get(player.getUUID());
        if (current != null && current.castKey().equals(castKey)) {
            cancel(player);
            return false;
        }

        ACTIVE_CASTS.put(player.getUUID(), new PendingCast(
                itemId, skillIndex, castKey, server.getTickCount() + safeDuration,
                sourceHand, castingHand));
        syncState(player, safeDuration, castingHand);
        return true;
    }

    /** Returns true when the request toggled off an in-progress cast of the same skill. */
    public static boolean cancelIfSame(ServerPlayer player, String castKey) {
        PendingCast pending = ACTIVE_CASTS.get(player.getUUID());
        if (pending == null || !pending.castKey().equals(castKey)) return false;
        cancel(player);
        return true;
    }

    public static boolean cancel(ServerPlayer player) {
        PendingCast removed = ACTIVE_CASTS.remove(player.getUUID());
        if (removed == null) return false;
        syncState(player, 0, removed.castingHand());
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE_CASTS.isEmpty()) return;

        MinecraftServer server = event.getServer();
        int currentTick = server.getTickCount();
        List<CompletedCast> completed = new ArrayList<>();

        Iterator<Map.Entry<UUID, PendingCast>> iterator = ACTIVE_CASTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingCast> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            PendingCast pending = entry.getValue();

            if (player == null || !player.isAlive() || player.isSpectator()) {
                iterator.remove();
                if (player != null) syncState(player, 0, pending.castingHand());
                continue;
            }

            if (shouldInterrupt(player, pending)) {
                iterator.remove();
                syncState(player, 0, pending.castingHand());
                continue;
            }

            if (currentTick >= pending.endTick()) {
                iterator.remove();
                syncState(player, 0, pending.castingHand());
                completed.add(new CompletedCast(player, pending));
            }
        }

        for (CompletedCast completedCast : completed) {
            PendingCast pending = completedCast.pending();
            SkillWheelPayload.completeCast(
                    completedCast.player(), pending.itemId(), pending.skillIndex(),
                    pending.sourceHand(), pending.castingHand());
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getOriginalDamage() > 0.0F && event.getEntity() instanceof ServerPlayer player) {
            cancel(player);
        }
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getEffectInstance().is(ModEffects.freeze)) {
            cancel(player);
        }
    }

    @SubscribeEvent
    public static void onContainerOpened(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player);
    }

    private static boolean shouldInterrupt(ServerPlayer player, PendingCast pending) {
        if (player.hasEffect(ModEffects.freeze)
                || player.containerMenu != player.inventoryMenu) {
            return true;
        }
        if (pending.sourceHand() == null) return false;
        return !BuiltInRegistries.ITEM.getKey(
                player.getItemInHand(pending.sourceHand()).getItem()).equals(pending.itemId());
    }

    private static void syncState(ServerPlayer player, int durationTicks,
                                  InteractionHand castingHand) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new SkillCastStatePayload(player.getId(), durationTicks,
                        castingHand == InteractionHand.MAIN_HAND));
    }

    private record PendingCast(ResourceLocation itemId, int skillIndex, String castKey,
                               int endTick, @Nullable InteractionHand sourceHand,
                               InteractionHand castingHand) {
    }

    private record CompletedCast(ServerPlayer player, PendingCast pending) {
    }
}
