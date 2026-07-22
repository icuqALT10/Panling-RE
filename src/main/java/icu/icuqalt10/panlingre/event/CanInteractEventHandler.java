package icu.icuqalt10.panlingre.event;

import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.entity.interaction.CanInteractData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Optional;

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
                || !canInteract.contains("function", Tag.TAG_STRING)) {
            return;
        }

        String functionPath = canInteract.getString("function").trim();
        ResourceLocation functionId = ResourceLocation.tryParse(functionPath);
        if (functionId == null) {
            player.sendSystemMessage(Component.translatable("message.panlingre.can_interact.invalid_function", functionPath));
            return;
        }

        ServerFunctionManager functionManager = player.getServer().getFunctions();
        Optional<CommandFunction<CommandSourceStack>> function = functionManager.get(functionId);
        if (function.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.panlingre.can_interact.missing_function", functionId));
            return;
        }

        CommandSourceStack source = player.createCommandSourceStack()
                .withPermission(2)
                .withSuppressedOutput();
        functionManager.execute(function.get(), source);
    }
}
