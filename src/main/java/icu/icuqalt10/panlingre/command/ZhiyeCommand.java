package icu.icuqalt10.panlingre.command;

import com.mojang.brigadier.context.CommandContext;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.ZhiyeData;
import icu.icuqalt10.panlingre.attachment.ZhiyeData.Profession;
import icu.icuqalt10.panlingre.init.ModAttachments;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = PanlingRE.MODID)
public final class ZhiyeCommand {
    private ZhiyeCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        var root = Commands.literal("plre")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("zhiye")
                        .then(profession(Profession.ARCHER))
                        .then(profession(Profession.WARLOCK))
                        .then(profession(Profession.WARRIOR)));
        event.getDispatcher().register(root);
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> profession(
            Profession profession) {
        return Commands.literal(profession.commandName())
                .then(Commands.literal("on")
                        .executes(context -> set(context, profession, true)))
                .then(Commands.literal("off")
                        .executes(context -> set(context, profession, false)))
                .then(Commands.literal("query")
                        .executes(context -> query(context, profession)));
    }

    private static int set(CommandContext<CommandSourceStack> context,
                           Profession profession, boolean enabled) {
        ServerPlayer player = getPlayer(context);
        if (player == null) return 0;

        ZhiyeData current = player.getData(ModAttachments.ZHIYE.get());
        player.setData(ModAttachments.ZHIYE.get(), current.with(profession, enabled));
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.panlingre.zhiye.set",
                profession.commandName(), enabled ? "on" : "off"), false);
        return 1;
    }

    private static int query(CommandContext<CommandSourceStack> context, Profession profession) {
        ServerPlayer player = getPlayer(context);
        if (player == null) return 0;

        boolean enabled = ZhiyeData.has(player, profession);
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.panlingre.zhiye.query",
                profession.commandName(), enabled ? "on" : "off"), false);
        return enabled ? 1 : 0;
    }

    private static ServerPlayer getPlayer(CommandContext<CommandSourceStack> context) {
        try {
            return context.getSource().getPlayerOrException();
        } catch (Exception exception) {
            context.getSource().sendFailure(
                    Component.translatable("command.panlingre.zhiye.player_only"));
            return null;
        }
    }
}
