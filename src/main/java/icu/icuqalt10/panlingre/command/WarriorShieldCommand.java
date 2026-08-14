package icu.icuqalt10.panlingre.command;

import com.mojang.brigadier.context.CommandContext;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.WarriorShieldData;
import icu.icuqalt10.panlingre.init.ModAttachments;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = PanlingRE.MODID)
public final class WarriorShieldCommand {

    private WarriorShieldCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("plre")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("warrior")
                                .then(Commands.literal("di_shi_dun")
                                        .then(Commands.literal("on").executes(context -> set(context, true)))
                                        .then(Commands.literal("off").executes(context -> set(context, false)))
                                        .then(Commands.literal("query").executes(WarriorShieldCommand::query))))
        );
    }

    private static int set(CommandContext<CommandSourceStack> context, boolean enabled) {
        ServerPlayer player = getPlayer(context);
        if (player == null) return 0;

        player.setData(ModAttachments.WARRIOR_SHIELD.get(), new WarriorShieldData(enabled));
        context.getSource().sendSuccess(
                () -> Component.translatable("command.panlingre.warrior.shield."
                        + (enabled ? "on" : "off")), false);
        return 1;
    }

    private static int query(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = getPlayer(context);
        if (player == null) return 0;

        int result = WarriorShieldData.hasPermission(player) ? 1 : 0;
        context.getSource().sendSuccess(() -> Component.literal(Integer.toString(result)), false);
        return result;
    }

    @Nullable
    private static ServerPlayer getPlayer(CommandContext<CommandSourceStack> context) {
        try {
            return context.getSource().getPlayerOrException();
        } catch (Exception exception) {
            context.getSource().sendFailure(
                    Component.translatable("command.panlingre.warrior.shield.player_only"));
            return null;
        }
    }
}
