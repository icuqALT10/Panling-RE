package icu.icuqalt10.panlingre.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.YuansuData;
import icu.icuqalt10.panlingre.init.ModAttachments;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = PanlingRE.MODID)
public final class YuansuCommand {
    private YuansuCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("plre")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("warlock")
                                .then(Commands.literal("yuansu")
                                        .then(series("ys2"))
                                        .then(series("ys3"))))
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> series(String series) {
        return Commands.literal(series)
                .then(Commands.literal("on").executes(context -> set(context, series, true)))
                .then(Commands.literal("off").executes(context -> set(context, series, false)))
                .then(Commands.literal("query").executes(context -> query(context, series)));
    }

    private static int set(CommandContext<CommandSourceStack> context, String series, boolean value) {
        ServerPlayer player;
        try {
            player = context.getSource().getPlayerOrException();
        } catch (Exception exception) {
            context.getSource().sendFailure(Component.literal("该指令必须由玩家执行"));
            return 0;
        }

        YuansuData current = player.getData(ModAttachments.YUANSU.get());
        player.setData(ModAttachments.YUANSU.get(), current.with(series, value));
        context.getSource().sendSuccess(
                () -> Component.literal(series + " 使用权已" + (value ? "开启" : "关闭")), false);
        return 1;
    }

    private static int query(CommandContext<CommandSourceStack> context, String series) {
        ServerPlayer player;
        try {
            player = context.getSource().getPlayerOrException();
        } catch (Exception exception) {
            context.getSource().sendFailure(Component.literal("该指令必须由玩家执行"));
            return 0;
        }

        int result = YuansuData.hasPermission(player, series) ? 1 : 0;
        context.getSource().sendSuccess(() -> Component.literal(Integer.toString(result)), false);
        return result;
    }
}
