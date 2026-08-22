package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.item.fuzhi.FuZhiBagItem;
import icu.icuqalt10.panlingre.item.skill_trigger;
import icu.icuqalt10.panlingre.skill.SkillCastManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.concurrent.atomic.AtomicReference;

public record SkillWheelPayload(ResourceLocation itemId, int skillIndex) implements CustomPacketPayload {
    public static final Type<SkillWheelPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "skill_wheel"));

    public static final StreamCodec<ByteBuf, SkillWheelPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, SkillWheelPayload::itemId,
            ByteBufCodecs.VAR_INT, SkillWheelPayload::skillIndex,
            SkillWheelPayload::new
    );

    public static void handle(final SkillWheelPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                tryUse(player, payload.itemId(), payload.skillIndex(), true,
                        null, InteractionHand.MAIN_HAND);
            }
        });
    }

    public static void requestHandUse(ServerPlayer player, ResourceLocation itemId,
                                      int skillIndex, InteractionHand hand) {
        tryUse(player, itemId, skillIndex, true, hand, hand);
    }

    public static void completeCast(ServerPlayer player, ResourceLocation itemId, int skillIndex,
                                    @Nullable InteractionHand sourceHand,
                                    InteractionHand castingHand) {
        tryUse(player, itemId, skillIndex, false, sourceHand, castingHand);
    }

    private static void tryUse(ServerPlayer player, ResourceLocation itemId,
                               int skillIndex, boolean allowWindup,
                               @Nullable InteractionHand sourceHand,
                               InteractionHand castingHand) {
        ItemStack stack = findSkillStack(player, itemId, sourceHand);
        if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(itemId)
                || !(stack.getItem() instanceof skill_trigger skillItem)) {
            return;
        }

        // Never trust the client-provided index when reading server-owned skill data.
        if (skillIndex < 0 || skillIndex >= skillItem.getSkillCount(stack)) return;

        String castKey = skillItem.getSkillCooldownKey(stack, skillIndex);
        if (castKey.isEmpty()) castKey = itemId + ".skill_" + skillIndex;
        if (allowWindup && SkillCastManager.cancelIfSame(player, castKey)) return;

        long baseCooldown = Math.max(0L, skillItem.getSkillCD(stack, skillIndex));
        String cdTag = "cd." + castKey;
        String cooldownKey = skillItem.getSkillNameKey(stack, skillIndex);
        if (cooldownKey.isEmpty()) cooldownKey = stack.getDescriptionId();
        Item cooldownItem = skillItem.getSkillCooldownItem(stack, skillIndex);
        long currentTime = System.currentTimeMillis();
        long lastUseTime = player.getPersistentData().getLong(cdTag);

        if (currentTime < lastUseTime
                || cooldownItem != null && player.getCooldowns().isOnCooldown(cooldownItem)) {
            long timeLeft = Math.max(0L, (lastUseTime - currentTime + 999L) / 1000L);
            player.displayClientMessage(Component.translatable("chat.panlingre.cooldown", timeLeft), true);
            return;
        }

        if (!skillItem.canUse(player.level(), player, stack, skillIndex)) return;

        float lingqiCost = skillItem.getSkillLingQiCost(stack, skillIndex);
        if (!Float.isFinite(lingqiCost) || lingqiCost < 0f) return;
        LingQiData lingQiData = player.getData(ModAttachments.LINGQI);
        if (lingQiData.getCurrent() < lingqiCost) {
            player.displayClientMessage(Component.translatable("title.lingqi.cant_use"), true);
            return;
        }

        if (allowWindup) {
            int castTimeTicks = Math.max(0, skillItem.getSkillCastTimeTicks(stack, skillIndex));
            if (castTimeTicks > 0) {
                SkillCastManager.start(player, itemId, skillIndex, castKey,
                        castTimeTicks, sourceHand, castingHand);
                return;
            }
            SkillCastManager.cancel(player);
        }

        float lingqiBeforeUse = lingQiData.getCurrent();
        if (lingqiCost > 0) {
            lingQiData.setCurrent(lingqiBeforeUse - lingqiCost, player);
        }

        boolean succeeded = false;
        try {
            succeeded = skillItem.skill_use(player.level(), player, stack, skillIndex);
        } finally {
            if (!succeeded && lingqiCost > 0) {
                lingQiData.setCurrent(lingqiBeforeUse, player);
            }
        }

        if (!succeeded) return;

        if (lingqiCost > 0) lingQiData.sync(player);
        long actualCooldown = Math.max(
                0L, cooldown_remove.skill_cd_remove(player, baseCooldown));
        player.getPersistentData().putLong(
                cdTag, System.currentTimeMillis() + actualCooldown);

        if (cooldownItem != null && actualCooldown > 0) {
            int cooldownTicks = (int) Math.min(
                    Integer.MAX_VALUE, (actualCooldown + 49L) / 50L);
            player.getCooldowns().addCooldown(cooldownItem, cooldownTicks);
        }
        PacketDistributor.sendToPlayer(player,
                new SkillUseSucceededPayload(cooldownKey, actualCooldown));
        if (allowWindup) {
            player.swing(castingHand, true);
        } else {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                    new SkillCastReleasePayload(
                            player.getId(), castingHand == InteractionHand.MAIN_HAND));
        }
    }

    private static ItemStack findSkillStack(Player player, ResourceLocation itemId,
                                            @Nullable InteractionHand sourceHand) {
        if (sourceHand != null) {
            return player.getItemInHand(sourceHand);
        }

        AtomicReference<ItemStack> foundStack = new AtomicReference<>(ItemStack.EMPTY);

        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            if (BuiltInRegistries.ITEM.get(itemId) instanceof FuZhiBagItem) {
                handler.getStacksHandler(FuZhiBagItem.CURIO_SLOT).ifPresent(stackHandler -> {
                    var stacks = stackHandler.getStacks();
                    for (int slot = 0; slot < stacks.getSlots(); slot++) {
                        ItemStack curio = stacks.getStackInSlot(slot);
                        if (handler.isSlotActive(FuZhiBagItem.CURIO_SLOT, slot)
                                && BuiltInRegistries.ITEM.getKey(curio.getItem()).equals(itemId)) {
                            foundStack.set(curio);
                            break;
                        }
                    }
                });
            } else {
                var equippedCurios = handler.getEquippedCurios();
                for (int slot = 0; slot < equippedCurios.getSlots(); slot++) {
                    ItemStack curio = equippedCurios.getStackInSlot(slot);
                    if (BuiltInRegistries.ITEM.getKey(curio.getItem()).equals(itemId)) {
                        foundStack.set(curio);
                        break;
                    }
                }
            }
        });

        boolean bagRequest = BuiltInRegistries.ITEM.get(itemId) instanceof FuZhiBagItem;
        if (!bagRequest && foundStack.get().isEmpty()) {
            for (ItemStack armor : player.getArmorSlots()) {
                if (BuiltInRegistries.ITEM.getKey(armor.getItem()).equals(itemId)) {
                    foundStack.set(armor);
                    break;
                }
            }
        }

        if (!bagRequest && foundStack.get().isEmpty()
                && BuiltInRegistries.ITEM.getKey(player.getOffhandItem().getItem()).equals(itemId)) {
            foundStack.set(player.getOffhandItem());
        }
        if (!bagRequest && foundStack.get().isEmpty()
                && BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).equals(itemId)) {
            foundStack.set(player.getMainHandItem());
        }
        return foundStack.get();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}
