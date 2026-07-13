package icu.icuqalt10.panlingre.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument; // 新增导入
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer; // 推荐使用 ServerPlayer
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = PanlingRE.MODID)
public class curios {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {

        event.getDispatcher().register(
                Commands.literal("plre")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("curios")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("slot", StringArgumentType.string())
                                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                                        .executes(context -> {
                                                            var source = context.getSource();

                                                            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");

                                                            String slotIdentifier = StringArgumentType.getString(context, "slot");
                                                            Item targetItem = ItemArgument.getItem(context, "item").getItem();

                                                            boolean hasItem = CuriosApi.getCuriosInventory(targetPlayer).map(handler -> {
                                                                return handler.getStacksHandler(slotIdentifier).map(stacksHandler -> {
                                                                    var stacks = stacksHandler.getStacks();
                                                                    for (int i = 0; i < stacks.getSlots(); i++) {
                                                                        if (stacks.getStackInSlot(i).is(targetItem)) {
                                                                            return true;
                                                                        }
                                                                    }
                                                                    return false;
                                                                }).orElse(false);
                                                            }).orElse(false);

                                                            String targetName = targetPlayer.getScoreboardName();

                                                            if (hasItem) {
                                                                source.sendSuccess(() -> Component.literal("§a[PanlingRE]检测成功！玩家 " + targetName + " 装备了该物品。"), false);
                                                                return 1;
                                                            } else {
                                                                source.sendFailure(Component.literal("§c[PanlingRE]检测失败！玩家 " + targetName + " 未装备该物品。"));
                                                                return 0;
                                                            }
                                                        })
                                                )
                                        )
                                )
                        )
        );

    }
}