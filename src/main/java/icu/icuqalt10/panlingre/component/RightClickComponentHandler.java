package icu.icuqalt10.panlingre.component;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attribute.cooldown_remove;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class RightClickComponentHandler {
    private RightClickComponentHandler() {
    }

    public static void completeUse(ServerPlayer player, ItemStack stack, RightClickComponent component) {
        String command = component.command().trim();
        if (command.startsWith("/")) {
            command = command.substring(1).trim();
        }

        CommandSourceStack source = player.createCommandSourceStack()
                .withPermission(4)
                .withSuppressedOutput();
        player.getServer().getCommands().performPrefixedCommand(source, command);

        RightClickComponent.Cooldown cooldown = component.cooldown();
        if (cooldown.time() <= 0) {
            return;
        }

        if (cooldown.cdRemove()) {
            cooldown_remove.cd_remove(player, stack.getItem(), cooldown.time());
        } else {
            player.getCooldowns().addCooldown(stack.getItem(), cooldown.time());
        }
    }
}
