package icu.icuqalt10.panlingre.command;

import com.mojang.brigadier.context.CommandContext;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.compat.beyonddimensions.BeyondDimensionsAccess;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = PanlingRE.MODID)
public final class BeyondDimensionsCommand {
    private BeyondDimensionsCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        if (!BeyondDimensionsAccess.isInstalled()) return;

        event.getDispatcher().register(
                Commands.literal("plre")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("byd")
                                .then(Commands.literal("on")
                                        .executes(context -> set(context, true)))
                                .then(Commands.literal("off")
                                        .executes(context -> set(context, false)))
                                .then(Commands.literal("query")
                                        .executes(BeyondDimensionsCommand::query)))
        );
    }

    private static int set(CommandContext<CommandSourceStack> context, boolean enabled) {
        MinecraftServer server = context.getSource().getServer();
        BeyondDimensionsAccess.setEnabled(server, enabled);
        int closedMenus = enabled ? 0 : BeyondDimensionsAccess.closeOpenStorageMenus(server);

        String key = enabled
                ? "command.panlingre.byd.on"
                : "command.panlingre.byd.off";
        context.getSource().sendSuccess(
                () -> enabled
                        ? Component.translatable(key)
                        : Component.translatable(key, closedMenus),
                false
        );
        return 1;
    }

    private static int query(CommandContext<CommandSourceStack> context) {
        boolean enabled = BeyondDimensionsAccess.isEnabled(context.getSource().getServer());
        context.getSource().sendSuccess(
                () -> Component.translatable("command.panlingre.byd.query." + (enabled ? "on" : "off")),
                false
        );
        return enabled ? 1 : 0;
    }
}
