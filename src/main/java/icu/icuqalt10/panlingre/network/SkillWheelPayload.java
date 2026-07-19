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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.concurrent.atomic.AtomicReference;

public record SkillWheelPayload(ResourceLocation itemId, int skillIndex, long cooldown) implements CustomPacketPayload {
    public static final Type<SkillWheelPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, "skill_wheel"));

    public static final StreamCodec<ByteBuf, SkillWheelPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, SkillWheelPayload::itemId,
            ByteBufCodecs.VAR_INT, SkillWheelPayload::skillIndex,
            ByteBufCodecs.VAR_LONG, SkillWheelPayload::cooldown,
            SkillWheelPayload::new
    );

    public static void handle(final SkillWheelPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            var itemId = payload.itemId();
            var skillIndex = payload.skillIndex();
            var cd = payload.cooldown();

            AtomicReference<ItemStack> foundStack = new AtomicReference<>(player.getMainHandItem());

            if (!(BuiltInRegistries.ITEM.getKey(foundStack.get().getItem()).equals(itemId))) {
                ItemStack offhand = player.getOffhandItem();
                if (BuiltInRegistries.ITEM.getKey(offhand.getItem()).equals(itemId)) {
                    foundStack.set(offhand);
                } else {
                    CuriosApi.getCuriosInventory(player).flatMap(
                            handler -> handler.findFirstCurio(
                                    stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(itemId)
                            )
                    ).ifPresent(result -> foundStack.set(result.stack()));
                }
            }

            ItemStack stack = foundStack.get();
            if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(itemId)) return;

            if (stack.getItem() instanceof skill_trigger skillItem) {
                // 冷却检查
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

                // 灵气检查
                float lingqiCost = skillItem.getSkillLingQiCost(skillIndex);
                if (lingqiCost > 0) {
                    LingQiData lingQiData = player.getData(ModAttachments.LINGQI);
                    if (!lingQiData.consume(player, lingqiCost)) return;
                }

                // 执行技能
                long cooldownNeeded = cooldown_remove.skill_cd_remove(player, cd) + currentTime;
                if (skillItem.skill_use(player.level(), player, stack, skillIndex)) {
                    player.getPersistentData().putLong(cdTag, cooldownNeeded);
                    player.swing(InteractionHand.MAIN_HAND, true);
                }
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
}
