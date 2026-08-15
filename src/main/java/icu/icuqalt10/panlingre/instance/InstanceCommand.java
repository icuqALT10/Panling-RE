package icu.icuqalt10.panlingre.instance;

import com.mojang.brigadier.arguments.StringArgumentType;
import icu.icuqalt10.panlingre.PanlingRE;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

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
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    String value = StringArgumentType.getString(context, "id");
                                                    ResourceLocation id = value.indexOf(':') >= 0
                                                            ? ResourceLocation.tryParse(value)
                                                            : ResourceLocation.fromNamespaceAndPath(PanlingRE.MODID, value);
                                                    if (id == null) {
                                                        context.getSource().sendFailure(Component.translatable("command.panlingre.instance.unknown", value));
                                                        return 0;
                                                    }
                                                    return InstanceManager.start(player, id) ? 1 : 0;
                                                })
                                        )
                                )
                        )
        );
    }
}
