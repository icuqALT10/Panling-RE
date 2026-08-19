package icu.icuqalt10.panlingre.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument; // 新增导入
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer; // 推荐使用 ServerPlayer
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import top.theillusivec4.curios.api.CuriosApi;

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
                                                .then(Commands.argument("item", ItemPredicateArgument.itemPredicate(event.getBuildContext()))
                                                        .executes(context -> executeCheck(
                                                                context,
                                                                ItemPredicateArgument.getItemPredicate(context, "item")
                                                        ))
                                                )
                                        )
                                )
                        )
        );

    }

    private static int executeCheck(
            CommandContext<CommandSourceStack> context,
            ItemPredicateArgument.Result itemPredicate
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");
        String slotIdentifier = StringArgumentType.getString(context, "slot");

        boolean hasItem = CuriosApi.getCuriosInventory(targetPlayer).map(handler ->
                handler.getStacksHandler(slotIdentifier).map(stacksHandler -> {
                    var stacks = stacksHandler.getStacks();
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        var stack = stacks.getStackInSlot(i);
                        if (!stack.isEmpty() && itemPredicate.test(stack)) {
                            return true;
                        }
                    }
                    return false;
                }).orElse(false)
        ).orElse(false);

        String targetName = targetPlayer.getScoreboardName();
        if (hasItem) {
            source.sendSuccess(() -> Component.literal("§a[PanlingRE]检测成功！玩家 " + targetName + " 的 " + slotIdentifier + " 槽位中存在匹配物品。"), false);
            return 1;
        }

        source.sendFailure(Component.literal("§c[PanlingRE]检测失败！玩家 " + targetName + " 的 " + slotIdentifier + " 槽位中不存在匹配物品。"));
        return 0;
    }
}
