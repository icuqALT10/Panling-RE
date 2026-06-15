package icu.icuqalt10.panlingre.server;

import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import icu.icuqalt10.panlingre.item.skill_1_key;
import icu.icuqalt10.panlingre.item.skill_2_key;
import icu.icuqalt10.panlingre.network.SkillPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.concurrent.atomic.AtomicReference;

public class ServerPayloadHandler {
    public static void handleSkill(final SkillPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            int keyID = payload.keyID();

            if (keyID == 1) {
                AtomicReference<ItemStack> foundStack = new AtomicReference<>(player.getMainHandItem());

                if (!(foundStack.get().getItem() instanceof skill_1_key)) {
                    CuriosApi.getCuriosInventory(player)
                            .flatMap(handler -> handler.findFirstCurio(stack -> stack.getItem() instanceof skill_1_key)).ifPresent(result -> foundStack.set(result.stack()));
                }

                ItemStack stack = foundStack.get();
                // 检查物品是否支持技能
                if (stack.getItem() instanceof skill_1_key skillItem) {
                    // 生成一个唯一的标签名，例如 "cd.panlingre:teng_mu_gong.skill_1"
                    String cdTag = "cd." + BuiltInRegistries.ITEM.getKey(stack.getItem()).toString() + ".skill_" + keyID;

                    long currentTime = System.currentTimeMillis();
                    long lastUseTime = player.getPersistentData().getLong(cdTag);

                    // 检查是否还在冷却
                    if (currentTime < lastUseTime) {
                        long timeLeft = (lastUseTime - currentTime) / 1000;
                        player.displayClientMessage(Component.translatable("chat.panlingre.cooldown", timeLeft), true);
                        return;
                    }

                    long cooldownNeeded =cooldown_remove.skill_cd_remove(player,skillItem.getCD_1(cdTag))+currentTime;
                    // 执行技能并更新时间戳 若技能执行成功 计算冷却缩减后的时间
                    if(skillItem.skill_1_trigger(player.level(), player, stack)) {

                        player.getPersistentData().putLong(cdTag, cooldownNeeded);
                        // 挥手动作
                        player.swing(InteractionHand.MAIN_HAND, true);
                        
                    }
                }
            }


            if (keyID == 2) {
                AtomicReference<ItemStack> foundStack = new AtomicReference<>(player.getMainHandItem());

                if (!(foundStack.get().getItem() instanceof skill_2_key)) {
                    CuriosApi.getCuriosInventory(player)
                            .flatMap(handler -> handler.findFirstCurio(stack -> stack.getItem() instanceof skill_2_key)).ifPresent(result -> foundStack.set(result.stack()));
                }

                ItemStack stack = foundStack.get();
                // 检查物品是否支持技能
                if (stack.getItem() instanceof skill_2_key skillItem) {
                    // 生成一个唯一的标签名，例如 "cd.panlingre:teng_mu_gong.skill_2"
                    String cdTag = "cd." + BuiltInRegistries.ITEM.getKey(stack.getItem()).toString() + ".skill_" + keyID;

                    long currentTime = System.currentTimeMillis();
                    long lastUseTime = player.getPersistentData().getLong(cdTag);

                    // 检查是否还在冷却
                    if (currentTime < lastUseTime) {
                        long timeLeft = (lastUseTime - currentTime) / 1000;
                        player.displayClientMessage(Component.translatable("chat.panlingre.cooldown", timeLeft), true);
                        return;
                    }

                    long cooldownNeeded =cooldown_remove.skill_cd_remove(player,skillItem.getCD_2(cdTag))+currentTime;
                    // 执行技能并更新时间戳 计算冷却缩减后的时间
                    if(skillItem.skill_2_trigger(player.level(), player, stack)) {

                        player.getPersistentData().putLong(cdTag, cooldownNeeded);
                        // 挥手动作
                        player.swing(InteractionHand.MAIN_HAND, true);

                    }
                }
            }
        });
    }
}