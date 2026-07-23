package icu.icuqalt10.panlingre.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.task.TaskGuideLoader;
import icu.icuqalt10.panlingre.task.TaskGuideService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = PanlingRE.MODID)
public final class TaskGuideCommand {
    private TaskGuideCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("plre")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("task")
                                .then(Commands.literal("find")
                                        .then(action("on"))
                                        .then(action("off"))
                                        .then(action("query"))))
        );
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> action(String action) {
        return Commands.literal(action)
                .then(Commands.argument("task", StringArgumentType.greedyString())
                        .suggests((context, builder) ->
                                SharedSuggestionProvider.suggest(TaskGuideLoader.paths(), builder))
                        .executes(context -> execute(context, action)));
    }

    private static int execute(CommandContext<CommandSourceStack> context, String action) {
        ServerPlayer player;
        try {
            player = context.getSource().getPlayerOrException();
        } catch (Exception exception) {
            context.getSource().sendFailure(Component.literal("任务指引指令必须以玩家身份执行"));
            return 0;
        }

        String value = StringArgumentType.getString(context, "task");
        ResourceLocation taskId = value.indexOf(':') >= 0
                ? null
                : ResourceLocation.tryBuild(PanlingRE.MODID, value);
        if (taskId == null || TaskGuideLoader.get(taskId).isEmpty()) {
            context.getSource().sendFailure(Component.literal("未知或无效的任务指引: " + value));
            return 0;
        }

        return switch (action) {
            case "on" -> {
                TaskGuideService.activate(player, taskId);
                yield 1;
            }
            case "off" -> {
                TaskGuideService.deactivate(player, taskId);
                yield 1;
            }
            case "query" -> TaskGuideService.isActive(player, taskId) ? 1 : 0;
            default -> 0;
        };
    }
}
