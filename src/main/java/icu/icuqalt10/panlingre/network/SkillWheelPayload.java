package icu.icuqalt10.panlingre.network;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.LingQiData;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.item.skill_trigger;
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
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
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
            Player player = context.player();
            var itemId = payload.itemId();
            var skillIndex = payload.skillIndex();

            AtomicReference<ItemStack> foundStack = new AtomicReference<>(ItemStack.EMPTY);

            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                var equippedCurios = handler.getEquippedCurios();
                for (int slot = 0; slot < equippedCurios.getSlots(); slot++) {
                    ItemStack curio = equippedCurios.getStackInSlot(slot);
                    if (BuiltInRegistries.ITEM.getKey(curio.getItem()).equals(itemId)) {
                        foundStack.set(curio);
                        break;
                    }
                }
            });

            if (foundStack.get().isEmpty()) {
                for (ItemStack armor : player.getArmorSlots()) {
                    if (BuiltInRegistries.ITEM.getKey(armor.getItem()).equals(itemId)) {
                        foundStack.set(armor);
                        break;
                    }
                }
            }

            if (foundStack.get().isEmpty()
                    && BuiltInRegistries.ITEM.getKey(player.getOffhandItem().getItem()).equals(itemId)) {
                foundStack.set(player.getOffhandItem());
            }
            if (foundStack.get().isEmpty()
                    && BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).equals(itemId)) {
                foundStack.set(player.getMainHandItem());
            }

            ItemStack stack = foundStack.get();
            if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(itemId)) return;

            if (stack.getItem() instanceof skill_trigger skillItem) {
                // 不信任客户端传入的索引，避免越界访问技能数据。
                if (skillIndex < 0 || skillIndex >= skillItem.getSkillCount()) return;

                // 冷却时长只能由服务端的物品实现决定。
                long baseCooldown = Math.max(0L, skillItem.getSkillCD(skillIndex));
                String cdTag = "cd." + itemId.toString() + ".skill_" + skillIndex;
                long currentTime = System.currentTimeMillis();
                long lastUseTime = player.getPersistentData().getLong(cdTag);

                if (currentTime < lastUseTime) {
                    long timeLeft = (lastUseTime - currentTime) / 1000;
                    player.displayClientMessage(Component.translatable("chat.panlingre.cooldown", timeLeft), true);
                    return;
                }

                // 自定义前置条件
                if (!skillItem.canUse(player.level(), player, stack, skillIndex)) return;

                // 预留灵气；技能失败或抛出异常时在 finally 中回滚。
                float lingqiCost = skillItem.getSkillLingQiCost(skillIndex);
                if (!Float.isFinite(lingqiCost) || lingqiCost < 0f) return;
                LingQiData lingQiData = player.getData(ModAttachments.LINGQI);
                if (lingQiData.getCurrent() < lingqiCost) {
                    player.displayClientMessage(Component.translatable("title.lingqi.cant_use"), true);
                    return;
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

                if (succeeded) {
                    if (lingqiCost > 0) lingQiData.sync(player);
                    long actualCooldown = Math.max(
                            0L, cooldown_remove.skill_cd_remove(player, baseCooldown));
                    player.getPersistentData().putLong(
                            cdTag, System.currentTimeMillis() + actualCooldown);

                    String cooldownKey = skillItem.getSkillNameKey(skillIndex);
                    if (cooldownKey.isEmpty()) cooldownKey = stack.getDescriptionId();
                    if (player instanceof ServerPlayer serverPlayer) {
                        PacketDistributor.sendToPlayer(serverPlayer,
                                new SkillUseSucceededPayload(cooldownKey, actualCooldown));
                    }
                    player.swing(InteractionHand.MAIN_HAND, true);
                }
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}
