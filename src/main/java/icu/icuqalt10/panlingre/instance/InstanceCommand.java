package icu.icuqalt10.panlingre.instance;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.List;

@EventBusSubscriber(modid = PanlingRE.MODID)
public final class InstanceCommand {
    private InstanceCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("plre")
                        .then(Commands.literal("instance")
                                .then(Commands.literal("start")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        InstanceManager.definitionIds().stream().map(ResourceLocation::getPath), builder))
                                                .executes(context -> start(context, List.of(context.getSource().getPlayerOrException())))
                                                .then(Commands.argument("players", EntityArgument.players())
                                                        .executes(context -> start(context, EntityArgument.getPlayers(context, "players"))))
                                        )
                                )
                        )
        );
    }

    private static int start(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players) {
        String value = StringArgumentType.getString(context, "id");
        ResourceLocation id = ResourceLocation.tryParse(value.indexOf(':') >= 0 ? value : PanlingRE.MODID + ":" + value);
        if (id == null) {
            context.getSource().sendFailure(Component.translatable("command.panlingre.instance.unknown", value));
            return 0;
        }
        return InstanceManager.start(players, id, context.getSource()) ? players.size() : 0;
    }
}
