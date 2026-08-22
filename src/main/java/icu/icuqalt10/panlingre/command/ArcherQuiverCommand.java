package icu.icuqalt10.panlingre.command;

import com.mojang.brigadier.context.CommandContext;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.ArcherQuiverData;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.item.archer.other.tian_xing_jian;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = PanlingRE.MODID)
public final class ArcherQuiverCommand {
    private ArcherQuiverCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("plre")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("archer")
                                .then(Commands.literal("tian_xing_jian")
                                        .then(Commands.literal("on").executes(context -> set(context, true)))
                                        .then(Commands.literal("off").executes(context -> set(context, false)))
                                        .then(Commands.literal("query").executes(ArcherQuiverCommand::query))))
        );
    }

    private static int set(CommandContext<CommandSourceStack> context, boolean enabled) {
        ServerPlayer player = getPlayer(context);
        if (player == null) return 0;

        player.setData(ModAttachments.ARCHER_TIAN_XING_JIAN.get(),
                new ArcherQuiverData(enabled));
        if (!enabled) {
            tian_xing_jian.deactivateEquipped(player);
        }
        context.getSource().sendSuccess(
                () -> Component.translatable("command.panlingre.archer.tian_xing_jian."
                        + (enabled ? "on" : "off")), false);
        return 1;
    }

    private static int query(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = getPlayer(context);
        if (player == null) return 0;

        int result = ArcherQuiverData.hasPermission(player) ? 1 : 0;
        context.getSource().sendSuccess(() -> Component.literal(Integer.toString(result)), false);
        return result;
    }

    @Nullable
    private static ServerPlayer getPlayer(CommandContext<CommandSourceStack> context) {
        try {
            return context.getSource().getPlayerOrException();
        } catch (Exception exception) {
            context.getSource().sendFailure(Component.translatable(
                    "command.panlingre.archer.tian_xing_jian.player_only"));
            return null;
        }
    }
}
