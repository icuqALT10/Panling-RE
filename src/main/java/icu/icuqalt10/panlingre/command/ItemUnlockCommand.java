package icu.icuqalt10.panlingre.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.ItemUnlockData;
import icu.icuqalt10.panlingre.init.ModAttachments;
import icu.icuqalt10.panlingre.item.RecipeLockedItem;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = PanlingRE.MODID)
public final class ItemUnlockCommand {
    private ItemUnlockCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("plre")
                .then(Commands.literal("unlock").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("item")
                                .then(Commands.argument("item", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                BuiltInRegistries.ITEM.stream()
                                                        .filter(item -> item instanceof RecipeLockedItem)
                                                        .map(BuiltInRegistries.ITEM::getKey), builder))
                                        .then(Commands.literal("on").executes(context -> execute(context, "on")))
                                        .then(Commands.literal("off").executes(context -> execute(context, "off")))
                                        .then(Commands.literal("query").executes(context -> execute(context, "query")))))));
    }

    private static int execute(CommandContext<CommandSourceStack> context, String action) throws CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        var id = ResourceLocationArgument.getId(context, "item");
        var item = BuiltInRegistries.ITEM.get(id);
        if (!(item instanceof RecipeLockedItem)) {
            context.getSource().sendFailure(Component.translatable("command.panlingre.unlock.invalid", id.toString()));
            return 0;
        }
        if (action.equals("query")) {
            int result = ItemUnlockData.canUse(player, item) ? 1 : 0;
            context.getSource().sendSuccess(() -> Component.literal(Integer.toString(result)), false);
            return result;
        }

        boolean enabled = action.equals("on");
        var data = player.getData(ModAttachments.ITEM_UNLOCKS);
        player.setData(ModAttachments.ITEM_UNLOCKS, enabled ? data.unlock(id) : data.lock(id));
        var recipes = player.server.getRecipeManager().getRecipes().stream()
                .filter(recipe -> recipe.value().getResultItem(player.registryAccess()).is(item))
                .toList();
        if (enabled) player.awardRecipes(recipes);
        else player.resetRecipes(recipes);

        // Recompute open results immediately, including the player's 2x2 crafting grid.
        player.inventoryMenu.slotsChanged(player.getInventory());
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.slotsChanged(player.getInventory());
        context.getSource().sendSuccess(() -> Component.translatable(enabled
                ? "command.panlingre.unlock.success" : "command.panlingre.unlock.off", item.getDescription()), false);
        return 1;
    }
}
