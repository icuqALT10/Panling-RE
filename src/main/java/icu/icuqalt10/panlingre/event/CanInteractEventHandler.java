package icu.icuqalt10.panlingre.event;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.entity.interaction.CanInteractData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = PanlingRE.MODID)
public final class CanInteractEventHandler {
    private CanInteractEventHandler() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Entity target = event.getTarget();
        if (!(target instanceof Interaction) && !(target instanceof Villager)) {
            return;
        }

        CompoundTag canInteract = ((CanInteractData) target).panlingre$getCanInteract();
        if (canInteract == null
                || !canInteract.contains("active", Tag.TAG_BYTE)
                || !canInteract.getBoolean("active")
                || !canInteract.contains("command", Tag.TAG_STRING)) {
            return;
        }

        String command = canInteract.getString("command").trim();
        if (command.startsWith("/")) {
            command = command.substring(1).trim();
        }
        if (command.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.panlingre.can_interact.empty_command"));
            return;
        }

        CommandSourceStack source = player.createCommandSourceStack()
                .withPermission(4)
                .withSuppressedOutput();
        player.getServer().getCommands().performPrefixedCommand(source, command);
    }
}
